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
import org.grad.eNav.cKeeper.models.dtos.mcp.McpServiceDto;
import org.grad.eNav.cKeeper.services.McpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(controllers = McpServiceController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
class McpServiceControllerTest {

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
    @MockitoBean
    McpService mcpService;

    // Test Variables
    private McpServiceDto mcpServiceDto;

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setUp() {
        this.mcpServiceDto = new McpServiceDto("Test","urn:mrn:mcp:service:mcc:grad:test");

    }

    /**
     * Test that we can correctly retrieve a single MCP service based on the
     * provided entry MRN.
     */
    @Test
    void testGetMcpService() throws Exception {
        doReturn(this.mcpServiceDto).when(this.mcpService).getMcpEntity(this.mcpServiceDto.getMrn(), McpServiceDto.class);

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(get("/api/mcp/{mcpEntityType}/{mcp}", McpEntityType.SERVICE.getValue(), this.mcpServiceDto.getMrn()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        // Parse and validate the response
        McpServiceDto result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), McpServiceDto.class);
        assertEquals(this.mcpServiceDto, result);
    }

    /**
     * Test that if the service receives a failure when requesting the MCP MIR
     * retrieve an MCP service, an HTTP BAD_REQUEST response will be returned.
     */
    @Test
    void testGetMcpServiceFailed() throws Exception {
        doThrow(DataNotFoundException.class).when(this.mcpService).getMcpEntity(this.mcpServiceDto.getMrn(), McpServiceDto.class);

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(get("/api/mcp/{mcpEntityType}/{mcp}", McpEntityType.SERVICE.getValue(), this.mcpServiceDto.getMrn()))
                .andExpect(status().isNotFound())
                .andReturn();
    }

    /**
     * Test that if the service fails to connect to the MCP MIR to retrieve an
     * MCP service, an HTTP BAD_REQUEST response  will be returned.
     */
    @Test
    void testGetMcpServiceMcpConnectivityFailed() throws Exception {
        doThrow(McpConnectivityException.class).when(this.mcpService).getMcpEntity(this.mcpServiceDto.getMrn(), McpServiceDto.class);

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(get("/api/mcp/{mcpEntityType}/{mcp}/", McpEntityType.SERVICE.getValue(), this.mcpServiceDto.getMrn()))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    /**
     * Test that we can create a new MCP service correctly through a POST
     * request. The provided] MCP service should NOT have an ID, while the
     * returned value will have the ID field populated.
     */
    @Test
    void testCreateMcpService() throws Exception {
        // Mock the service call for creating a new instance
        doReturn(this.mcpServiceDto).when(this.mcpService).createMcpEntity(this.mcpServiceDto);

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(post("/api/mcp/{mcpEntityType}", McpEntityType.SERVICE.getValue())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.mcpServiceDto)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        // Parse and validate the response
        McpServiceDto result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), McpServiceDto.class);
        assertEquals(this.mcpServiceDto, result);
    }

    /**
     * Test that if we try to create an MCP service with an existing ID field,
     * an HTTP BAD_REQUEST response will be returns, with a description of
     * the error in the header.
     */
    @Test
    void testCreateMcpServiceWithId() throws Exception {
        // To invalidate set an ID to the MCP service
        this.mcpServiceDto.setId(BigInteger.ONE);

        // Perform the MVC request
        this.mockMvc.perform(post("/api/mcp/{mcpEntityType}", McpEntityType.SERVICE.getValue())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.mcpServiceDto)))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    /**
     * Test that if we try to create an MCP service without a valid MRN,
     * an HTTP BAD_REQUEST response will be returns, with a description of
     * the error in the header.
     */
    @Test
    void testCreateMcpServiceWithoutMRN() throws Exception {
        // To invalidate set an ID to the MCP service
        this.mcpServiceDto.setMrn(null);

        // Perform the MVC request
        this.mockMvc.perform(post("/api/mcp/{mcpEntityType}", McpEntityType.SERVICE.getValue())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.mcpServiceDto)))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    /**
     * Test that if the service receives a failure when requesting the MCP MIR
     * create an new service, an HTTP BAD_REQUEST response will be returned.
     */
    @Test
    void testCreateMcpServiceFailed() throws Exception {
        doThrow(SavingFailedException.class).when(this.mcpService).createMcpEntity(this.mcpServiceDto);

        // Perform the MVC request
        this.mockMvc.perform(post("/api/mcp/{mcpEntityType}", McpEntityType.SERVICE.getValue())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.mcpServiceDto)))
                .andExpect(status().isUnprocessableEntity())
                .andReturn();
    }

    /**
     * Test that if the service fails to connect to the MCP MIR to create an
     * MCP service, an HTTP BAD_REQUEST response  will be returned.
     */
    @Test
    void testCreateMcpServiceMcpConnectivityFailed() throws Exception {
        doThrow(McpConnectivityException.class).when(this.mcpService).createMcpEntity(this.mcpServiceDto);

        // Perform the MVC request
        this.mockMvc.perform(post("/api/mcp/{mcpEntityType}", McpEntityType.SERVICE.getValue())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.mcpServiceDto)))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    /**
     * Test that we can update an existing MCP service correctly through a PUT
     * request. The incoming MRN entity should always have an ID.
     */
    @Test
    void testUpdateMcpService() throws Exception {
        // For an update set an ID in the MCP service
        this.mcpServiceDto.setId(BigInteger.ONE);

        // Mock the service call for updating an existing instance
        doReturn(this.mcpServiceDto).when(this.mcpService).updateMcpEntity(this.mcpServiceDto.getMrn(), this.mcpServiceDto);

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(put("/api/mcp/{mcpEntityType}/{mrn}", McpEntityType.SERVICE.getValue(), this.mcpServiceDto.getMrn())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.mcpServiceDto)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        // Parse and validate the response
        McpServiceDto result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), McpServiceDto.class);
        assertEquals(this.mcpServiceDto, result);
    }

    /**
     * Test that if we try to create an MCP service without an existing ID field,
     * an HTTP BAD_REQUEST response will be returns, with a description of
     * the error in the header.
     */
    @Test
    void testUpdateMcpServiceWithoutId() throws Exception {
        // To invalidate don't an ID to the MCP service
        this.mcpServiceDto.setId(null);

        // Perform the MVC request
        this.mockMvc.perform(put("/api/mcp/{mcpEntityType}/{mrn}", McpEntityType.SERVICE.getValue(), this.mcpServiceDto.getMrn())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.mcpServiceDto)))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    /**
     * Test that if we try to update an MCP service without a valid MRN,
     * an HTTP BAD_REQUEST response will be returns, with a description of
     * the error in the header.
     */
    @Test
    void testUpdateMcpServiceWithoutMRN() throws Exception {
        // For an update set an ID in the MCP service
        this.mcpServiceDto.setId(BigInteger.ONE);
        // To invalidate don't set an MRN to the MCP service
        this.mcpServiceDto.setMrn(null);

        // Perform the MVC request
        this.mockMvc.perform(put("/api/mcp/{mcpEntityType}/{mrn}", McpEntityType.SERVICE.getValue(), "urn:mrn:mcp:service:mcc:grad:test")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.mcpServiceDto)))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    /**
     * Test that if we fail to update the provided MCP service due to a general
     * error, an HTTP BAD_REQUEST response will be returned, with a description
     * of the error in the header.
     */
    @Test
    void testUpdateMcpServiceFailed() throws Exception {
        // For an update set an ID in the MCP service
        this.mcpServiceDto.setId(BigInteger.ONE);

        // Mock a general Exception when saving the instance
        doThrow(SavingFailedException.class).when(this.mcpService).updateMcpEntity(any(), any());

        // Perform the MVC request
        this.mockMvc.perform(put("/api/mcp/{mcpEntityType}/{mrn}", McpEntityType.SERVICE.getValue(), this.mcpServiceDto.getMrn())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.mcpServiceDto)))
                .andExpect(status().isUnprocessableEntity())
                .andReturn();
    }

    /**
     * Test that if the service fails to connect to the MCP MIR to update an
     * MRN entity, an HTTP BAD_REQUEST response  will be returned.
     */
    @Test
    void testUpdateMcpServiceMcpConnectivityFailure() throws Exception {
        // For an update set an ID in the MCP service
        this.mcpServiceDto.setId(BigInteger.ONE);

        // Mock a general Exception when saving the instance
        doThrow(McpConnectivityException.class).when(this.mcpService).updateMcpEntity(any(), any());

        // Perform the MVC request
        this.mockMvc.perform(put("/api/mcp/{mcpEntityType}/{mrn}", McpEntityType.SERVICE.getValue(), this.mcpServiceDto.getMrn())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.mcpServiceDto)))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    /**
     * Test that we can correctly delete an existing MCP service by using a valid
     * MRN.
     */
    @Test
    void testDeleteMcpService() throws Exception {
        doReturn(Boolean.TRUE).when(this.mcpService).deleteMcpEntity(this.mcpServiceDto.getMrn(), McpServiceDto.class);

        // Perform the MVC request
        this.mockMvc.perform(delete("/api/mcp/{mcpEntityType}/{mrn}", McpEntityType.SERVICE.getValue(), this.mcpServiceDto.getMrn())
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
    }

    /**
     * Test that if we do NOT find the MCP service we are trying to delete, an
     * HTTP NOT_FOUND response will be returned.
     */
    @Test
    void testDeleteMcpServiceNotFound() throws Exception {
        doThrow(DataNotFoundException.class).when(this.mcpService).deleteMcpEntity(any(), any());

        // Perform the MVC request
        this.mockMvc.perform(delete("/api/mcp/{mcpEntityType}/{mrn}", McpEntityType.SERVICE.getValue(), this.mcpServiceDto.getMrn()))
                .andExpect(status().isNotFound());
    }

    /**
     * Test that if the service receives a failure when requesting the MCP MIR
     * delete an MCP service, an HTTP BAD_REQUEST response will be returned.
     */
    @Test
    void testDeleteMcpServiceFailed() throws Exception {
        doThrow(DataNotFoundException.class).when(this.mcpService).deleteMcpEntity(any(), any());

        // Perform the MVC request
        this.mockMvc.perform(delete("/api/mcp/{mcpEntityType}/{mrn}", McpEntityType.SERVICE.getValue(), this.mcpServiceDto.getMrn())
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isNotFound())
                .andReturn();
    }

    /**
     * Test that if the service fails to connect to the MCP MIR to delete an
     * MCP service, an HTTP BAD_REQUEST response  will be returned.
     */
    @Test
    void testDeleteMcpServiceMcpConnectivityFailed() throws Exception {
        doThrow(McpConnectivityException.class).when(this.mcpService).deleteMcpEntity(any(), any());

        // Perform the MVC request
        this.mockMvc.perform(delete("/api/mcp/{mcpEntityType}/{mrn}", McpEntityType.SERVICE.getValue(), this.mcpServiceDto.getMrn())
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

}