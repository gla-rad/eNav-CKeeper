package org.grad.eNav.cKeeper.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.grad.eNav.cKeeper.TestingConfiguration;
import org.grad.eNav.cKeeper.models.domain.SignatureCertificate;
import org.grad.eNav.cKeeper.models.domain.mcp.McpEntityType;
import org.grad.eNav.cKeeper.models.dtos.SignatureCertificateDto;
import org.grad.eNav.cKeeper.models.dtos.SignatureVerificationRequestDto;
import org.grad.eNav.cKeeper.services.CertificateService;
import org.grad.eNav.cKeeper.services.SignatureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(controllers = SignatureController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@Import(TestingConfiguration.class)
class SignatureControllerTest {

    /**
     * The Mock MVC.
     */
    @Autowired
    MockMvc mockMvc;

    /**
     * The JSON Object Mapper.
     */
    @Autowired
    ObjectMapper objectMapper;

    /**
     * The Certificate Service mock.
     */
    @MockBean
    CertificateService certificateService;

    /**
     * The Signature Service mock.
     */
    @MockBean
    SignatureService signatureService;

    // Test Variables
    private String entityName;
    private String entityMrn;
    private Integer mmsi;
    private McpEntityType mcpEntityType;
    private SignatureCertificate signatureCertificate;
    private SignatureVerificationRequestDto svr;

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        this.mmsi = 123456789;
        this.entityName = "test_aton";
        this.mcpEntityType = McpEntityType.SERVICE;
        this.entityMrn = "urn:mrn:mcp:" + this.mcpEntityType.getValue() + ":mcc:grad:instance:" + this.entityName;

        // Create a new signature certificate
        this.signatureCertificate = new SignatureCertificate();
        this.signatureCertificate.setCertificateId(BigInteger.ONE);
        this.signatureCertificate.setCertificate("Certificate");
        this.signatureCertificate.setPublicKey("PublicKey");
        this.signatureCertificate.setRootCertificate("rootCertificateThumbprint");

        // Create a new signature verification request
        this.svr = new SignatureVerificationRequestDto();
        this.svr.setContent(Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest("Hello World".getBytes())));
        this.svr.setSignature(Base64.getEncoder().encodeToString("That's the signature?".getBytes()));
    }

    /**
     * Test that we can correctly retrieve the signature certificate that will be
     * used for the signing requests of an MRN entity.
     */
    @Test
    void testGetSignatureCertificate() throws Exception {
        doReturn(this.signatureCertificate).when(this.signatureService).getSignatureCertificate(any(), any(), any());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(get("/api/signature/certificate?entityName={entityName}&mmsi={mmsi}&entityType={entityType}", this.entityName, this.mmsi, McpEntityType.SERVICE.getValue())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.svr.getContent()))
                .andExpect(status().isOk())
                .andReturn();

        // Parse and validate the response
        SignatureCertificateDto result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), SignatureCertificateDto.class);
        assertEquals(this.signatureCertificate.getCertificateId(), result.getCertificateId());
        assertEquals(this.signatureCertificate.getCertificate(), result.getCertificate());
        assertEquals(this.signatureCertificate.getPublicKey(), result.getPublicKey());
        assertEquals(this.signatureCertificate.getRootCertificate(), result.getRootCertificate());
    }

    /**
     * Test that we can generate a signature based on a specific certificate
     * assigned to an entity.
     */
    @Test
    void testGenerateCertificateSignature() throws Exception {
        doReturn(this.svr.getSignature().getBytes()).when(this.signatureService).generateEntitySignature(eq(this.signatureCertificate.getCertificateId()), any(), any());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(post("/api/signature/certificate/{certificateId}", this.signatureCertificate.getCertificateId())
                        .contentType(MediaType.TEXT_PLAIN_VALUE)
                        .content(this.svr.getContent()))
                .andExpect(status().isOk())
                .andReturn();

        // Assert the signature equality byte by byte
        byte[] signatureBytes = this.svr.getSignature().getBytes();
        for(int i=0; i<signatureBytes.length; i++) {
            assertEquals(signatureBytes[i], mvcResult.getResponse().getContentAsByteArray()[i]);
        }
    }

    /**
     * Test that we can generate a signature based on a specific certificate
     * assigned to an entity, and also provide the algorithm to be used for the
     * signature generation.
     */
    @Test
    void testGenerateCertificateSignatureWithAlgorithm() throws Exception {
        doReturn(this.svr.getSignature().getBytes()).when(this.signatureService).generateEntitySignature(eq(this.signatureCertificate.getCertificateId()), eq("DSA"), any());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(post("/api/signature/certificate/{certificateId}?algorithm={algorithm}", this.signatureCertificate.getCertificateId(), "DSA")
                        .contentType(MediaType.TEXT_PLAIN_VALUE)
                        .content(this.svr.getContent()))
                .andExpect(status().isOk())
                .andReturn();

        // Assert the signature equality byte by byte
        byte[] signatureBytes = this.svr.getSignature().getBytes();
        for(int i=0; i<signatureBytes.length; i++) {
            assertEquals(signatureBytes[i], mvcResult.getResponse().getContentAsByteArray()[i]);
        }
    }

    /**
     * Test that we can correctly generate a new signature for an MRN device
     * entity.
     */
    @Test
    void testGenerateEntitySignatureForDevice() throws Exception {
        doReturn(this.signatureCertificate).when(this.signatureService).getSignatureCertificate(any(), any(), any());
        doReturn(this.svr.getSignature().getBytes()).when(this.signatureService).generateEntitySignature(any(), any(), any());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(post("/api/signature/entity/generate/{entityName}?mmsi={mmsi}&entityType={entityType}", this.entityName, this.mmsi, McpEntityType.DEVICE.getValue())
                        .contentType(MediaType.TEXT_PLAIN_VALUE)
                        .content(this.svr.getContent()))
                .andExpect(status().isOk())
                .andReturn();

        // Assert the signature equality byte by byte
        byte[] signatureBytes = this.svr.getSignature().getBytes();
        for(int i=0; i<signatureBytes.length; i++) {
            assertEquals(signatureBytes[i], mvcResult.getResponse().getContentAsByteArray()[i]);
        }
    }

    /**
     * Test that we can correctly generate a new signature for an MRN service
     * entity.
     */
    @Test
    void testGenerateEntitySignatureForService() throws Exception {
        doReturn(this.signatureCertificate).when(this.signatureService).getSignatureCertificate(any(), any(), any());
        doReturn(this.svr.getSignature().getBytes()).when(this.signatureService).generateEntitySignature(any(), any(), any());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(post("/api/signature/entity/generate/{entityName}?mmsi={mmsi}&entityType={entityType}", this.entityName, this.mmsi, McpEntityType.SERVICE.getValue())
                        .contentType(MediaType.TEXT_PLAIN_VALUE)
                        .content(this.svr.getContent()))
                .andExpect(status().isOk())
                .andReturn();

        // Assert the signature equality byte by byte
        byte[] signatureBytes = this.svr.getSignature().getBytes();
        for(int i=0; i<signatureBytes.length; i++) {
            assertEquals(signatureBytes[i], mvcResult.getResponse().getContentAsByteArray()[i]);
        }
    }

    /**
     * Test that we can correctly generate a new signature for an MRN vessel
     * entity.
     */
    @Test
    void testGenerateEntitySignatureForVessel() throws Exception {
        doReturn(this.signatureCertificate).when(this.signatureService).getSignatureCertificate(any(), any(), any());
        doReturn(this.svr.getSignature().getBytes()).when(this.signatureService).generateEntitySignature(any(), any(), any());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(post("/api/signature/entity/generate/{entityName}?mmsi={mmsi}&entityType={entityType}", this.entityName, this.mmsi, McpEntityType.VESSEL.getValue())
                        .contentType(MediaType.TEXT_PLAIN_VALUE)
                        .content(this.svr.getContent()))
                .andExpect(status().isOk())
                .andReturn();

        // Assert the signature equality byte by byte
        byte[] signatureBytes = this.svr.getSignature().getBytes();
        for(int i=0; i<signatureBytes.length; i++) {
            assertEquals(signatureBytes[i], mvcResult.getResponse().getContentAsByteArray()[i]);
        }
    }

    /**
     * Test that we can correctly generate a new signature for an MRN user
     * entity.
     */
    @Test
    void testGenerateEntitySignatureForUser() throws Exception {
        doReturn(this.signatureCertificate).when(this.signatureService).getSignatureCertificate(any(), any(), any());
        doReturn(this.svr.getSignature().getBytes()).when(this.signatureService).generateEntitySignature(any(), any(), any());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(post("/api/signature/entity/generate/{entityName}?mmsi={mmsi}&entityType={entityType}", this.entityName, this.mmsi, McpEntityType.USER.getValue())
                        .contentType(MediaType.TEXT_PLAIN_VALUE)
                        .content(this.svr.getContent()))
                .andExpect(status().isOk())
                .andReturn();

        // Assert the signature equality byte by byte
        byte[] signatureBytes = this.svr.getSignature().getBytes();
        for(int i=0; i<signatureBytes.length; i++) {
            assertEquals(signatureBytes[i], mvcResult.getResponse().getContentAsByteArray()[i]);
        }
    }

    /**
     * Test that we can correctly generate a new signature for an MRN role
     * entity.
     */
    @Test
    void testGenerateEntitySignatureFoRole() throws Exception {
        doReturn(this.signatureCertificate).when(this.signatureService).getSignatureCertificate(any(), any(), any());
        doReturn(this.svr.getSignature().getBytes()).when(this.signatureService).generateEntitySignature(any(), any(), any());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(post("/api/signature/entity/generate/{entityName}?mmsi={mmsi}&entityType={entityType}", this.entityName, this.mmsi, McpEntityType.ROLE.getValue())
                        .contentType(MediaType.TEXT_PLAIN_VALUE)
                        .content(this.svr.getContent()))
                .andExpect(status().isOk())
                .andReturn();

        // Assert the signature equality byte by byte
        byte[] signatureBytes = this.svr.getSignature().getBytes();
        for(int i=0; i<signatureBytes.length; i++) {
            assertEquals(signatureBytes[i], mvcResult.getResponse().getContentAsByteArray()[i]);
        }
    }

    /**
     * Test that we can correctly generate a new signature for an MRN device
     * entity, with a specified signature algorithm.
     */
    @Test
    void testGenerateEntitySignatureWithAlgorithm() throws Exception {
        doReturn(this.signatureCertificate).when(this.signatureService).getSignatureCertificate(any(), any(), any());
        doReturn(this.svr.getSignature().getBytes()).when(this.signatureService).generateEntitySignature(any(), eq("someAlgorithm"), any());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(post("/api/signature/entity/generate/{entityName}?mmsi={mmsi}&entityType={entityType}&algorithm={algorithm}", this.entityName, this.mmsi, McpEntityType.DEVICE.getValue(), "someAlgorithm")
                        .contentType(MediaType.TEXT_PLAIN_VALUE)
                        .content(this.svr.getContent()))
                .andExpect(status().isOk())
                .andReturn();

        // Assert the signature equality byte by byte
        byte[] signatureBytes = this.svr.getSignature().getBytes();
        for(int i=0; i<signatureBytes.length; i++) {
            assertEquals(signatureBytes[i], mvcResult.getResponse().getContentAsByteArray()[i]);
        }
    }

    /**
     * Test that we can correctly verify that some content matches the provided
     * signature, for a given entity MRN.
     */
    @Test
    void testVerifyEntitySignatureByMrn() throws Exception {
        doReturn(Boolean.TRUE).when(this.signatureService).verifyEntitySignatureByMrn(any(), any(), any(), any());

        // Perform the MVC request
        this.mockMvc.perform(post("/api/signature/entity/verify/{entityMrn}", this.entityMrn)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.svr)))
                .andExpect(status().isOk())
                .andReturn();
    }

    /**
     * Test that if we cannot correctly verify that some content matches the
     * provided signature, an HTTP BAD REQUEST will be returned.
     */
    @Test
    void testVerifyEntitySignatureByMrnFail() throws Exception {
        doReturn(Boolean.FALSE).when(this.signatureService).verifyEntitySignatureByMrn(any(), any(), any(), any());

        // Perform the MVC request
        this.mockMvc.perform(post("/api/signature/entity/verify/{entityMrn}", this.entityMrn)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.svr)))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    /**
     * Test that we can correctly verify that some content matches the provided
     * signature, for a given MMSI.
     */
    @Test
    void testVerifyEntitySignatureByMmsi() throws Exception {
        doReturn(Boolean.TRUE).when(this.signatureService).verifyEntitySignatureByMmsi(any(), any(), any(), any());

        // Perform the MVC request
        this.mockMvc.perform(post("/api/signature/mmsi/verify/{mmsi}", this.mmsi)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.svr)))
                .andExpect(status().isOk())
                .andReturn();
    }

    /**
     * Test that if we cannot correctly verify that some content matches the
     * provided signature, an HTTP BAD REQUEST will be returned.
     */
    @Test
    void testVerifyEntitySignatureByMmsiFail() throws Exception {
        doReturn(Boolean.FALSE).when(this.signatureService).verifyEntitySignatureByMmsi(any(), any(), any(), any());

        // Perform the MVC request
        this.mockMvc.perform(post("/api/signature/mmsi/verify/{mmsi}", this.mmsi)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.svr)))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

}