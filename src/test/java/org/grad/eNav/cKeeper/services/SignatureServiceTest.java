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

        // Create a new Signature Certificate
        this.signatureCertificate = new SignatureCertificate();
        this.signatureCertificate.setCertificateId(this.certificate.getId());
        this.signatureCertificate.setCertificate(this.certificate.getCertificate());
        this.signatureCertificate.setPublicKey(this.certificate.getPublicKey());
        this.signatureCertificate.setRootCertificateThumbprint("rootCertificateThumbprint");

        // Generate a dummy content and signature
        this.content = MessageDigest.getInstance("SHA-256").digest(("Hello World").getBytes());
        this.signature = MessageDigest.getInstance("SHA-256").digest(("That's the signature?").getBytes());
    }

    /**
     * Test that we can successfully retrieve the certificate information that
     * will be used to generate a signature, on demand, for a specific MRN
     * entity.
     */
    @Test
    void testGetSignatureCertificate() {
        doReturn(this.mrnEntity).when(this.mrnEntityService).getOrCreate(any(), any(), any(), any());
        doReturn(this.certificate).when(this.certificateService).getLatestOrCreate(this.mrnEntity.getId());
        doReturn(this.signatureCertificate.getRootCertificateThumbprint()).when(this.certificateService).getTrustedCertificateThumbprint(any(), any());

        // Perform the service call
        SignatureCertificate result = this.signatureService.getSignatureCertificate(this.mrnEntity.getName(), this.mrnEntity.getMmsi(), this.mrnEntity.getEntityType());

        // Assert that the signature certificate seems OK
        assertNotNull(result);
        assertEquals(this.signatureCertificate.getCertificateId(), result.getCertificateId());
        assertEquals(this.signatureCertificate.getCertificate(), result.getCertificate());
        assertEquals(this.signatureCertificate.getPublicKey(), result.getPublicKey());
        assertEquals(this.signatureCertificate.getRootCertificateThumbprint(), result.getRootCertificateThumbprint());
    }

    /**
     * Test that we can correctly generate a signature for the provided entity
     * MMSI and the content we want to sign.
     */
    @Test
    void testGenerateEntitySignature() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, SignatureException, InvalidKeyException {
        doReturn(this.signatureCertificate).when(this.signatureService).getSignatureCertificate(any(), any(), any());
        doReturn(this.signature).when(this.certificateService).signContent(mrnEntity.getId(), this.content);

        // Perform the service call
        final byte[] result = this.signatureService.generateEntitySignature(this.mrnEntity.getName(),
                this.mrnEntity.getMmsi(), this.mrnEntity.getEntityType(), null, this.content);

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
        doReturn(this.signatureCertificate).when(this.signatureService).getSignatureCertificate(any(), any(), any());
        doThrow(SignatureException.class).when(this.certificateService).signContent(mrnEntity.getId(), this.content);

        // Perform the service call
        assertThrows(InvalidRequestException.class, () ->
            this.signatureService.generateEntitySignature(this.mrnEntity.getName(),
                    this.mrnEntity.getMmsi(), this.mrnEntity.getEntityType(), null, this.content)
        );
    }

    /**
     * Test that we can correctly verify a signature for the provided entity
     * ID and the content we submit.
     */
    @Test
    void testVerifyEntitySignature() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, SignatureException, InvalidKeyException {
        doReturn(this.mrnEntity).when(this.mrnEntityService).findOneByName(this.mrnEntity.getName());
        doReturn(this.certificate).when(this.certificateService).getLatestOrCreate(this.mrnEntity.getId());
        doReturn(Boolean.TRUE).when(this.certificateService).verifyContent(mrnEntity.getId(), this.content, this.signature);

        // Perform the service call
        assertTrue(this.signatureService.verifyEntitySignature(this.mrnEntity.getName(), Base64.getEncoder().encodeToString(this.content), Base64.getEncoder().encodeToString((this.signature))));
    }

    /**
     * Test that we can correctly detect when a signature for the provided
     * entity ID and the content we submit is NOT valid.
     */
    @Test
    void testVerifyEntitySignatureFail() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, SignatureException, InvalidKeyException {
        doReturn(this.mrnEntity).when(this.mrnEntityService).findOneByName(this.mrnEntity.getName());
        doReturn(this.certificate).when(this.certificateService).getLatestOrCreate(this.mrnEntity.getId());
        doReturn(Boolean.FALSE).when(this.certificateService).verifyContent(mrnEntity.getId(), this.content, this.signature);

        // Perform the service call
        assertFalse(this.signatureService.verifyEntitySignature(this.mrnEntity.getName(), Base64.getEncoder().encodeToString(this.content), Base64.getEncoder().encodeToString(this.signature)));
    }

    /**
     * Test that we can correctly verify a signature for the provided entity
     * MMSI and the content we submit.
     */
    @Test
    void testVerifyEntitySignatureByMmsi() {
        doReturn(this.mrnEntity).when(this.mrnEntityService).findOneByMmsi(this.mrnEntity.getMmsi());
        doReturn(Boolean.TRUE).when(this.signatureService).verifyEntitySignature(eq(this.mrnEntity.getName()), any(), any());

        // Perform the service call
        assertTrue(this.signatureService.verifyEntitySignatureByMmsi(this.mrnEntity.getMmsi(), Base64.getEncoder().encodeToString(this.content), Base64.getEncoder().encodeToString((this.signature))));
    }

    /**
     * Test that we can correctly detect when a signature for the provided
     * entity MMSI and the content we submit is NOT valid.
     */
    @Test
    void testVerifyEntitySignatureByMmsiFail() {
        doReturn(this.mrnEntity).when(this.mrnEntityService).findOneByMmsi(this.mrnEntity.getMmsi());
        doReturn(Boolean.FALSE).when(this.signatureService).verifyEntitySignature(eq(this.mrnEntity.getName()), any(), any());

        // Perform the service call
        assertFalse(this.signatureService.verifyEntitySignatureByMmsi(this.mrnEntity.getMmsi(), Base64.getEncoder().encodeToString(this.content), Base64.getEncoder().encodeToString(this.signature)));
    }

}