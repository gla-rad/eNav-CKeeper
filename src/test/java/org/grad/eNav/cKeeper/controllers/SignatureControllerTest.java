package org.grad.eNav.cKeeper.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private SignatureVerificationRequestDto svr;

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        this.atonUID = "test_aton";
        this.svr = new SignatureVerificationRequestDto();
        this.svr.setContent(MessageDigest.getInstance("SHA-256").digest(("Hello World").getBytes()));
        this.svr.setSignature(MessageDigest.getInstance("SHA-256").digest(("That's the signature?").getBytes()));
    }

    /**
     * Test that we can correctly delete an existing MCP device by using a valid
     * MRN.
     */
    @Test
    void testGenerateAtoNSignature() throws Exception {
        doReturn(this.svr.getSignature()).when(this.signatureService).generateAtonSignature(any(), any());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(post("/api/signatures/atons/generate?atonUID={atonUID}", this.atonUID)
                        .contentType(MediaType.TEXT_PLAIN_VALUE)
                        .content(this.svr.getContent()))
                .andExpect(status().isOk())
                .andReturn();

        // Assert the signature equality byte by byte
        for(int i=0; i<this.svr.getContent().length; i++) {
            assertEquals(this.svr.getSignature()[i], mvcResult.getResponse().getContentAsByteArray()[i]);
        }
    }

    /**
     * Test that we can correctly verify that some content matches the provided
     * signature, for a given AtoN.
     */
    @Test
    void testVerifyAtoNSignature() throws Exception {
        doReturn(Boolean.TRUE).when(this.signatureService).verifyAtonSignature(any(), any(), any());

        // Perform the MVC request
        this.mockMvc.perform(post("/api/signatures/atons/verify?atonUID={atonUID}", this.atonUID)
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
        doReturn(Boolean.FALSE).when(this.signatureService).verifyAtonSignature(any(), any(), any());

        // Perform the MVC request
        this.mockMvc.perform(post("/api/signatures/atons/verify?atonUID={atonUID}", this.atonUID)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.svr)))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

}