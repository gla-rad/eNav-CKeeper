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
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.grad.eNav.cKeeper.exceptions.DataNotFoundException;
import org.grad.eNav.cKeeper.exceptions.SavingFailedException;
import org.grad.eNav.cKeeper.models.domain.Certificate;
import org.grad.eNav.cKeeper.models.domain.MRNEntity;
import org.grad.eNav.cKeeper.models.dtos.CertificateDto;
import org.grad.eNav.cKeeper.repos.CertificateRepo;
import org.grad.eNav.cKeeper.repos.MRNEntityRepo;
import org.grad.eNav.cKeeper.utils.X509Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The Certificate Service Class
 *
 * Service Implementation for managing Certificates.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Service
@Slf4j
public class CertificateService {

    /**
     * The Key-Pair Curve.
     */
    @Value("${gla.rad.ckeeper.x509.keypair.curve:secp256r1}")
    String keyPairCurve;

    /**
     * The X.509 Certificate Algorithm.
     */
    @Value("${gla.rad.ckeeper.x509.cert.algorithm:SHA256withCVC-ECDSA}")
    String certAlgorithm;

    /**
     * The X.509 Certificate Name String.
     */
    @Value("${gla.rad.ckeeper.x509.cert.dirName:CN=GLA}")
    String certDirName;

    /**
     * The X.509 Certificate Years Duration.
     */
    @Value("${gla.rad.ckeeper.x509.cert.yearDuration:1}")
    Integer certYearDuration;

    /**
     * The Certificate Repo.
     */
    @Autowired
    CertificateRepo certificateRepo;

    /**
     * The MRN Entity Repo.
     */
    @Autowired
    MRNEntityRepo mrnEntityRepo;

    /**
     * The MCP Service.
     */
    @Autowired
    McpService mcpService;

    /**
     * The service post-construct operations where the Bouncy Castle
     * security provider is added onto the environment.
     */
    @PostConstruct
    public void init() {
        // Add the Bouncy castle as a security provider to make signatures
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Returns all the certificates assigned to the MRN entity specified by
     * the MRN Entity ID. The result will be translated into DTO objects.
     *
     * @param mrnEntityId   The ID of the MRN entity to retrieve the certificates for
     * @return the set of certificates assigned to the provided MRN entity
     */
    public Set<CertificateDto> findAllByMrnEntityId(@NotNull BigInteger mrnEntityId) {
        return this.certificateRepo.findAllByMrnEntityId(mrnEntityId)
                .stream()
                .map(CertificateDto::new)
                .collect(Collectors.toSet());
    }

    /**
     * Generates a brand-new X.509 certificate for the MRN Entity specified by
     * the provided ID. The new certificate will be added into the database
     * and the corresponding DTO object will be returned.
     *
     * @param mrnEntityId   The ID of the MRN entity to generate the certificate for
     * @return The generated X.509 certificate DTO
     * @throws InvalidAlgorithmParameterException if the provided encryption parameters are not valid
     * @throws NoSuchAlgorithmException if the provided encryption algorithm is not found
     * @throws OperatorCreationException if the certificate generation process fails
     * @throws IOException for errors during the PEM exporting or HTTP call operations
     */
    public CertificateDto generateMrnEntityCertificate(@NotNull BigInteger mrnEntityId) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, OperatorCreationException, IOException {
        MRNEntity mrnEntity = this.mrnEntityRepo.findById(mrnEntityId)
                .orElseThrow(() ->
                        new DataNotFoundException(String.format("No MRN Entity node found for the provided ID: %d", mrnEntityId))
                );

        // Generate a new keypair for the certificate
        KeyPair keyPair = X509Utils.generateKeyPair(this.keyPairCurve);

        // Generate a new X509 certificate signing request
        PKCS10CertificationRequest csr = X509Utils.generateX509CSR(keyPair, this.certDirName, this.certAlgorithm);

        // Get the X509 certificate signed by the MCP
        X509Certificate x509Certificate = this.mcpService.issueMcpDeviceCertificate(mrnEntity.getMrn(), csr);

        // Populate the new certificate object
        Certificate certificate = new Certificate();
        certificate.setCertificate(X509Utils.formatCertificate(x509Certificate));
        certificate.setPublicKey(X509Utils.formatPublicKey(keyPair));
        certificate.setPrivateKey(X509Utils.formatPrivateKey(keyPair));
        certificate.setStartDate(x509Certificate.getNotBefore());
        certificate.setEndDate(x509Certificate.getNotAfter());
        certificate.setMcpMirId(null); // TODO: We need to wait for MIR to return the generated ID
        certificate.setMrnEntity(mrnEntity);

        // Save the certificate into the database
        return Optional.of(certificate)
                .map(this.certificateRepo::save)
                .map(CertificateDto::new)
                .orElseThrow(() ->
                        new SavingFailedException(String.format("Failed to generate the X.509 certificate for the MRN Entity with ID {}", mrnEntityId))
                );
    }

    /**
     * Delete the certificate by ID.
     *
     * @param id the ID of the certificate
     */
    public void delete(@NotNull BigInteger id) {
        log.debug("Request to delete Certificate : {}", id);
        if(this.certificateRepo.existsById(id)) {
            this.certificateRepo.deleteById(id);
        } else {
            throw new DataNotFoundException(String.format("No Certificate found for the provided ID: %d", id));
        }
    }

    /**
     * Revokes the certificate identified by the provided certificate ID.
     *
     * @param id    The ID of the certificate to be revoked
     * @return The revoked certificate
     * @throws IOException for errors during the HTTP call operation
     */
    public CertificateDto revoke(@NotNull BigInteger id) throws IOException {
        // Access the certificate if found
        Certificate certificate = this.certificateRepo.findById(id)
                .orElseThrow(() ->
                    new DataNotFoundException(String.format("No Certificate found for the provided ID: %d", id))
                );

        // Mark as revoked in the MCP
        this.mcpService.revokeMcpDeviceCertificate(certificate.getMrnEntity().getMrn(), certificate.getId());

        // And if successful, make it locally as well
        certificate.setRevoked(true);

        // Save and return
        return Optional.of(certificate)
                .map(this.certificateRepo::save)
                .map(CertificateDto::new)
                .orElseThrow(() ->
                        new SavingFailedException(String.format("Failed to revoke Certificate with ID: %d", id))
                );
    }

}
