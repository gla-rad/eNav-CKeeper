package org.grad.eNav.cKeeper.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.grad.eNav.cKeeper.models.domain.Pair;
import org.grad.eNav.cKeeper.models.domain.mcp.McpEntityType;
import org.grad.eNav.cKeeper.models.dtos.SignatureVerificationRequestDto;
import org.grad.eNav.cKeeper.services.SignatureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(controllers = SignatureController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
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
     * The Signature Service mock.
     */
    @MockBean
    SignatureService signatureService;

    // Test Variables
    private String entityId;
    private Integer mmsi;
    private McpEntityType mcpEntityType;
    private SignatureVerificationRequestDto svr;

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        this.mmsi = 123456789;
        this.entityId = "test_aton";
        this.svr = new SignatureVerificationRequestDto();
        this.svr.setContent(Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest("Hello World".getBytes())));
        this.svr.setSignature(Base64.getEncoder().encodeToString("That's the signature?".getBytes()));
    }

    /**
     * Test that we can correctly generate a new signature for an MRN device
     * entity.
     */
    @Test
    void testGenerateEntitySignatureForDevice() throws Exception {
        doReturn(new Pair<>("Certificate", this.svr.getSignature().getBytes())).when(this.signatureService).generateEntitySignature(any(), any(), any(), any());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(post("/api/signature/entity/generate/{entityId}?mmsi={mmsi}&entityType={entityType}", this.entityId, this.mmsi, McpEntityType.DEVICE.getValue())
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
        doReturn(new Pair<>("Certificate", this.svr.getSignature().getBytes())).when(this.signatureService).generateEntitySignature(any(), any(), any(), any());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(post("/api/signature/entity/generate/{entityId}?mmsi={mmsi}&entityType={entityType}", this.entityId, this.mmsi, McpEntityType.SERVICE.getValue())
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
        doReturn(new Pair<>("Certificate", this.svr.getSignature().getBytes())).when(this.signatureService).generateEntitySignature(any(), any(), any(), any());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(post("/api/signature/entity/generate/{entityId}?mmsi={mmsi}&entityType={entityType}", this.entityId, this.mmsi, McpEntityType.VESSEL.getValue())
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
        doReturn(new Pair<>("Certificate", this.svr.getSignature().getBytes())).when(this.signatureService).generateEntitySignature(any(), any(), any(), any());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(post("/api/signature/entity/generate/{entityId}?mmsi={mmsi}&entityType={entityType}", this.entityId, this.mmsi, McpEntityType.USER.getValue())
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
        doReturn(new Pair<>("Certificate", this.svr.getSignature().getBytes())).when(this.signatureService).generateEntitySignature(any(), any(), any(), any());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(post("/api/signature/entity/generate/{entityId}?mmsi={mmsi}&entityType={entityType}", this.entityId, this.mmsi, McpEntityType.ROLE.getValue())
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
     * signature, for a given entity ID.
     */
    @Test
    void testVerifyEntitySignature() throws Exception {
        doReturn(Boolean.TRUE).when(this.signatureService).verifyEntitySignature(any(), any(), any());

        // Perform the MVC request
        this.mockMvc.perform(post("/api/signature/entity/verify/{entityId}", this.entityId)
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
    void testVerifyEntitySignatureFail() throws Exception {
        doReturn(Boolean.FALSE).when(this.signatureService).verifyEntitySignature(any(), any(), any());

        // Perform the MVC request
        this.mockMvc.perform(post("/api/signature/entity/verify/{entityId}", this.entityId)
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
        doReturn(Boolean.TRUE).when(this.signatureService).verifyEntitySignatureByMmsi(any(), any(), any());

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
        doReturn(Boolean.FALSE).when(this.signatureService).verifyEntitySignatureByMmsi(any(), any(), any());

        // Perform the MVC request
        this.mockMvc.perform(post("/api/signature/mmsi/verify/{mmsi}", this.mmsi)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.svr)))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

}