package org.grad.eNav.cKeeper.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.core.util.Base64;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
    private String atonUID;
    private Integer mmsi;
    private SignatureVerificationRequestDto svr;

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        this.atonUID = "test_aton";
        this.mmsi = 123456789;
        this.svr = new SignatureVerificationRequestDto();
        this.svr.setContent(Base64.encode(MessageDigest.getInstance("SHA-256").digest("Hello World".getBytes())).toString());
        this.svr.setSignature(Base64.encode("That's the signature?".getBytes()).toString());
    }

    /**
     * Test that we can correctly delete an existing MCP device by using a valid
     * MRN.
     */
    @Test
    void testGenerateAtoNSignature() throws Exception {
        doReturn(this.svr.getSignature().getBytes()).when(this.signatureService).generateAtonSignature(any(), any(), any());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(post("/api/signatures/atons/generate?atonUID={atonUID}&mmsi={mmsi}", this.atonUID, this.mmsi)
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
     * signature, for a given MMSI.
     */
    @Test
    void testVerifyAtoNSignature() throws Exception {
        doReturn(Boolean.TRUE).when(this.signatureService).verifyMmsiSignature(any(), any(), any());

        // Perform the MVC request
        this.mockMvc.perform(post("/api/signatures/mmsi/verify/{mmsi}", this.mmsi)
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
    void testVerifyAtoNSignatureFail() throws Exception {
        doReturn(Boolean.FALSE).when(this.signatureService).verifyMmsiSignature(any(), any(), any());

        // Perform the MVC request
        this.mockMvc.perform(post("/api/signatures/mmsi/verify/{mmsi}", this.mmsi)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.svr)))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

}