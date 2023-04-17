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

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.grad.eNav.cKeeper.exceptions.DataNotFoundException;
import org.grad.eNav.cKeeper.exceptions.McpConnectivityException;
import org.grad.eNav.cKeeper.exceptions.SavingFailedException;
import org.grad.eNav.cKeeper.models.domain.Certificate;
import org.grad.eNav.cKeeper.models.domain.MrnEntity;
import org.grad.eNav.cKeeper.models.domain.Pair;
import org.grad.eNav.cKeeper.models.domain.mcp.McpEntityType;
import org.grad.eNav.cKeeper.repos.CertificateRepo;
import org.grad.eNav.cKeeper.repos.MRNEntityRepo;
import org.grad.eNav.cKeeper.utils.X509Utils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CertificateServiceTest {

    /**
     * The Tested Service.
     */
    @InjectMocks
    @Spy
    CertificateService certificateService;

    /**
     * The Certificate Repo mock.
     */
    @Mock
    CertificateRepo certificateRepo;

    /**
     * The MRN Entity Repo mock.
     */
    @Mock
    MRNEntityRepo mrnEntityRepo;

    /**
     * The MCP Service mock.
     */
    @Mock
    McpService mcpService;

    // Test Variables
    private Certificate certificate;
    private Certificate newCertificate;
    private MrnEntity mrnEntity;

    /**
     * Add the Bouncy Castle as a security provider for the unit tests.
     */
    @BeforeAll
    static void addSecurityProvider() {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setUp() {
        // Create an existing MRN entity
        this.mrnEntity = new MrnEntity();
        this.mrnEntity.setId(BigInteger.ONE);
        this.mrnEntity.setName("Existing Entity Name");
        this.mrnEntity.setMrn("urn:mrn:mcp:device:mcc:grad:test");
        this.mrnEntity.setEntityType(McpEntityType.DEVICE);

        // Create an existing certificate object
        this.certificate = new Certificate();
        this.certificate.setId(BigInteger.ONE);
        this.certificate.setMcpMirId("1234567890");
        this.certificate.setMrnEntity(this.mrnEntity);
        this.certificate.setCertificate("CERTIFICATE");
        this.certificate.setPublicKey("PUBLIC KEY");
        this.certificate.setPrivateKey("PRIVATE KEY");
        this.certificate.setStartDate(new Date(new Date().getTime() - 1000));
        this.certificate.setEndDate(new Date(new Date().getTime() + 1000));
        this.certificate.setRevoked(Boolean.FALSE);

        // Create a new certificate object
        this.newCertificate = new Certificate();
        this.newCertificate.setId(BigInteger.TEN);
        this.newCertificate.setMcpMirId("2345678901");
        this.newCertificate.setMrnEntity(this.mrnEntity);
        this.newCertificate.setCertificate("NEW CERTIFICATE");
        this.newCertificate.setPublicKey("NEW PUBLIC KEY");
        this.newCertificate.setPrivateKey("NEW PRIVATE KEY");
        this.newCertificate.setStartDate(new Date());
        this.newCertificate.setEndDate(new Date(this.certificate.getStartDate().getTime() + 1000));
        this.newCertificate.setRevoked(Boolean.FALSE);
    }

    /**
     * Test that we can sync up with the latest certificates comming in from the
     * MCP Identity Registry. Of course, if an existing local certificate
     * doesn't exist in the MCP any more, that should be revoked.
     */
    @Test
    void testSyncMrnEntityWithMcpMir() throws McpConnectivityException, IOException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, CertificateException, OperatorCreationException {
        // Add the certificate to the MRN Entity
        this.mrnEntity.setCertificates(Collections.singleton(this.certificate));

        // Create a new test certificate
        KeyPair keypair = X509Utils.generateKeyPair(null);
        PKCS10CertificationRequest csr = X509Utils.generateX509CSR(keypair, "CN=Test", null);
        X509Certificate cert = X509Utils.generateX509Certificate(keypair, "CN=Test", new Date(), new Date(), null);

        // Mock the internal calls
        doReturn(Optional.of(this.mrnEntity)).when(this.mrnEntityRepo).findById(this.mrnEntity.getId());
        doReturn(Collections.singletonMap(String.valueOf(cert.getSerialNumber()), cert)).when(this.mcpService).getMcpEntityCertificates(McpEntityType.DEVICE, this.mrnEntity.getMrn(), null);

        // Perform the service call
        this.certificateService.syncMrnEntityWithMcpMir(this.mrnEntity.getId());

        // Make sure we saved twice, once for the existing and one for the new certificate
        verify(this.certificateRepo, times(2)).save(any());
    }

    /**
     * Test that if nothing has changes and we have no ceritifates to sync with
     * no saving whatsoever will take place.
     */
    @Test
    void testSyncMrnEntityWithMcpMirNoChange() throws McpConnectivityException, IOException {
        // Mock the internal calls
        doReturn(Optional.of(this.mrnEntity)).when(this.mrnEntityRepo).findById(this.mrnEntity.getId());
        doReturn(Collections.emptyMap()).when(this.mcpService).getMcpEntityCertificates(McpEntityType.DEVICE, this.mrnEntity.getMrn(), null);

        // Perform the service call
        this.certificateService.syncMrnEntityWithMcpMir(this.mrnEntity.getId());

        // Make sure we don't do any saving since nothing has changed
        verifyNoInteractions(this.certificateRepo);
    }

    /**
     * Test that we can retrieve all the certificates associated with a specific
     * MRN entity, using the MRN entity ID.
     */
    @Test
    void testFindAllByMrnEntityId() {
        doReturn(Collections.singleton(this.certificate)).when(certificateRepo).findAllByMrnEntityId(this.mrnEntity.getId());

        // Perform the service call
        Set<Certificate> result = this.certificateService.findAllByMrnEntityId(this.mrnEntity.getId());

        // Make sure the response is correct
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(this.certificate, result.toArray()[0]);
    }

    /**
     * Test that we can correctly retrieve the root certificate from the
     * truststore in the classpath..
     */
    @Test
    void testGetTrustedCertificate() {
        // Initialise the service parameters
        this.certificateService.trustStore="truststore.jks";
        this.certificateService.trustStorePassword="password";
        this.certificateService.trustStoreType="JKS";

        // Call the service
        X509Certificate certificate = this.certificateService.getTrustedCertificate("test-cert");

        // Make sure the thumbprint looks OK
        assertNotNull(certificate);
        assertNotNull(certificate.getPublicKey());
        assertNotNull(certificate.getIssuerX500Principal());
        assertNotNull(certificate.getSubjectX500Principal());
        assertEquals("SHA256withRSA", certificate.getSigAlgName());
    }

    /**
     * Test that we can correctly generate a new X.509 certificate for a given
     * MRN entity, based on the provided MRN entity ID.
     */
    @Test
    void testGenerateMrnEntityCertificate() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, CertificateException, OperatorCreationException, IOException, McpConnectivityException {
        // Spin up a self-signed
        this.certificateService.certDirName="CN=Test";
        KeyPair keyPair = X509Utils.generateKeyPair(null);
        X509Certificate x509Certificate = X509Utils.generateX509Certificate(keyPair, this.certificateService.certDirName, new Date(), new Date(), null);

        doReturn(Optional.of(this.mrnEntity)).when(this.mrnEntityRepo).findById(this.mrnEntity.getId());
        doReturn(new Pair<>(this.certificate.getMcpMirId(), x509Certificate)).when(this.mcpService).issueMcpEntityCertificate(eq(McpEntityType.DEVICE), eq(this.mrnEntity.getMrn()), eq(this.mrnEntity.getVersion()), any());
        doReturn(this.certificate).when(this.certificateRepo).save(any());

        // Perform the service call
        Certificate result = this.certificateService.generateMrnEntityCertificate(this.mrnEntity.getId());

        // Make sure the response is correct
        assertNotNull(result);
        assertEquals(this.certificate.getId(), result.getId());
        assertEquals(this.certificate.getMrnEntity().getId(), result.getMrnEntity().getId());
        assertEquals(this.certificate.getPublicKey(), result.getPublicKey());
        assertEquals(this.certificate.getStartDate(), result.getStartDate());
        assertEquals(this.certificate.getEndDate(), result.getEndDate());
        assertEquals(this.certificate.getRevoked(), result.getRevoked());
    }

    /**
     * Test that if we attempt to generate a certificate for an MRN entity that
     * does not exist, a DataNotFoundException will be thrown.
     */
    @Test
    void testGenerateMrnEntityCertificateNoEntity() {
        doReturn(Optional.empty()).when(this.mrnEntityRepo).findById(any());

        // Perform the service call
        assertThrows(DataNotFoundException.class, () ->
                this.certificateService.generateMrnEntityCertificate(this.mrnEntity.getId())
        );
    }

    /**
     * Test that even if we manage to generate a successful X.509 certificate
     * for our MRN entity, if the service fails to save the result in the
     * database, a SavingFailedException will be thrown.
     */
    @Test
    void testGenerateMrnEntityCertificateSaveFailed() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, CertificateException, OperatorCreationException, IOException, McpConnectivityException {
        // Spin up a self-signed
        this.certificateService.certDirName="CN=Test";
        KeyPair keyPair = X509Utils.generateKeyPair(null);
        X509Certificate x509Certificate = X509Utils.generateX509Certificate(keyPair, this.certificateService.certDirName, new Date(), new Date(), null);

        doReturn(Optional.of(this.mrnEntity)).when(this.mrnEntityRepo).findById(this.mrnEntity.getId());
        doReturn(new Pair<>(this.certificate.getMcpMirId(), x509Certificate)).when(this.mcpService).issueMcpEntityCertificate(eq(McpEntityType.DEVICE), eq(this.mrnEntity.getMrn()), any(), any());

        // Perform the service call
        assertThrows(SavingFailedException.class, () ->
                this.certificateService.generateMrnEntityCertificate(this.mrnEntity.getId())
        );
    }

    /**
     * Test that we can successfully delete an existing certificate entry from
     * the database.
     *
     * TODO: This operation should also revoke the certificate from the MCP MIR
     * but for now let's go with this.
     */
    @Test
    void testDelete() {
        doReturn(Boolean.TRUE).when(this.certificateRepo).existsById(this.certificate.getId());

        // Perform the service call
        this.certificateService.delete(this.certificate.getId());
    }

    /**
     * Test that if the certificate to be deleted is not found, based on the
     * provided ID, a DataNotFoundException will be thrown.
     */
    @Test
    void testDeleteNotFound() {
        doReturn(Boolean.FALSE).when(this.certificateRepo).existsById(this.certificate.getId());

        // Perform the service call
        assertThrows(DataNotFoundException.class, () ->
                this.certificateService.delete(this.certificate.getId())
        );
    }

    /**
     * Test that we can actually revoke a certificate based on the provided
     * certificate ID. Note that this operation should also revoke the
     * certificate from the MCP MIR, but currently we don't have the MIR's
     * certificate ID, so we can't actually do that.
     */
    @Test
    void testRevoke() throws IOException, McpConnectivityException {
        doReturn(Optional.of(this.certificate)).when(this.certificateRepo).findById(this.certificate.getId());
        doReturn(this.certificate).when(this.certificateRepo).save(any());

        // Perform the service call
        Certificate result = this.certificateService.revoke(this.mrnEntity.getId());

        // Make sure the response is correct
        assertNotNull(result);
        assertEquals(this.certificate.getId(), result.getId());
        assertEquals(this.certificate.getMrnEntity().getId(), result.getMrnEntity().getId());
        assertEquals(this.certificate.getPublicKey(), result.getPublicKey());
        assertEquals(this.certificate.getStartDate(), result.getStartDate());
        assertEquals(this.certificate.getEndDate(), result.getEndDate());
        assertEquals(Boolean.TRUE, result.getRevoked());
    }

    /**
     * Test that if we try revoke a certificate based on the provided
     * certificate ID and this does NOT exist, then a DataNotFoundException
     * will be thrown.
     */
    @Test
    void testRevokeNotFound() throws IOException {
        doReturn(Optional.empty()).when(this.certificateRepo).findById(this.certificate.getId());

        // Perform the service call
        assertThrows(DataNotFoundException.class, () ->
            this.certificateService.revoke(this.mrnEntity.getId())
        );
    }

    /**
     * Test that even if we actually revoke a certificate based on the provided
     * certificate ID, but the local database entry fails to be updates, a
     * SavingFailedException will be thrown.
     */
    @Test
    void testRevokeSaveFailed() throws IOException {
        doReturn(Optional.of(this.certificate)).when(this.certificateRepo).findById(this.certificate.getId());

        // Perform the service call
        assertThrows(SavingFailedException.class, () ->
                this.certificateService.revoke(this.mrnEntity.getId())
        );
    }

    /**
     * Test that when the latest certificate we have for an MRN Entity is valid,
     * it will be returned as expected.
     */
    @Test
    void testGetLatestOrCreate() {
        doReturn(Collections.singleton(this.certificate)).when(this.certificateService).findAllByMrnEntityId(any());

        // Perform the service call
        Certificate result = this.certificateService.getLatestOrCreate(this.mrnEntity.getId());

        // Assert that the resulting certificate seems correct
        assertNotNull(result);
        assertEquals(this.certificate.getId(), result.getId());
        assertEquals(this.certificate.getStartDate(), result.getStartDate());
        assertEquals(this.certificate.getEndDate(), result.getEndDate());
        assertEquals(this.certificate.getPrivateKey(), result.getPrivateKey());
        assertEquals(this.certificate.getPublicKey(), result.getPublicKey());
        assertEquals(this.certificate.getCertificate(), result.getCertificate());
        assertEquals(this.certificate.getMcpMirId(), result.getMcpMirId());
        assertEquals(this.certificate.getRevoked(), result.getRevoked());
    }

    /**
     * Test that when the latest certificate we have for an MRN Entity has an
     * invalid start date, a new one will be generated on demand.
     */
    @Test
    void testGetLatestOrCreateStartDateWrong() throws InvalidAlgorithmParameterException, McpConnectivityException, NoSuchAlgorithmException, IOException, OperatorCreationException {
        // Set the start date to be in the future
        this.certificate.setStartDate(this.certificate.getEndDate());
        doReturn(Collections.singleton(this.certificate)).when(this.certificateService).findAllByMrnEntityId(any());
        doReturn(this.newCertificate).when(this.certificateService).generateMrnEntityCertificate(this.mrnEntity.getId());

        // Perform the service call
        Certificate result = this.certificateService.getLatestOrCreate(this.mrnEntity.getId());

        // Assert that the resulting certificate seems correct
        assertNotNull(result);
        assertEquals(this.newCertificate.getId(), result.getId());
        assertEquals(this.newCertificate.getStartDate(), result.getStartDate());
        assertEquals(this.newCertificate.getEndDate(), result.getEndDate());
        assertEquals(this.newCertificate.getPrivateKey(), result.getPrivateKey());
        assertEquals(this.newCertificate.getPublicKey(), result.getPublicKey());
        assertEquals(this.newCertificate.getCertificate(), result.getCertificate());
        assertEquals(this.newCertificate.getMcpMirId(), result.getMcpMirId());
        assertEquals(this.newCertificate.getRevoked(), result.getRevoked());
    }

    /**
     * Test that when the latest certificate we have for an MRN Entity has an
     * invalid end date, a new one will be generated on demand.
     */
    @Test
    void testGetLatestOrCreateEndDateWrong() throws InvalidAlgorithmParameterException, McpConnectivityException, NoSuchAlgorithmException, IOException, OperatorCreationException {
        // Set the start date to be in the future
        this.certificate.setEndDate(this.certificate.getStartDate());
        doReturn(Collections.singleton(this.certificate)).when(this.certificateService).findAllByMrnEntityId(any());
        doReturn(this.newCertificate).when(this.certificateService).generateMrnEntityCertificate(this.mrnEntity.getId());

        // Perform the service call
        Certificate result = this.certificateService.getLatestOrCreate(this.mrnEntity.getId());

        // Assert that the resulting certificate seems correct
        assertNotNull(result);
        assertEquals(this.newCertificate.getId(), result.getId());
        assertEquals(this.newCertificate.getStartDate(), result.getStartDate());
        assertEquals(this.newCertificate.getEndDate(), result.getEndDate());
        assertEquals(this.newCertificate.getPrivateKey(), result.getPrivateKey());
        assertEquals(this.newCertificate.getPublicKey(), result.getPublicKey());
        assertEquals(this.newCertificate.getCertificate(), result.getCertificate());
        assertEquals(this.newCertificate.getMcpMirId(), result.getMcpMirId());
        assertEquals(this.newCertificate.getRevoked(), result.getRevoked());
    }

    /**
     * Test that when the latest certificate we have for an MRN Entity has been
     * revoked, a new one will be generated on demand.
     */
    @Test
    void testGetLatestOrCreateRevoked() throws InvalidAlgorithmParameterException, McpConnectivityException, NoSuchAlgorithmException, IOException, OperatorCreationException {
        // Set the start date to be in the future
        this.certificate.setRevoked(Boolean.TRUE);
        doReturn(Collections.singleton(this.certificate)).when(this.certificateService).findAllByMrnEntityId(any());
        doReturn(this.newCertificate).when(this.certificateService).generateMrnEntityCertificate(this.mrnEntity.getId());

        // Perform the service call
        Certificate result = this.certificateService.getLatestOrCreate(this.mrnEntity.getId());

        // Assert that the resulting certificate seems correct
        assertNotNull(result);
        assertEquals(this.newCertificate.getId(), result.getId());
        assertEquals(this.newCertificate.getStartDate(), result.getStartDate());
        assertEquals(this.newCertificate.getEndDate(), result.getEndDate());
        assertEquals(this.newCertificate.getPrivateKey(), result.getPrivateKey());
        assertEquals(this.newCertificate.getPublicKey(), result.getPublicKey());
        assertEquals(this.newCertificate.getCertificate(), result.getCertificate());
        assertEquals(this.newCertificate.getMcpMirId(), result.getMcpMirId());
        assertEquals(this.newCertificate.getRevoked(), result.getRevoked());
    }

    /**
     * Test that we can successfully right any byte array content using
     * a certificate identified by the certificate ID.
     */
    @Test
    void testSignContent() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, CertificateException, OperatorCreationException, IOException, InvalidKeySpecException, SignatureException, InvalidKeyException {
        // Initialise the service parameters
        this.certificateService.keyPairCurve="secp256r1";
        this.certificateService.certDirName="CN=Test";

        // Spin up a self-signed certificate
        final KeyPair keyPair = X509Utils.generateKeyPair(this.certificateService.keyPairCurve);
        final X509Certificate x509Certificate = X509Utils.generateX509Certificate(keyPair, this.certificateService.certDirName, new Date(), new Date(), this.certificateService.defaultSigningtAlgorithm);

        // Populate the mock certificate with the actual keys
        this.certificate.setCertificate(X509Utils.formatCertificate(x509Certificate));
        this.certificate.setPublicKey(X509Utils.formatPublicKey(keyPair.getPublic()));
        this.certificate.setPrivateKey(X509Utils.formatPrivateKey(keyPair.getPrivate()));

        // Initialise the verification signature
        final Signature sign = Signature.getInstance("SHA256WITHECDSA");
        sign.initVerify(keyPair.getPublic());

        // Create a dummy payload
        final byte[] payload = MessageDigest.getInstance("SHA-256").digest(("Hello World").getBytes());

        // Mock the service database call
        doReturn(Optional.of(this.certificate)).when(this.certificateRepo).findById(this.certificate.getId());

        // Perform the service call
        final byte[] signature = this.certificateService.signContent(this.certificate.getId(), sign.getAlgorithm(), payload);

        // Verify that the signature is correct
        sign.update(payload);
        assertTrue(sign.verify(signature));
    }

    /**
     * Test that when we are signing a payload, we if the provided certificate
     * ID does not match an entry in the database, a DataNotFoundException will
     * be thrown.
     */
    @Test
    void testSignContentCertificateNotFound() throws NoSuchAlgorithmException {
        // Create a dummy payload
        final byte[] payload = MessageDigest.getInstance("SHA-256").digest(("Hello World").getBytes());

        // Perform the service call
        assertThrows(DataNotFoundException.class, () ->
                this.certificateService.signContent(this.certificate.getId(), "SHA-256", payload)
        );
    }

    /**
     * Test that we can successfully verify an appropriate content if the
     * provided signature matched the specified certificate.
     */
    @Test
    void testVerifyContent() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, CertificateException, OperatorCreationException, IOException, InvalidKeyException, SignatureException, InvalidKeySpecException {
        // Initialise the service parameters
        this.certificateService.keyPairCurve="secp256r1";
        this.certificateService.defaultSigningtAlgorithm ="SHA256WITHECDSA";
        this.certificateService.certDirName="CN=Test";

        // Spin up a self-signed certificate
        final KeyPair keyPair = X509Utils.generateKeyPair(this.certificateService.keyPairCurve);
        final X509Certificate x509Certificate = X509Utils.generateX509Certificate(keyPair, this.certificateService.certDirName, new Date(), new Date(), this.certificateService.defaultSigningtAlgorithm);

        // Populate the mock certificate with the actual keys
        this.certificate.setCertificate(X509Utils.formatCertificate(x509Certificate));
        this.certificate.setPublicKey(X509Utils.formatPublicKey(keyPair.getPublic()));
        this.certificate.setPrivateKey(X509Utils.formatPrivateKey(keyPair.getPrivate()));

        // Initialise the signing signature
        final Signature sign = Signature.getInstance(this.certificateService.defaultSigningtAlgorithm);
        sign.initSign(keyPair.getPrivate());

        // Create a dummy payload
        final byte[] payload = MessageDigest.getInstance("SHA-256").digest(("Hello World").getBytes());

        // Mock the service database call
        doReturn(Optional.of(this.certificate)).when(this.certificateRepo).findById(this.certificate.getId());

        // Create the signature bytes
        sign.update(payload);
        byte[] signature = sign.sign();

        // Verify that the signature is correct
        assertTrue(this.certificateService.verifyContent(certificate.getId(), payload, signature));
    }

    /**
     * Test that when we are verifying a content, we if the provided certificate
     * ID does not match an entry in the database, a DataNotFoundException will
     * be thrown.
     */
    @Test
    void testVerifyContentCertificateNotFound() throws NoSuchAlgorithmException {
        // Create a dummy payload
        final byte[] payload = MessageDigest.getInstance("SHA-256").digest(("Hello World").getBytes());

        // Perform the service call
        assertThrows(DataNotFoundException.class, () ->
                this.certificateService.verifyContent(this.certificate.getId(), payload, null)
        );
    }

}