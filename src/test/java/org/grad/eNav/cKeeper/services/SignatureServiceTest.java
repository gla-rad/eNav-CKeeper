package org.grad.eNav.cKeeper.services;

import org.bouncycastle.operator.OperatorCreationException;
import org.grad.eNav.cKeeper.exceptions.InvalidRequestException;
import org.grad.eNav.cKeeper.exceptions.McpConnectivityException;
import org.grad.eNav.cKeeper.models.domain.Certificate;
import org.grad.eNav.cKeeper.models.domain.MrnEntity;
import org.grad.eNav.cKeeper.models.domain.Pair;
import org.grad.eNav.cKeeper.models.domain.mcp.McpEntityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.*;
import java.util.Base64;
import java.io.IOException;
import java.math.BigInteger;
import java.security.spec.InvalidKeySpecException;
import java.util.Collections;
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

        // Create a new Certificate DTO
        this.certificate = new Certificate();
        this.certificate.setId(BigInteger.ONE);
        this.certificate.setMrnEntity(this.mrnEntity);
        this.certificate.setStartDate(new Date());
        this.certificate.setEndDate(new Date());

        // Generate a dummy content and signature
        this.content = MessageDigest.getInstance("SHA-256").digest(("Hello World").getBytes());
        this.signature = MessageDigest.getInstance("SHA-256").digest(("That's the signature?").getBytes());
    }

    /**
     * Test that we can correctly generate a signature for the provided entity
     * MMSI and the content we want to sign.
     */
    @Test
    void testGenerateEntitySignature() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, SignatureException, InvalidKeyException {
        doReturn(this.mrnEntity).when(this.mrnEntityService).findOneByName(this.mrnEntity.getName());
        doReturn(Collections.singleton(this.certificate)).when(this.certificateService).findAllByMrnEntityId(this.mrnEntity.getId());
        doReturn(this.signature).when(this.certificateService).signContent(mrnEntity.getId(), this.content);

        // Perform the service call
        final Pair<String, byte[]> result = this.signatureService.generateEntitySignature(this.mrnEntity.getName(), this.mrnEntity.getMmsi(), this.mrnEntity.getEntityType(), this.content);

        // Assert the certificate
        assertEquals(this.certificate.getCertificate(), result.getKey());
        // Assert the signature equality byte by byte
        for(int i=0; i<this.signature.length; i++) {
            assertEquals(this.signature[i], result.getValue()[i]);
        }
    }

    /**
     * Test that we can correctly generate a signature for the provided entity
     * MMSI and the content we want to sign, even if the respective AtoN or
     * a certificate doesn't exist in our system. They should be created on
     * the fly.
     */
    @Test
    void testGenerateEntitySignatureAutoCreate() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, SignatureException, InvalidKeyException, InvalidAlgorithmParameterException, OperatorCreationException, McpConnectivityException {
        doReturn(this.mrnEntity.getMrn()).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());
        doReturn(this.mrnEntity).when(this.mrnEntityService).save(any());
        doReturn(Collections.emptySet()).when(this.certificateService).findAllByMrnEntityId(this.mrnEntity.getId());
        doReturn(this.certificate).when(this.certificateService).generateMrnEntityCertificate(this.mrnEntity.getId());
        doReturn(this.signature).when(this.certificateService).signContent(mrnEntity.getId(), this.content);

        // Perform the service call
        final Pair<String, byte[]> result = this.signatureService.generateEntitySignature(this.mrnEntity.getName(), this.mrnEntity.getMmsi(), this.mrnEntity.getEntityType(), this.content);

        // Assert the certificate
        assertEquals(this.certificate.getCertificate(), result.getKey());
        // Assert the signature equality byte by byte
        for(int i=0; i<this.signature.length; i++) {
            assertEquals(this.signature[i], result.getValue()[i]);
        }
    }

    /**
     * Test that we can correctly generate a signature for the provided entity
     * MMSI and the content we want to sign, even if the respective AtoN or
     * a certificate has been revoked. They should be created on the fly.
     */
    @Test
    void testGenerateEntitySignatureAutoCreateWhenRevoked() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, SignatureException, InvalidKeyException, InvalidAlgorithmParameterException, OperatorCreationException, McpConnectivityException {
        // Revoke the certificate
        this.certificate.setRevoked(Boolean.TRUE);

        doReturn(this.mrnEntity.getMrn()).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());
        doReturn(this.mrnEntity).when(this.mrnEntityService).save(any());
        doReturn(Collections.singleton(this.certificate)).when(this.certificateService).findAllByMrnEntityId(this.mrnEntity.getId());
        doReturn(this.certificate).when(this.certificateService).generateMrnEntityCertificate(this.mrnEntity.getId());
        doReturn(this.signature).when(this.certificateService).signContent(mrnEntity.getId(), this.content);

        // Perform the service call
        final Pair<String, byte[]> result = this.signatureService.generateEntitySignature(this.mrnEntity.getName(), this.mrnEntity.getMmsi(), this.mrnEntity.getEntityType(), this.content);

        // Assert that we called the certificate generation method
        verify(this.certificateService, times(1)).generateMrnEntityCertificate(this.mrnEntity.getId());

        // Assert the certificate
        assertEquals(this.certificate.getCertificate(), result.getKey());
        // Assert the signature equality byte by byte
        for(int i=0; i<this.signature.length; i++) {
            assertEquals(this.signature[i], result.getValue()[i]);
        }
    }

    /**
     * Test that if we cannot correctly generate a signature for the provided
     * entity MMSI and the content we want to sign, an InvalidRequestException
     * will be thrown.
     */
    @Test
    void testGenerateEntitySignatureFail() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, SignatureException, InvalidKeyException {
        doReturn(this.mrnEntity).when(this.mrnEntityService).findOneByName(this.mrnEntity.getName());
        doReturn(Collections.singleton(this.certificate)).when(this.certificateService).findAllByMrnEntityId(this.mrnEntity.getId());
        doThrow(SignatureException.class).when(this.certificateService).signContent(mrnEntity.getId(), this.content);

        // Perform the service call
        assertThrows(InvalidRequestException.class, () ->
            this.signatureService.generateEntitySignature(this.mrnEntity.getName(), this.mrnEntity.getMmsi(), this.mrnEntity.getEntityType(), this.content)
        );
    }

    /**
     * Test that we can correctly verify a signature for the provided entity
     * ID and the content we submit.
     */
    @Test
    void testVerifyEntitySignature() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, SignatureException, InvalidKeyException {
        doReturn(this.mrnEntity).when(this.mrnEntityService).findOneByName(this.mrnEntity.getName());
        doReturn(Collections.singleton(this.certificate)).when(this.certificateService).findAllByMrnEntityId(this.mrnEntity.getId());
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
        doReturn(Collections.singleton(this.certificate)).when(this.certificateService).findAllByMrnEntityId(this.mrnEntity.getId());
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