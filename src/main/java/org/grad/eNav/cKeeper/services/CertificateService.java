/*
 * Copyright (c) 2024 GLA Research and Development Directorate
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.grad.eNav.cKeeper.services;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.grad.eNav.cKeeper.exceptions.DataNotFoundException;
import org.grad.eNav.cKeeper.exceptions.McpConnectivityException;
import org.grad.eNav.cKeeper.exceptions.SavingFailedException;
import org.grad.eNav.cKeeper.exceptions.ValidationException;
import org.grad.eNav.cKeeper.models.domain.Certificate;
import org.grad.eNav.cKeeper.models.domain.MrnEntity;
import org.grad.eNav.cKeeper.models.domain.Pair;
import org.grad.eNav.cKeeper.models.domain.mcp.McpEntityType;
import org.grad.eNav.cKeeper.repos.CertificateRepo;
import org.grad.eNav.cKeeper.repos.MRNEntityRepo;
import org.grad.eNav.cKeeper.utils.X509Utils;
import org.grad.secom.core.utils.KeyStoreUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

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
    @Value("${gla.rad.ckeeper.x509.keypair.curve:secp384r1}")
    String keyPairCurve;

    /**
     * The device-specific Key-Pair Curve.
     */
    @Value("${gla.rad.ckeeper.x509.keypair.device.curve:secp256r1}")
    String deviceKeyPairCurve;

    /**
     * The X.509 Certificate Algorithm.
     */
    @Value("${gla.rad.ckeeper.x509.cert.algorithm:SHA3-384withECDSA}")
    String defaultSigningAlgorithm;

    /**
     * The device-specific X.509 Certificate Algorithm.
     */
    @Value("${gla.rad.ckeeper.x509.cert.device.algorithm:SHA256withCVC-ECDSA}")
    String deviceDefaultSigningAlgorithm;

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
     * The X.509 Trust-Store.
     */
    @Value("${gla.rad.ckeeper.mcp.trustStore:mcp/truststore.p12}")
    String trustStore;

    /**
     * The X.509 Trust-Store Password.
     */
    @Value("${gla.rad.ckeeper.mcp.trustStorePassword:password}")
    String trustStorePassword;

    /**
     * The X.509 Trust-Store Type.
     */
    @Value("${gla.rad.ckeeper.mcp.trustStoreType:PKCS12}")
    String trustStoreType;

    /**
     * The X.509 Trust-Store Type.
     */
    @Value("${gla.rad.ckeeper.mcp.max-daily-generated-certificates:100}")
    int maxDailyGeneratedCertificates;

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
     * This function can be used to sync the certificate status of a given
     * MRN entity with the MCP MSR.
     *
     * @param mrnEntityId the MRN Entity ID
     */
    @Transactional
    public void syncMrnEntityWithMcpMir(@NotNull BigInteger mrnEntityId) {
        // First check that the MRN Entity exists and get its MIR certificates
        final MrnEntity mrnEntity = this.mrnEntityRepo.findById(mrnEntityId)
                .orElse(null);

        // Extract all the local certificates
        final Map<String, Certificate> localCertificates = Optional.ofNullable(mrnEntity)
                .map(MrnEntity::getCertificates)
                .orElse(Collections.emptySet())
                .stream()
                .collect(Collectors.toMap(Certificate::getMcpMirId, Function.identity()));

        // And get the current MCP state
        final Map<String, X509Certificate> mcpCertificates = Optional.ofNullable(mrnEntity)
                .map(entity -> {
                    try {
                        return mcpService.getMcpEntityCertificates(mrnEntity.getEntityType(), entity.getMrn(), mrnEntity.getVersion());
                    } catch (DataNotFoundException | McpConnectivityException ex) {
                        // If the MCP connectivity failed, just don't use it
                        return null;
                    }
                })
                .orElse(Collections.emptyMap());

        // Revoke all the certificates that are not found
        localCertificates.entrySet()
                .stream()
                .filter(not(entry -> Objects.equals(entry.getValue().getRevoked(), Boolean.TRUE)))
                .filter(not(entry -> mcpCertificates.containsKey(entry.getKey())))
                .map(Map.Entry::getValue)
                .map(cert -> {
                    cert.setRevoked(Boolean.TRUE);
                    return cert;
                })
                .forEach(this.certificateRepo::save);

        // Finally update the database with any new entries
        mcpCertificates.entrySet()
                .stream()
                .filter(not(entry -> localCertificates.containsKey(entry.getKey())))
                .map(entry -> {
                    try {
                        Certificate certificate = new Certificate(entry.getKey(), entry.getValue());
                        certificate.setMrnEntity(mrnEntity);
                        return certificate;
                    } catch (IOException ex) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .forEach(this.certificateRepo::save);
    }

    /**
     * Returns all the certificates assigned to the MRN entity specified by
     * the MRN Entity ID. The result will be translated into DTO objects.
     *
     * @param mrnEntityId   The ID of the MRN entity to retrieve the certificates for
     * @return the set of certificates assigned to the provided MRN entity
     */
    @Transactional
    public Set<Certificate> findAllByMrnEntityId(@NotNull BigInteger mrnEntityId) {
        // First always try to check the MCP MSR
        this.syncMrnEntityWithMcpMir(mrnEntityId);

        // And then lookup the local database
        return this.certificateRepo.findAllByMrnEntityId(mrnEntityId);
    }

    /**
     * Generate a certificate PEM string based on the selection from the
     * provided truststore, using the certificate alias.
     *
     * @param alias                 The alias of the certificate to be returned
     * @return the generated certificate thumbprint
     */
    public X509Certificate getTrustedCertificate(String alias) {
            return Optional.of(alias)
                    .map(a -> {
                        try {
                            return KeyStoreUtils.getKeyStore(this.trustStore,
                                            this.trustStorePassword,
                                            this.trustStoreType)
                                    .getCertificate(a);
                        } catch (Exception ex) {
                            log.error(ex.getMessage());
                            return null;
                        }
                    })
                    .filter(X509Certificate.class::isInstance)
                    .map(X509Certificate.class::cast)
                    .orElse(null);
    }

    /**
     * Generates a brand-new X.509 certificate for the MRN Entity specified by
     * the provided ID. The new certificate will be added into the database
     * and the corresponding DTO object will be returned.
     * <p/>
     * Note that for devices the hash of the certificate will be fixed to SHA-256
     * to allow for smaller signatures for AIS transmissions.
     *
     * @param mrnEntityId   The ID of the MRN entity to generate the certificate for
     * @return The generated X.509 certificate DTO
     * @throws InvalidAlgorithmParameterException if the provided encryption parameters are not valid
     * @throws NoSuchAlgorithmException if the provided encryption algorithm is not found
     * @throws OperatorCreationException if the certificate generation process fails
     * @throws IOException for errors during the PEM exporting or HTTP call operations
     */
    @Transactional
    public Certificate generateMrnEntityCertificate(@NotNull BigInteger mrnEntityId) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, OperatorCreationException, IOException, McpConnectivityException {
        MrnEntity mrnEntity = this.mrnEntityRepo.findById(mrnEntityId)
                .orElseThrow(() ->
                        new DataNotFoundException(String.format("No MRN Entity node found for the provided ID: %d", mrnEntityId))
                );

        // Perform a check to stop certificate flooding
        if(this.certificateRepo.getNumOfGeneratedCertificatesToday() >= this.maxDailyGeneratedCertificates) {
            log.error(String.format(
                    "Certificate generation maximum limit breached!!!" +
                    "\nCannot generate the requested certificate for MRN entity %s.", mrnEntity.getName())
            );
            throw new ValidationException("Too many certificates generated for one day... is there a leak taking place?");
        }

        // Generate a new keypair for the certificate - device will follow a different curve
        String curve = McpEntityType.DEVICE.equals(mrnEntity.getEntityType()) ? this.deviceKeyPairCurve : this.keyPairCurve;
        KeyPair keyPair = X509Utils.generateKeyPair(curve);

        // Generate a new X509 certificate signing request - device will follow a different algorithm
        String algorithm = McpEntityType.DEVICE.equals(mrnEntity.getEntityType()) ? this.deviceDefaultSigningAlgorithm : this.defaultSigningAlgorithm;
        PKCS10CertificationRequest csr = X509Utils.generateX509CSR(keyPair, this.certDirName, algorithm);

        // Get the X509 certificate signed by the MCP
        Pair<String, X509Certificate> certificateInfo = this.mcpService.issueMcpEntityCertificate(mrnEntity.getEntityType(), mrnEntity.getMrn(), mrnEntity.getVersion(), csr);

        // Populate the new certificate object
        Certificate certificate = new Certificate(certificateInfo.getKey(), certificateInfo.getValue());
        certificate.setPrivateKey(X509Utils.formatPrivateKey(keyPair.getPrivate()));
        certificate.setMrnEntity(mrnEntity);

        // Save the certificate into the database
        return Optional.of(certificate)
                .map(this.certificateRepo::save)
                .orElseThrow(() ->
                        new SavingFailedException(String.format("Failed to generate the X.509 certificate for the MRN Entity with ID: %d", mrnEntityId))
                );
    }

    /**
     * Delete the certificate by ID.
     *
     * @param id the ID of the certificate
     */
    @Transactional
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
    @Transactional
    public Certificate revoke(@NotNull BigInteger id) throws IOException, McpConnectivityException {
        // Access the certificate if found
        Certificate certificate = this.certificateRepo.findById(id)
                .orElseThrow(() ->
                    new DataNotFoundException(String.format("No Certificate found for the provided ID: %d", id))
                );

        // Mark as revoked in the MCP
        this.mcpService.revokeMcpEntityCertificate(certificate.getMrnEntity().getEntityType(), certificate.getMrnEntity().getMrn(), certificate.getMrnEntity().getVersion(), certificate.getMcpMirId());

        // And if successful, make it locally as well
        certificate.setRevoked(Boolean.TRUE);

        // Save and return
        return Optional.of(certificate)
                .map(this.certificateRepo::save)
                .orElseThrow(() ->
                        new SavingFailedException(String.format("Failed to revoke Certificate with ID: %d", id))
                );
    }

    /**
     * Retrieves the latest valid certificate that is present in the system for
     * the specified MRN Entity, using its ID. If no valid certificate is
     * detected, the certificate generation method will be called to provide
     * a new one.
     *
     * @param mrnEntityId       The ID of the MRN entity to get the certificate for
     * @return the latest valid certificate for the specifed MRN entity
     */
    @Synchronized
    public Certificate getLatestOrCreate(BigInteger mrnEntityId) {
        return this.findAllByMrnEntityId(mrnEntityId)
                .stream()
                .filter(c -> Optional.of(c).map(Certificate::getStartDate).map(d -> d.compareTo(Date.from(Instant.now())) <= 0).orElse(true))
                .filter(c -> Optional.of(c).map(Certificate::getEndDate).map(d -> d.compareTo(Date.from(Instant.now())) >= 0).orElse(true))
                .filter(not(c -> Objects.equals(c.getRevoked(), Boolean.TRUE)))
                .filter(not(c -> Objects.isNull(c.getStartDate())))
                .max(Comparator.comparing(Certificate::getStartDate))
                .orElseGet(() -> {
                    try {
                        return this.generateMrnEntityCertificate(mrnEntityId);
                    } catch (Exception ex) {
                        throw new SavingFailedException(ex.getMessage());
                    }
                });
    }

    /**
     * Performs the signing operation on the provided payload bytes using the
     * private key of the certificate specified by the selected certificate ID.
     *
     * The algorithm used to generate the signature. This is by default the
     * SHA256withCVC-ECDSA algorithm, but it can be controlled through the
     * application.properties file.
     *
     * @param id            The ID of the certificate to use the private key of
     * @param algorithm     The algorithm to be used for the signature generation
     * @param payload       The payload bytes to be signed
     * @return The signed payload
     * @throws NoSuchAlgorithmException if the selected certificate algorithm is not found
     * @throws IOException for errors during the private key loading operation
     * @throws InvalidKeySpecException if the provided key specification is invalid
     * @throws SignatureException when the signature generation process fails
     * @throws InvalidKeyException if the key provided for the signature is invalid
     */
    public byte[] signContent(BigInteger id, String algorithm, byte[] payload) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, SignatureException, InvalidKeyException {
        // Pick up the certificate by the provided ID
        Certificate certificate = this.certificateRepo.findById(id)
                .orElseThrow(() ->
                    new DataNotFoundException(String.format("No Certificate found for the provided ID: %d", id))
                );

        // Create a new signature to sign the provided content
        Signature sign = Signature.getInstance(Optional.ofNullable(algorithm).orElse(this.defaultSigningAlgorithm));
        sign.initSign(X509Utils.privateKeyFromPem(certificate.getPrivateKey(), this.keyPairCurve));
        sign.update(payload);

        // Sign and return the signature
        return sign.sign();
    }

    /**
     * Attempts to verify the provided content using the signature specified. The
     * certificate used in this process is identified through the specified ID.
     *
     * @param id            The ID of the certificate to be used for the verification
     * @param algorithm     The algorithm to be used for the signature generation
     * @param payload       The payload to be verified
     * @param signature     The signature to verify the content with
     * @return Whether the content verification was successful or not
     * @throws NoSuchAlgorithmException if the selected certificate algorithm is not found
     * @throws IOException for errors during the public key loading operation
     * @throws InvalidKeySpecException  if the provided key specification is invalid
     * @throws SignatureException when the signature generation process fails
     * @throws InvalidKeyException if the key provided for the signature is invalid
     */
    public boolean verifyContent(BigInteger id, String algorithm, byte[] payload, byte[] signature) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, SignatureException, InvalidKeyException {
        // Pick up the certificate by the provided ID
        Certificate certificate = this.certificateRepo.findById(id)
                .orElseThrow(() ->
                        new DataNotFoundException(String.format("No Certificate found for the provided ID: %d", id))
                );

        // Create a new signature to sign the provided content
        Signature sign = Signature.getInstance(Optional.ofNullable(algorithm).orElse(this.defaultSigningAlgorithm));
        sign.initVerify(X509Utils.publicKeyFromPem(certificate.getPublicKey()));
        sign.update(payload);

        // Sign and return the signature
        return sign.verify(signature);
    }

}
