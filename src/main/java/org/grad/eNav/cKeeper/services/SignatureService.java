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
import org.grad.eNav.cKeeper.models.domain.Certificate;
import org.grad.eNav.cKeeper.models.domain.MrnEntity;
import org.grad.eNav.cKeeper.models.domain.Pair;
import org.grad.eNav.cKeeper.models.domain.mcp.McpEntityType;
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

import static java.util.function.Predicate.not;

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
     * The MCP Config Service.
     */
    @Autowired
    McpConfigService mcpConfigService;

    /**
     * Generates and returns a signature for the provided payload using the
     * keys from the latest certificate assigned to the MRN entity identified
     * by the MRN constructed from the AtoN UID provided.
     *
     * @param entityId      The entity ID to generate the signature for
     * @param entityId      The ID of the entity to generate the signature for
     * @param entityType    The MCP type of the entity to generate the signature for
     * @param payload       The payload to be signed
     * @return The signature for the provided payload
     */
    public Pair<String, byte[]> generateEntitySignature(@NotNull String entityId, String mmsi, @NotNull McpEntityType entityType, @NotNull byte[] payload) {
        final MrnEntity mrnEntity = Optional.of(entityId)
                .map(mrn -> {
                    try {
                        return this.mrnEntityService.findOneByName(entityId);
                    } catch (DataNotFoundException ex) {
                        return null;
                    }
                })
                .orElseGet(() -> {
                    try {
                        final MrnEntity newMrnEntity = new MrnEntity();
                        newMrnEntity.setName(entityId);
                        newMrnEntity.setMrn(this.mcpConfigService.constructMcpEntityMrn(entityType, entityId));
                        newMrnEntity.setMmsi(mmsi);
                        newMrnEntity.setEntityType(entityType);
                        newMrnEntity.setVersion(entityType == McpEntityType.SERVICE ? "0.0.1" : null);
                        return this.mrnEntityService.save(newMrnEntity);
                    } catch (Exception ex) {
                        throw new SavingFailedException(ex.getMessage());
                    }
                });

        // Now get the latest certificate for it if it exists, or create a new one
        Certificate certificate = this.certificateService.findAllByMrnEntityId(mrnEntity.getId())
                .stream()
                .filter(not(c -> Objects.equals(c.getRevoked(), Boolean.TRUE)))
                .filter(not(c -> Objects.isNull(c.getStartDate())))
                .max(Comparator.comparing(Certificate::getStartDate))
                .orElseGet(() -> {
                    try {
                        return this.certificateService.generateMrnEntityCertificate(mrnEntity.getId());
                    } catch (Exception ex) {
                        throw new SavingFailedException(ex.getMessage());
                    }
                });

        // Finally, sing the payload
        try {
            this.log.debug("Signature service signing payload: {}", Base64.getEncoder().encodeToString(payload));
            final byte[] signature = this.certificateService.signContent(certificate.getId(), payload);
            this.log.debug("Signature service generated signature: {}", Base64.getEncoder().encodeToString(signature));
            return new Pair<>(certificate.getCertificate(), signature);
        } catch (NoSuchAlgorithmException | IOException | InvalidKeySpecException | SignatureException |  InvalidKeyException ex) {
            throw new InvalidRequestException(ex.getMessage());
        }
    }

    /**
     * Verify that for the MRN constructed for the provided entity ID (name),
     * the signature is a valid one for the specified content.
     *
     * Note that the content and signature need to be Base64 encoded, coming
     * from the controller.
     *
     * @param entityId      The entity ID to get the certificate for
     * @param b64Content    The Base64 encoded content to be verified
     * @param b64Signature  The Base64 encoded signature to verify the content with
     * @return Whether the verification was successful or not
     */
    public boolean verifyEntitySignature(@NotNull String entityId, String b64Content, String b64Signature) {
        return Optional.of(entityId)
                .map(this.mrnEntityService::findOneByName)
                .map(MrnEntity::getId)
                .map(this.certificateService::findAllByMrnEntityId)
                .orElseGet(() -> Collections.emptySet())
                .stream()
                .filter(c -> Objects.nonNull(c.getStartDate()))
                .max(Comparator.comparing(Certificate::getStartDate))
                .map(Certificate::getId)
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
    public boolean verifyEntitySignatureByMmsi(@NotNull String mmsi, String b64Content, String b64Signature) {
        return Optional.of(mmsi)
                .map(this.mrnEntityService::findOneByMmsi)
                .map(MrnEntity::getName)
                .map(name -> this.verifyEntitySignature(name, b64Content, b64Signature))
                .orElse(Boolean.FALSE);
    }
}
