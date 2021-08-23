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
import org.grad.eNav.cKeeper.exceptions.DataNotFoundException;
import org.grad.eNav.cKeeper.models.dtos.McpDeviceDto;
import org.grad.eNav.cKeeper.services.McpService;
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

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@WebMvcTest(controllers = McpController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
class McpControllerTest {

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
     * The MCP Service mock.
     */
    @MockBean
    McpService mcpService;

    // Test Variables
    private McpDeviceDto mcpDevice;

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setUp() {
        this.mcpDevice = new McpDeviceDto("Test","urn:mrn:mcp:device:mcc:grad:test");
    }

    /**
     * Test that we can correctly retrieve a single MCP device based on the
     * provided entry MRN.
     */
    @Test
    void testGetMrnEntity() throws Exception {
        doReturn(this.mcpDevice).when(this.mcpService).getMcpDevice(this.mcpDevice.getMrn());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(get("/api/mcp/devices/{mcp}", this.mcpDevice.getMrn()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        // Parse and validate the response
        McpDeviceDto result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), McpDeviceDto.class);
        assertEquals(this.mcpDevice, result);
    }

    /**
     * Test that we can create a new MCN device correctly through a POST request.
     * The incoming MRN entity should NOT have an ID, while the returned
     * value will have the ID field populated.
     */
    @Test
    void testCreateMrnEntity() throws Exception {
        // Mock the service call for creating a new instance
        doReturn(this.mcpDevice).when(this.mcpService).createMcpDevice(this.mcpDevice);

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(post("/api/mcp/devices")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.mcpDevice)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        // Parse and validate the response
        McpDeviceDto result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), McpDeviceDto.class);
        assertEquals(this.mcpDevice, result);
    }

    /**
     * Test that if we try to create an MCP device with an existing ID field,
     * an HTTP BAD_REQUEST response will be returns, with a description of
     * the error in the header.
     */
    @Test
    void testCreateMcpDeviceWithId() throws Exception {
        // To invalidate set an ID to the MCP device
        this.mcpDevice.setId(BigInteger.ONE);

        // Perform the MVC request
        this.mockMvc.perform(post("/api/mcp/devices")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.mcpDevice)))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-cKeeper-error"))
                .andExpect(header().exists("X-cKeeper-params"))
                .andReturn();
    }

    /**
     * Test that if we try to create an MCP device without a valid MRN,
     * an HTTP BAD_REQUEST response will be returns, with a description of
     * the error in the header.
     */
    @Test
    void testCreateMcpDeviceWithoutMRN() throws Exception {
        // To invalidate set an ID to the MCP device
        this.mcpDevice.setMrn(null);

        // Perform the MVC request
        this.mockMvc.perform(post("/api/mcp/devices")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.mcpDevice)))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-cKeeper-error"))
                .andExpect(header().exists("X-cKeeper-params"))
                .andReturn();
    }

    /**
     * Test that we can update an existing MRN entity correctly through a PUT
     * request. The incoming MRN entity should always have an ID.
     */
    @Test
    void testUpdateMcpDevice() throws Exception {
        // For an update set an ID in the MCP device
        this.mcpDevice.setId(BigInteger.ONE);

        // Mock the service call for updating an existing instance
        doReturn(this.mcpDevice).when(this.mcpService).updateMcpDevice(this.mcpDevice.getMrn(), this.mcpDevice);

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(put("/api/mcp/devices/{mrn}", this.mcpDevice.getMrn())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.mcpDevice)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        // Parse and validate the response
        McpDeviceDto result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), McpDeviceDto.class);
        assertEquals(this.mcpDevice, result);
    }

    /**
     * Test that if we try to create an MCP device without an existing ID field,
     * an HTTP BAD_REQUEST response will be returns, with a description of
     * the error in the header.
     */
    @Test
    void testUpdateMcpDeviceWithoutId() throws Exception {
        // To invalidate don't an ID to the MCP device
        this.mcpDevice.setId(null);

        // Perform the MVC request
        this.mockMvc.perform(put("/api/mcp/devices/{mrn}", this.mcpDevice.getMrn())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.mcpDevice)))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-cKeeper-error"))
                .andExpect(header().exists("X-cKeeper-params"))
                .andReturn();
    }

    /**
     * Test that if we try to update an MCP device without a valid MRN,
     * an HTTP BAD_REQUEST response will be returns, with a description of
     * the error in the header.
     */
    @Test
    void testUpdateMcpDeviceWithoutMRN() throws Exception {
        // For an update set an ID in the MCP device
        this.mcpDevice.setId(BigInteger.ONE);
        // To invalidate don't set an MRN to the MCP device
        this.mcpDevice.setMrn(null);

        // Perform the MVC request
        this.mockMvc.perform(put("/api/mcp/devices/{mrn}", "urn:mrn:mcp:device:mcc:grad:test")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.mcpDevice)))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-cKeeper-error"))
                .andExpect(header().exists("X-cKeeper-params"))
                .andReturn();
    }

    /**
     * Test that if we fail to update the provided MCP device due to a general
     * error, an HTTP BAD_REQUEST response will be returned, with a description
     * of the error in the header.
     */
    @Test
    void testUpdateMcpDeviceFailure() throws Exception {
        // For an update set an ID in the MCP device
        this.mcpDevice.setId(BigInteger.ONE);

        // Mock a general Exception when saving the instance
        doThrow(RuntimeException.class).when(this.mcpService).updateMcpDevice(any(), any());

        // Perform the MVC request
        this.mockMvc.perform(put("/api/mcp/devices/{mrn}", this.mcpDevice.getMrn())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.mcpDevice)))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-cKeeper-error"))
                .andExpect(header().exists("X-cKeeper-params"))
                .andReturn();
    }

    /**
     * Test that we can correctly delete an existing MCP device by using a valid
     * MRN.
     */
    @Test
    void testDeleteMcpDevice() throws Exception {
        doReturn(Boolean.TRUE).when(this.mcpService).deleteMcpDevice(any());

        // Perform the MVC request
        this.mockMvc.perform(delete("/api/mcp/devices/{mrn}", this.mcpDevice.getMrn())
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
    }

    /**
     * Test that if we do NOT find the MCP device we are trying to delete, an
     * HTTP NOT_FOUND response will be returned.
     */
    @Test
    void testDeleteMcpDeviceNotFound() throws Exception {
        doThrow(DataNotFoundException.class).when(this.mcpService).deleteMcpDevice(any());

        // Perform the MVC request
        this.mockMvc.perform(delete("/api/mcp/devices/{mrn}", this.mcpDevice.getMrn()))
                .andExpect(status().isNotFound());
    }

}