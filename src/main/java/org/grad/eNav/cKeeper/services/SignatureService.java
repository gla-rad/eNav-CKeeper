/*
 * Copyright (c) 2021 GLA Research and Development Directorate
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.grad.eNav.cKeeper.services;

import lombok.extern.slf4j.Slf4j;
import org.grad.eNav.cKeeper.exceptions.DataNotFoundException;
import org.grad.eNav.cKeeper.exceptions.InvalidRequestException;
import org.grad.eNav.cKeeper.exceptions.SavingFailedException;
import org.grad.eNav.cKeeper.models.dtos.CertificateDto;
import org.grad.eNav.cKeeper.models.dtos.MrnEntityDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

/**
 * The Signature Service Class
 *
 * Service Implementation for managing signatures.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Service
@Slf4j
public class SignatureService {

    /**
     * The MCP Service.
     */
    @Autowired
    McpService mcpService;

    /**
     * The MRN Entity Service.
     */
    @Autowired
    MrnEntityService mrnEntityService;

    /**
     * The Certificate Service.
     */
    @Autowired
    CertificateService certificateService;

    /**
     * Generates and returns a signature for the provided payload using the
     * keys from the latest certificate assigned to the MRN entity identified
     * by the MRN constructed from the AtoN UID provided.
     *
     * @param atonUID       The AtoN UID to construct the MRN from
     * @param mmsi          The AtoN MMSI number
     * @param payload       The payload to be signed
     * @return The signature for the provided payload
     */
    public byte[] generateAtonSignature(@NotNull String atonUID, String mmsi, @NotNull byte[] payload) {
        // Translate the AtoN UID into an MRC based on the MCP rules
        final String atonMrn = this.mcpService.constructMcpDeviceMrn(atonUID);

        // Get a matching MRN entity if it exists or create a new one
        final MrnEntityDto mrnEntityDto = Optional.of(atonMrn)
                .map(mrn -> {
                    try { return this.mrnEntityService.findOneByMrn(mrn); } catch (DataNotFoundException ex) { return null; }
                })
                .orElseGet(() -> {
                    try {
                        return this.mrnEntityService.save(new MrnEntityDto(atonUID, atonMrn, mmsi));
                    } catch (Exception ex) {
                        throw new SavingFailedException(ex.getMessage());
                    }
                });

        // Now get the latest certificate for it if it exists, or create a new one
        CertificateDto certificateDto = this.certificateService.findAllByMrnEntityId(mrnEntityDto.getId())
                .stream()
                .filter(c -> Objects.nonNull(c.getStartDate()))
                .max(Comparator.comparing(CertificateDto::getStartDate))
                .orElseGet(() -> {
                    try {
                        return this.certificateService.generateMrnEntityCertificate(mrnEntityDto.getId());
                    } catch (Exception ex) {
                        throw new SavingFailedException(ex.getMessage());
                    }
                });

        // Finally, sing the payload
        try {
            this.log.debug("Signature service signing payload: {}", Base64.getEncoder().encodeToString(payload));
            final byte[] signature = this.certificateService.signContent(certificateDto.getId(), payload);
            this.log.debug("Signature service generated signature: {}", Base64.getEncoder().encodeToString(signature));
            return signature;
        } catch (NoSuchAlgorithmException | IOException | InvalidKeySpecException | SignatureException |  InvalidKeyException ex) {
            throw new InvalidRequestException(ex.getMessage());
        }
    }

    /**
     * Verify that for the MRN constructed for the provided entity MMSI, the
     * signature is a valid one for the specified content.
     *
     * Note that the content and signature need to be Base64 encoded, coming
     * from the controller.
     *
     * @param mmsi          The entity MMSI to get the certificate for
     * @param b64Content    The Base64 encoded content to be verified
     * @param b64Signature  The Base64 encoded signature to verify the content with
     * @return Whether the verification was successful or not
     */
    public boolean verifyMmsiSignature(String mmsi, String b64Content, String b64Signature) {
        return Optional.of(mmsi)
                .map(this.mrnEntityService::findOneByMmsi)
                .map(MrnEntityDto::getId)
                .map(this.certificateService::findAllByMrnEntityId)
                .orElseGet(() -> Collections.emptySet())
                .stream()
                .filter(c -> Objects.nonNull(c.getStartDate()))
                .max(Comparator.comparing(CertificateDto::getStartDate))
                .map(CertificateDto::getId)
                .map(id -> {
                    try {
                        this.log.debug("Signature service verifying payload: {}\n with signature: {}", b64Content, b64Signature);
                        return this.certificateService.verifyContent(id, Base64.getDecoder().decode(b64Content), Base64.getDecoder().decode(b64Signature));
                    } catch (Exception ex) {
                        return false;
                    }
                })
                .orElse(Boolean.FALSE);
    }
}
