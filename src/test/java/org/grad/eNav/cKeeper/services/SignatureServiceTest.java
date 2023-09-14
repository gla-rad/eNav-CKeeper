package org.grad.eNav.cKeeper.services;

import org.grad.eNav.cKeeper.exceptions.InvalidRequestException;
import org.grad.eNav.cKeeper.models.domain.Certificate;
import org.grad.eNav.cKeeper.models.domain.MrnEntity;
import org.grad.eNav.cKeeper.models.domain.SignatureCertificate;
import org.grad.eNav.cKeeper.models.domain.mcp.McpEntityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SignatureServiceTest {

    /**
     * The Tested Service.
     */
    @InjectMocks
    @Spy
    SignatureService signatureService;

    /**
     * The MRN Entity Service mock.
     */
    @Mock
    MrnEntityService mrnEntityService;

    /**
     * The Certificate Service mock.
     */
    @Mock
    CertificateService certificateService;

    /**
     * The MCP Base Service mock.
     */
    @Mock
    McpConfigService mcpConfigService;

    // Test Variables
    private MrnEntity mrnEntity;
    private Certificate certificate;
    private SignatureCertificate signatureCertificate;
    private String algorithm;
    private byte[] content;
    private byte[] signature;

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setup() throws NoSuchAlgorithmException {
        // Create a new MRN entity DTO
        this.mrnEntity = new MrnEntity();
        this.mrnEntity.setId(BigInteger.ONE);
        this.mrnEntity.setName("mrn_entity");
        this.mrnEntity.setMmsi("123456789");
        this.mrnEntity.setMrn("mcp:prefix:" + this.mrnEntity.getName());
        this.mrnEntity.setEntityType(McpEntityType.DEVICE);

        // Create a new Certificate
        this.certificate = new Certificate();
        this.certificate.setId(BigInteger.ONE);
        this.certificate.setMrnEntity(this.mrnEntity);
        this.certificate.setStartDate(new Date());
        this.certificate.setEndDate(new Date());
        this.certificate.setCertificate("CERTIFICATE");
        this.certificate.setPublicKey("PUBLIC_KEY");
        this.certificate.setPrivateKey("PRIVATE_KEY");

        // Create a new Signature Certificate
        this.signatureCertificate = new SignatureCertificate();
        this.signatureCertificate.setCertificateId(this.certificate.getId());
        this.signatureCertificate.setCertificate(this.certificate.getCertificate());
        this.signatureCertificate.setPublicKey(this.certificate.getPublicKey());
        this.signatureCertificate.setRootCertificate("ROOT_CERTIFICATE");

        // Generate a dummy content and signature
        this.algorithm = "SHA256withECDSA";
        this.content = MessageDigest.getInstance("SHA-256").digest(("Hello World").getBytes());
        this.signature = MessageDigest.getInstance("SHA-256").digest(("That's the signature?").getBytes());
    }

    /**
     * Test that we can successfully retrieve the certificate information that
     * will be used to generate a signature, on demand, for a specific MRN
     * entity.
     */
    @Test
    void testGetSignatureCertificate() throws CertificateEncodingException {
        // Mock a root certificate
        X509Certificate rootCertificate = mock(X509Certificate.class);
        doReturn(new byte[]{0x01, 0x02, 0x03, 0x04}).when(rootCertificate).getEncoded();

        doReturn(this.mrnEntity).when(this.mrnEntityService).getOrCreate(any(), any(), any(), any(), any());
        doReturn(this.certificate).when(this.certificateService).getLatestOrCreate(this.mrnEntity.getId());
        doReturn(rootCertificate).when(this.certificateService).getTrustedCertificate(any());

        // Perform the service call
        SignatureCertificate result = this.signatureService.getSignatureCertificate(this.mrnEntity.getName(), this.mrnEntity.getMmsi(), this.mrnEntity.getVersion(), this.mrnEntity.getEntityType());

        // Assert that the signature certificate seems OK
        assertNotNull(result);
        assertEquals(this.certificate.getId(), result.getCertificateId());
        assertEquals(this.certificate.getCertificate(), result.getCertificate());
        assertEquals(this.certificate.getPublicKey(), result.getPublicKey());
        assertEquals(Base64.getEncoder().encodeToString(new byte[]{0x01, 0x02, 0x03, 0x04}), result.getRootCertificate());
    }

    /**
     * Test that we can correctly generate a signature for the provided entity
     * MMSI and the content we want to sign.
     */
    @Test
    void testGenerateEntitySignature() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, SignatureException, InvalidKeyException {
        doReturn(this.signature).when(this.certificateService).signContent(this.certificate.getId(), "SHA-256", this.content);

        // Perform the service call
        final byte[] result = this.signatureService.generateEntitySignature(this.certificate.getId(), "SHA-256", this.content);

        // Assert the signature equality byte by byte
        for(int i=0; i<this.signature.length; i++) {
            assertEquals(this.signature[i], result[i]);
        }
    }

    /**
     * Test that if we cannot correctly generate a signature for the provided
     * entity MMSI and the content we want to sign, an InvalidRequestException
     * will be thrown.
     */
    @Test
    void testGenerateEntitySignatureFail() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, SignatureException, InvalidKeyException {
        doThrow(SignatureException.class).when(this.certificateService).signContent(this.certificate.getId(), "SHA-256", this.content);

        // Perform the service call
        assertThrows(InvalidRequestException.class, () ->
            this.signatureService.generateEntitySignature(this.certificate.getId(), "SHA-256", this.content)
        );
    }

    /**
     * Test that we can correctly verify a signature for the provided entity
     * ID and the content we submit.
     */
    @Test
    void testVerifyEntitySignatureByMrn() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, SignatureException, InvalidKeyException {
        doReturn(this.mrnEntity).when(this.mrnEntityService).findOneByMrn(this.mrnEntity.getMrn());
        doReturn(this.certificate).when(this.certificateService).getLatestOrCreate(this.mrnEntity.getId());
        doReturn(Boolean.TRUE).when(this.certificateService).verifyContent(mrnEntity.getId(), this.algorithm, this.content, this.signature);

        // Perform the service call
        assertTrue(this.signatureService.verifyEntitySignatureByMrn(this.mrnEntity.getMrn(), this.algorithm, Base64.getEncoder().encodeToString(this.content), Base64.getEncoder().encodeToString((this.signature))));
    }

    /**
     * Test that we can correctly detect when a signature for the provided
     * entity ID and the content we submit is NOT valid.
     */
    @Test
    void testVerifyEntitySignatureByMrnFail() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, SignatureException, InvalidKeyException {
        doReturn(this.mrnEntity).when(this.mrnEntityService).findOneByMrn(this.mrnEntity.getMrn());
        doReturn(this.certificate).when(this.certificateService).getLatestOrCreate(this.mrnEntity.getId());
        doReturn(Boolean.FALSE).when(this.certificateService).verifyContent(mrnEntity.getId(), this.algorithm, this.content, this.signature);

        // Perform the service call
        assertFalse(this.signatureService.verifyEntitySignatureByMrn(this.mrnEntity.getMrn(), this.algorithm, Base64.getEncoder().encodeToString(this.content), Base64.getEncoder().encodeToString(this.signature)));
    }

    /**
     * Test that we can correctly verify a signature for the provided entity
     * MMSI and the content we submit.
     */
    @Test
    void testVerifyEntitySignatureByMmsi() {
        doReturn(this.mrnEntity).when(this.mrnEntityService).findOneByMmsi(this.mrnEntity.getMmsi());
        doReturn(Boolean.TRUE).when(this.signatureService).verifyEntitySignatureByMrn(eq(this.mrnEntity.getMrn()), any(), any(), any());

        // Perform the service call
        assertTrue(this.signatureService.verifyEntitySignatureByMmsi(this.mrnEntity.getMmsi(), this.algorithm, Base64.getEncoder().encodeToString(this.content), Base64.getEncoder().encodeToString((this.signature))));
    }

    /**
     * Test that we can correctly detect when a signature for the provided
     * entity MMSI and the content we submit is NOT valid.
     */
    @Test
    void testVerifyEntitySignatureByMmsiFail() {
        doReturn(this.mrnEntity).when(this.mrnEntityService).findOneByMmsi(this.mrnEntity.getMmsi());
        doReturn(Boolean.FALSE).when(this.signatureService).verifyEntitySignatureByMrn(eq(this.mrnEntity.getMrn()), any(), any(), any());

        // Perform the service call
        assertFalse(this.signatureService.verifyEntitySignatureByMmsi(this.mrnEntity.getMmsi(), this.algorithm, Base64.getEncoder().encodeToString(this.content), Base64.getEncoder().encodeToString(this.signature)));
    }

}