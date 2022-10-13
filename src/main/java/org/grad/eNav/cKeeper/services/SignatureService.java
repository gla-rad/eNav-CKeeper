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
import org.grad.eNav.cKeeper.exceptions.InvalidRequestException;
import org.grad.eNav.cKeeper.exceptions.ValidationException;
import org.grad.eNav.cKeeper.models.domain.Certificate;
import org.grad.eNav.cKeeper.models.domain.MrnEntity;
import org.grad.eNav.cKeeper.models.domain.SignatureCertificate;
import org.grad.eNav.cKeeper.models.domain.mcp.McpEntityType;
import org.grad.secom.core.utils.SecomPemUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Optional;

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
     * The X.509 Trust-Store Root Certificate Alias.
     */
    @Value("${gla.rad.ckeeper.mcp.trustStore.rootCertificate.alias:root}")
    String rootCertAlias;

    /**
     * The X.509 Trust-Store Certificate Thumbprint Algorithm.
     */
    @Value("${gla.rad.ckeeper.mcp.trustStore.rootCertificate.thumbprintAlgorithm:SHA-1}")
    String rootCertThumbprintAlgorithm;

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
     * This function will attempt to access the most recent valid certificate
     * to be used for signing and will return its information so that it can
     * be accessed and used, before the actual signing process.
     *
     * @param entityName        The name of the entity to retrieve the certificate for
     * @param mmsi              The mmsi of the entity to retrieve the certificate for
     * @param entityType        The type of the entity to retrieve the certificate for
     * @return the most recent valid certificate for the specified entity
     */
    public SignatureCertificate getSignatureCertificate(@NotNull String entityName, String mmsi, McpEntityType entityType) {
        // Get or create a new MRN Entity if it doesn't exist
        final MrnEntity mrnEntity = this.mrnEntityService.getOrCreate(
                entityName, mmsi, this.mcpConfigService.constructMcpEntityMrn(entityType, entityName), entityType);

        // Get the latest or create a certificate if it doesn't exist
        final Certificate certificate = this.certificateService.getLatestOrCreate(
                mrnEntity.getId());

        // Create the signature certificate response
        final SignatureCertificate signatureCertificate = new SignatureCertificate();
        try {
            signatureCertificate.setCertificateId(certificate.getId());
            signatureCertificate.setCertificate(SecomPemUtils.getMinifiedPemFromCertString(certificate.getCertificate()));
            signatureCertificate.setPublicKey(SecomPemUtils.getMinifiedPemFromPublicKeyString(certificate.getPublicKey()));
            signatureCertificate.setRootCertificate(SecomPemUtils.getMinifiedPemFromCert(this.certificateService.getTrustedCertificate(this.rootCertAlias)));
        } catch (CertificateEncodingException ex) {
            throw new ValidationException(ex.getMessage());
        }

        // And return the signature certificate
        return signatureCertificate;
    }

    /**
     * Generates and returns a signature for the provided payload using the
     * keys from the latest certificate assigned to the MRN entity identified
     * by the MRN constructed from the AtoN UID provided.
     *
     * @param certificateId The ID of the certificate to generate the signature for
     * @param algorithm     The algorithm to use for generating the signature
     * @param payload       The payload to be signed
     * @return The signature for the provided payload
     */
    public byte[] generateEntitySignature(@NotNull BigInteger certificateId,
                                          String algorithm,
                                          @NotNull byte[] payload) {
        try {
            log.debug("Signature service signing payload: {}", Base64.getEncoder().encodeToString(payload));
            final byte[] signature = this.certificateService.signContent(certificateId, algorithm, payload);
            log.debug("Signature service generated signature: {}", Base64.getEncoder().encodeToString(signature));
            return signature;
        } catch (NoSuchAlgorithmException | IOException | InvalidKeySpecException | SignatureException | InvalidKeyException ex) {
            log.error(ex.getMessage());
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
     * @param entityName    The name of the entity to get the certificate for
     * @param b64Content    The Base64 encoded content to be verified
     * @param b64Signature  The Base64 encoded signature to verify the content with
     * @return Whether the verification was successful or not
     */
    public boolean verifyEntitySignature(@NotNull String entityName, String b64Content, String b64Signature) {
        return Optional.of(entityName)
                .map(this.mrnEntityService::findOneByName)
                .map(MrnEntity::getId)
                .map(this.certificateService::getLatestOrCreate)
                .map(Certificate::getId)
                .map(id -> {
                    try {
                        log.debug("Signature service verifying payload: {}\n with signature: {}", b64Content, b64Signature);
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
