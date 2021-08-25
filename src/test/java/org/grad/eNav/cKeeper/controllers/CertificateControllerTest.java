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

package org.grad.eNav.cKeeper.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.grad.eNav.cKeeper.models.dtos.CertificateDto;
import org.grad.eNav.cKeeper.services.CertificateService;
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

import java.io.IOException;
import java.math.BigInteger;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(controllers = CertificateController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
class CertificateControllerTest {

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

    // Test Variables
    private CertificateDto certificateDto;

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setUp() {
        this.certificateDto = new CertificateDto();
        this.certificateDto.setId(BigInteger.ONE);
        this.certificateDto.setMrnEntityId(BigInteger.TEN);
        this.certificateDto.setPublicKey("PUBLIC KEY");
        this.certificateDto.setStartDate(new Date());
        this.certificateDto.setEndDate(new Date());
        this.certificateDto.setRevoked(Boolean.FALSE);
    }

    /**
     * Test that we can correctly revoke an existing certificate based on the
     * provided local certificate ID.
     */
    @Test
    void testRevokeCertificate() throws Exception {
        doReturn(this.certificateDto).when(this.certificateService).revoke(this.certificateDto.getId());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(put("/api/certificates/{id}/revoke", this.certificateDto.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        // Parse and validate the response
        CertificateDto result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), CertificateDto.class);
        assertEquals(this.certificateDto, result);
    }

    /**
     * Test that if the service fails to connect to the MCP MIR to revoke
     * the certificate, an HTTP BAD_REQUEST response  will be returned.
     */
    @Test
    void testRevokeCertificateFailed() throws Exception {
        doThrow(IOException.class).when(this.certificateService).revoke(this.certificateDto.getId());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(put("/api/certificates/{id}/revoke", this.certificateDto.getId()))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    /**
     * Test that we can correctly delete an existing certificate based on the
     * provided local certificate ID.
     */
    @Test
    void testDeleteCertificate() throws Exception {
        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(delete("/api/certificates/{id}", this.certificateDto.getId()))
                .andExpect(status().isOk())
                .andReturn();
    }

}