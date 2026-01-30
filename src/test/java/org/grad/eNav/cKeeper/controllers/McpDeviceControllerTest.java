/*
 * Copyright (c) 2024 GLA Research and Development Directorate
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
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
import org.grad.eNav.cKeeper.exceptions.McpConnectivityException;
import org.grad.eNav.cKeeper.exceptions.SavingFailedException;
import org.grad.eNav.cKeeper.models.domain.mcp.McpEntityType;
import org.grad.eNav.cKeeper.models.dtos.mcp.McpDeviceDto;
import org.grad.eNav.cKeeper.models.dtos.mcp.McpServiceDto;
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

import java.io.IOException;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@WebMvcTest(controllers = McpDeviceController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
class McpDeviceControllerTest {

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
    private McpDeviceDto mcpDeviceDto;

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setUp() {
        this.mcpDeviceDto = new McpDeviceDto("Test","urn:mrn:mcp:device:mcc:grad:test");
    }

    /**
     * Test that we can correctly retrieve a single MCP device based on the
     * provided entry MRN.
     */
    @Test
    void testGetMcpDevice() throws Exception {
        doReturn(this.mcpDeviceDto).when(this.mcpService).getMcpEntity(this.mcpDeviceDto.getMrn(), McpDeviceDto.class);

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(get("/api/mcp/{mcpEntityType}/{mcp}", McpEntityType.DEVICE.getValue(), this.mcpDeviceDto.getMrn()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        // Parse and validate the response
        McpDeviceDto result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), McpDeviceDto.class);
        assertEquals(this.mcpDeviceDto, result);
    }

    /**
     * Test that if the service receives a failure when requesting the MCP MIR
     * retrieve an MCP device, an HTTP BAD_REQUEST response will be returned.
     */
    @Test
    void testGetMcpDeviceFailed() throws Exception {
        doThrow(DataNotFoundException.class).when(this.mcpService).getMcpEntity(this.mcpDeviceDto.getMrn(), McpDeviceDto.class);

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(get("/api/mcp/{mcpEntityType}/{mcp}", McpEntityType.DEVICE.getValue(), this.mcpDeviceDto.getMrn()))
                .andExpect(status().isNotFound())
                .andReturn();
    }

    /**
     * Test that if the service fails to connect to the MCP MIR to retrieve an
     * MCP device, an HTTP BAD_REQUEST response  will be returned.
     */
    @Test
    void testGetMcpDeviceMcpConnectivityFailed() throws Exception {
        doThrow(McpConnectivityException.class).when(this.mcpService).getMcpEntity(this.mcpDeviceDto.getMrn(), McpDeviceDto.class);

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(get("/api/mcp/{mcpEntityType}/{mcp}", McpEntityType.DEVICE.getValue(), this.mcpDeviceDto.getMrn()))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    /**
     * Test that we can create a new MCP device correctly through a POST
     * request. The provided] MCP device should NOT have an ID, while the
     * returned value will have the ID field populated.
     */
    @Test
    void testCreateMcpDevice() throws Exception {
        // Mock the service call for creating a new instance
        doReturn(this.mcpDeviceDto).when(this.mcpService).createMcpEntity(this.mcpDeviceDto);

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(post("/api/mcp/{mcpEntityType}", McpEntityType.DEVICE.getValue())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.mcpDeviceDto)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        // Parse and validate the response
        McpDeviceDto result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), McpDeviceDto.class);
        assertEquals(this.mcpDeviceDto, result);
    }

    /**
     * Test that if we try to create an MCP device with an existing ID field,
     * an HTTP BAD_REQUEST response will be returns, with a description of
     * the error in the header.
     */
    @Test
    void testCreateMcpDeviceWithId() throws Exception {
        // To invalidate set an ID to the MCP device
        this.mcpDeviceDto.setId(BigInteger.ONE);

        // Perform the MVC request
        this.mockMvc.perform(post("/api/mcp/{mcpEntityType}", McpEntityType.DEVICE.getValue())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.mcpDeviceDto)))
                .andExpect(status().isBadRequest())
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
        this.mcpDeviceDto.setMrn(null);

        // Perform the MVC request
        this.mockMvc.perform(post("/api/mcp/{mcpEntityType}", McpEntityType.DEVICE.getValue())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.mcpDeviceDto)))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    /**
     * Test that if the service receives a failure when requesting the MCP MIR
     * create an new device, an HTTP BAD_REQUEST response will be returned.
     */
    @Test
    void testCreateMcpDeviceFailed() throws Exception {
        doThrow(SavingFailedException.class).when(this.mcpService).createMcpEntity(this.mcpDeviceDto);

        // Perform the MVC request
        this.mockMvc.perform(post("/api/mcp/{mcpEntityType}", McpEntityType.DEVICE.getValue())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.mcpDeviceDto)))
                .andExpect(status().isUnprocessableEntity())
                .andReturn();
    }

    /**
     * Test that if the service fails to connect to the MCP MIR to create an
     * MCP device, an HTTP BAD_REQUEST response  will be returned.
     */
    @Test
    void testCreateMcpDeviceMcpConnectivityFailed() throws Exception {
        doThrow(McpConnectivityException.class).when(this.mcpService).createMcpEntity(this.mcpDeviceDto);

        // Perform the MVC request
        this.mockMvc.perform(post("/api/mcp/{mcpEntityType}", McpEntityType.DEVICE.getValue())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.mcpDeviceDto)))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    /**
     * Test that we can update an existing MCP device correctly through a PUT
     * request. The incoming MRN entity should always have an ID.
     */
    @Test
    void testUpdateMcpDevice() throws Exception {
        // For an update set an ID in the MCP device
        this.mcpDeviceDto.setId(BigInteger.ONE);

        // Mock the service call for updating an existing instance
        doReturn(this.mcpDeviceDto).when(this.mcpService).updateMcpEntity(this.mcpDeviceDto.getMrn(), this.mcpDeviceDto);

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(put("/api/mcp/{mcpEntityType}/{mrn}", McpEntityType.DEVICE.getValue(), this.mcpDeviceDto.getMrn())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.mcpDeviceDto)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        // Parse and validate the response
        McpDeviceDto result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), McpDeviceDto.class);
        assertEquals(this.mcpDeviceDto, result);
    }

    /**
     * Test that if we try to create an MCP device without an existing ID field,
     * an HTTP BAD_REQUEST response will be returns, with a description of
     * the error in the header.
     */
    @Test
    void testUpdateMcpDeviceWithoutId() throws Exception {
        // To invalidate don't an ID to the MCP device
        this.mcpDeviceDto.setId(null);

        // Perform the MVC request
        this.mockMvc.perform(put("/api/mcp/{mcpEntityType}/{mrn}", McpEntityType.DEVICE.getValue(), this.mcpDeviceDto.getMrn())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.mcpDeviceDto)))
                .andExpect(status().isBadRequest())
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
        this.mcpDeviceDto.setId(BigInteger.ONE);
        // To invalidate don't set an MRN to the MCP device
        this.mcpDeviceDto.setMrn(null);

        // Perform the MVC request
        this.mockMvc.perform(put("/api/mcp/{mcpEntityType}/{mrn}", McpEntityType.DEVICE.getValue(), "urn:mrn:mcp:device:mcc:grad:test")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.mcpDeviceDto)))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    /**
     * Test that if we fail to update the provided MCP device due to a general
     * error, an HTTP BAD_REQUEST response will be returned, with a description
     * of the error in the header.
     */
    @Test
    void testUpdateMcpDeviceFailed() throws Exception {
        // For an update set an ID in the MCP device
        this.mcpDeviceDto.setId(BigInteger.ONE);

        // Mock a general Exception when saving the instance
        doThrow(SavingFailedException.class).when(this.mcpService).updateMcpEntity(any(), any());

        // Perform the MVC request
        this.mockMvc.perform(put("/api/mcp/{mcpEntityType}/{mrn}", McpEntityType.DEVICE.getValue(), this.mcpDeviceDto.getMrn())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.mcpDeviceDto)))
                .andExpect(status().isUnprocessableEntity())
                .andReturn();
    }

    /**
     * Test that if the service fails to connect to the MCP MIR to update an
     * MRN entity, an HTTP BAD_REQUEST response  will be returned.
     */
    @Test
    void testUpdateMcpDeviceMcpConnectivityFailure() throws Exception {
        // For an update set an ID in the MCP device
        this.mcpDeviceDto.setId(BigInteger.ONE);

        // Mock a general Exception when saving the instance
        doThrow(McpConnectivityException.class).when(this.mcpService).updateMcpEntity(any(), any());

        // Perform the MVC request
        this.mockMvc.perform(put("/api/mcp/{mcpEntityType}/{mrn}", McpEntityType.DEVICE.getValue(), this.mcpDeviceDto.getMrn())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.mcpDeviceDto)))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    /**
     * Test that we can correctly delete an existing MCP device by using a valid
     * MRN.
     */
    @Test
    void testDeleteMcpDevice() throws Exception {
        doReturn(Boolean.TRUE).when(this.mcpService).deleteMcpEntity(this.mcpDeviceDto.getMrn(), McpDeviceDto.class);

        // Perform the MVC request
        this.mockMvc.perform(delete("/api/mcp/{mcpEntityType}/{mrn}", McpEntityType.DEVICE.getValue(), this.mcpDeviceDto.getMrn())
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
        doThrow(DataNotFoundException.class).when(this.mcpService).deleteMcpEntity(any(), any());

        // Perform the MVC request
        this.mockMvc.perform(delete("/api/mcp/{mcpEntityType}/{mrn}", McpEntityType.DEVICE.getValue(), this.mcpDeviceDto.getMrn()))
                .andExpect(status().isNotFound());
    }

    /**
     * Test that if the service receives a failure when requesting the MCP MIR
     * delete an MCP device, an HTTP BAD_REQUEST response will be returned.
     */
    @Test
    void testDeleteMcpDeviceFailed() throws Exception {
        doThrow(DataNotFoundException.class).when(this.mcpService).deleteMcpEntity(any(), any());

        // Perform the MVC request
        this.mockMvc.perform(delete("/api/mcp/{mcpEntityType}/{mrn}", McpEntityType.DEVICE.getValue(), this.mcpDeviceDto.getMrn())
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isNotFound())
                .andReturn();
    }

    /**
     * Test that if the service fails to connect to the MCP MIR to delete an
     * MCP device, an HTTP BAD_REQUEST response  will be returned.
     */
    @Test
    void testDeleteMcpDeviceMcpConnectivityFailed() throws Exception {
        doThrow(McpConnectivityException.class).when(this.mcpService).deleteMcpEntity(any(), any());

        // Perform the MVC request
        this.mockMvc.perform(delete("/api/mcp/{mcpEntityType}/{mrn}", McpEntityType.DEVICE.getValue(), this.mcpDeviceDto.getMrn())
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

}