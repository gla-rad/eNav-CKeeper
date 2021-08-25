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
import org.grad.eNav.cKeeper.models.domain.Certificate;
import org.grad.eNav.cKeeper.models.dtos.CertificateDto;
import org.grad.eNav.cKeeper.models.dtos.McpDeviceDto;
import org.grad.eNav.cKeeper.models.dtos.MrnEntityDto;
import org.grad.eNav.cKeeper.repos.CertificateRepo;
import org.grad.eNav.cKeeper.utils.X509Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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
     * The MCP Service.
     */
    @Autowired
    McpService mcpService;

    /**
     * The Certificate Service.
     */
    @Autowired
    CertificateService certificateService;

    /**
     * The Certificate Repo.
     */
    @Autowired
    CertificateRepo certificateRepo;

    /**
     *
     * @param atonUID
     * @param payload
     * @return
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws SignatureException
     * @throws InvalidKeyException
     * @throws InvalidKeySpecException
     */
    public byte[] generateAtonSignature(@NotNull String atonUID, @NotNull byte[] payload) throws IOException, NoSuchAlgorithmException, SignatureException, InvalidKeyException, InvalidKeySpecException {
        final String atonMrn = this.mcpService.constructMcpDeviceMrn(atonUID);
        final MrnEntityDto mrnEntityDto = Optional.of(atonUID)
                .map(this.mcpService::constructMcpDeviceMrn)
                .map(uid -> {
                    try {
                        return this.mrnEntityService.findOneByMrn(atonMrn);
                    } catch (DataNotFoundException ex) {
                        return null;
                    }
                }).orElseGet(() -> {
                    // If not found, let's create it now through the MRN service
                    MrnEntityDto newEntity = new MrnEntityDto();
                    newEntity.setName(atonUID);
                    newEntity.setMrn(atonMrn);
                    return this.mrnEntityService.save(newEntity);
                });

        // Now get the latest certificate for it and if not exists, create a new
        Certificate certificate = this.certificateRepo.findAllByMrnEntityId(mrnEntityDto.getId())
                .stream()
                .filter(c -> Objects.nonNull(c.getStartDate()))
                .max(Comparator.comparing(Certificate::getStartDate))
                .orElseGet(() -> {
                    try {
                        CertificateDto certificateDto = this.certificateService.generateMrnEntityCertificate(mrnEntityDto.getId());
                        return this.certificateRepo.findById(certificateDto.getId()).orElse(null);
                    } catch (Exception ex) {
                        log.error(ex.getMessage());
                        return null;
                    }
                });

        // And now sign
        if(Objects.nonNull(certificate)) {
            // Sign using the private key
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initSign(X509Utils.privateKeyFromPem(certificate.getPrivateKey(), null));
            signature.update(payload);
            return signature.sign();
        }

        // If we reached this point, just throw an exception
        throw new InvalidRequestException("Failed to sign the provided payload");
    }

    public boolean verifyAtonSignature(String atonUID, byte[] content) {
        return false;
    }
}
