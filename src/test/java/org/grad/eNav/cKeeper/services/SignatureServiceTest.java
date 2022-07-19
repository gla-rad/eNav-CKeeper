package org.grad.eNav.cKeeper.services;

import org.bouncycastle.operator.OperatorCreationException;
import org.grad.eNav.cKeeper.exceptions.InvalidRequestException;
import org.grad.eNav.cKeeper.exceptions.McpConnectivityException;
import org.grad.eNav.cKeeper.models.dtos.CertificateDto;
import org.grad.eNav.cKeeper.models.dtos.MrnEntityDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
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
    private MrnEntityDto mrnEntityDto;
    private CertificateDto certificateDto;
    private byte[] content;
    private byte[] signature;

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setup() throws NoSuchAlgorithmException {
        // Create a new MRN entity DTO
        this.mrnEntityDto = new MrnEntityDto();
        this.mrnEntityDto.setId(BigInteger.ONE);
        this.mrnEntityDto.setName("mrn_entity");
        this.mrnEntityDto.setMmsi("123456789");
        this.mrnEntityDto.setMrn("mcp:prefix:" + this.mrnEntityDto.getName());

        // Create a new Certificate DTO
        this.certificateDto = new CertificateDto();
        this.certificateDto.setId(BigInteger.ONE);
        this.certificateDto.setMrnEntityId(this.mrnEntityDto.getId());
        this.certificateDto.setStartDate(new Date());
        this.certificateDto.setEndDate(new Date());

        // Generate a dummy content and signature
        this.content = MessageDigest.getInstance("SHA-256").digest(("Hello World").getBytes());
        this.signature = MessageDigest.getInstance("SHA-256").digest(("That's the signature?").getBytes());
    }

    /**
     * Test that we can correctly generate a signature for the provided AtoN
     * UID and the content we want to sign.
     */
    @Test
    void testGenerateAtonSignature() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, SignatureException, InvalidKeyException {
        doReturn(this.mrnEntityDto.getMrn()).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());
        doReturn(this.mrnEntityDto).when(this.mrnEntityService).findOneByMrn(this.mrnEntityDto.getMrn());
        doReturn(Collections.singleton(this.certificateDto)).when(this.certificateService).findAllByMrnEntityId(this.mrnEntityDto.getId());
        doReturn(this.signature).when(this.certificateService).signContent(mrnEntityDto.getId(), this.content);

        // Perform the service call
        final byte[] result = this.signatureService.generateAtonSignature(this.mrnEntityDto.getName(), this.mrnEntityDto.getMmsi(), this.content);

        // Assert the signature equality byte by byte
        for(int i=0; i<this.signature.length; i++) {
            assertEquals(this.signature[i], result[i]);
        }
    }

    /**
     * Test that we can correctly generate a signature for the provided AtoN
     * UID and the content we want to sign, even if the respective AtoN or
     * a certificate doesn't exist in our system. They should be created on
     * the fly.
     */
    @Test
    void testGenerateAtonSignatureAutoCreate() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, SignatureException, InvalidKeyException, InvalidAlgorithmParameterException, OperatorCreationException, McpConnectivityException {
        doReturn(this.mrnEntityDto.getMrn()).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());
        doReturn(this.mrnEntityDto).when(this.mrnEntityService).save(any());
        doReturn(Collections.emptySet()).when(this.certificateService).findAllByMrnEntityId(this.mrnEntityDto.getId());
        doReturn(this.certificateDto).when(this.certificateService).generateMrnEntityCertificate(any());
        doReturn(this.signature).when(this.certificateService).signContent(mrnEntityDto.getId(), this.content);

        // Perform the service call
        final byte[] result = this.signatureService.generateAtonSignature(this.mrnEntityDto.getName(), this.mrnEntityDto.getMmsi(), this.content);

        // Assert the signature equality byte by byte
        for(int i=0; i<this.signature.length; i++) {
            assertEquals(this.signature[i], result[i]);
        }
    }

    /**
     * Test that we can correctly generate a signature for the provided AtoN
     * UID and the content we want to sign, even if the respective AtoN or
     * a certificate has been revoked. They should be created on the fly.
     */
    @Test
    void testGenerateAtonSignatureAutoCreateWhenRevoked() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, SignatureException, InvalidKeyException, InvalidAlgorithmParameterException, OperatorCreationException, McpConnectivityException {
        // Revoke the certificate
        this.certificateDto.setRevoked(Boolean.TRUE);

        doReturn(this.mrnEntityDto.getMrn()).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());
        doReturn(this.mrnEntityDto).when(this.mrnEntityService).save(any());
        doReturn(Collections.singleton(this.certificateDto)).when(this.certificateService).findAllByMrnEntityId(this.mrnEntityDto.getId());
        doReturn(this.certificateDto).when(this.certificateService).generateMrnEntityCertificate(any());
        doReturn(this.signature).when(this.certificateService).signContent(mrnEntityDto.getId(), this.content);

        // Perform the service call
        final byte[] result = this.signatureService.generateAtonSignature(this.mrnEntityDto.getName(), this.mrnEntityDto.getMmsi(), this.content);

        // Assert that we called the certificate generation method
        verify(this.certificateService, times(1)).generateMrnEntityCertificate(this.mrnEntityDto.getId());

        // Assert the signature equality byte by byte
        for(int i=0; i<this.signature.length; i++) {
            assertEquals(this.signature[i], result[i]);
        }
    }

    /**
     * Test that if we cannot correctly generate a signature for the provided
     * AtoN UID and the content we want to sign, an InvalidRequestException
     * will be thrown.
     */
    @Test
    void testGenerateAtonSignatureFail() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, SignatureException, InvalidKeyException {
        doReturn(this.mrnEntityDto.getMrn()).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());
        doReturn(this.mrnEntityDto).when(this.mrnEntityService).findOneByMrn(this.mrnEntityDto.getMrn());
        doReturn(Collections.singleton(this.certificateDto)).when(this.certificateService).findAllByMrnEntityId(this.mrnEntityDto.getId());
        doThrow(SignatureException.class).when(this.certificateService).signContent(mrnEntityDto.getId(), this.content);

        // Perform the service call
        assertThrows(InvalidRequestException.class, () ->
            this.signatureService.generateAtonSignature(this.mrnEntityDto.getName(), this.mrnEntityDto.getMmsi(), this.content)
        );
    }

    /**
     * Test that we can correctly verify a signature for the provided entity
     * MMSI and the content we submit.
     */
    @Test
    void testVerifyMmsiSignature() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, SignatureException, InvalidKeyException {
        doReturn(this.mrnEntityDto).when(this.mrnEntityService).findOneByMmsi(this.mrnEntityDto.getMmsi());
        doReturn(Collections.singleton(this.certificateDto)).when(this.certificateService).findAllByMrnEntityId(this.mrnEntityDto.getId());
        doReturn(Boolean.TRUE).when(this.certificateService).verifyContent(mrnEntityDto.getId(), this.content, this.signature);

        // Perform the service call
        assertTrue(this.signatureService.verifyMmsiSignature(this.mrnEntityDto.getMmsi(), Base64.getEncoder().encodeToString(this.content), Base64.getEncoder().encodeToString((this.signature))));
    }

    /**
     * Test that we can correctly detect when a signature for the provided
     * entity MMSI and the content we submit is NOT valid.
     */
    @Test
    void testVerifyMmsiSignatureFail() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, SignatureException, InvalidKeyException {
        doReturn(this.mrnEntityDto).when(this.mrnEntityService).findOneByMmsi(this.mrnEntityDto.getMmsi());
        doReturn(Collections.singleton(this.certificateDto)).when(this.certificateService).findAllByMrnEntityId(this.mrnEntityDto.getId());
        doReturn(Boolean.FALSE).when(this.certificateService).verifyContent(mrnEntityDto.getId(), this.content, this.signature);

        // Perform the service call
        assertFalse(this.signatureService.verifyMmsiSignature(this.mrnEntityDto.getMmsi(), Base64.getEncoder().encodeToString(this.content), Base64.getEncoder().encodeToString(this.signature)));
    }

}