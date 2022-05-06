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
 *
 */

package org.grad.eNav.cKeeper.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.grad.eNav.cKeeper.models.dtos.McpDeviceDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class McpConfigServiceTest {

    /**
     * The Tested Service.
     */
    @InjectMocks
    @Spy
    McpConfigService mcpConfigService;

    // Test Variables
    private ObjectMapper objectMapper;
    private McpDeviceDto mcpDevice;
    private CloseableHttpClient httpClient;
    private CloseableHttpResponse httpResponse;
    private StatusLine statusLine;
    private Header locationHeader;
    private HttpEntity httpEntity;

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setUp() {
        // Initialise the absolutely necessary parameters
        this.mcpConfigService.mcpDevicePrefix = "urn:mrn:mcp:device:mcc:";
    }

    /**
     * Test that we correctly construct the endpoint URLs for the MCP.
     */
    @Test
    void testConstructMcpDeviceEndpointUrl() {
        // First set the host and the organization registered to the MCP
        this.mcpConfigService.host = "localhost";
        this.mcpConfigService.mcpOrgPrefix = "urn:mrn:mcp:org:mcc";
        this.mcpConfigService.organisation = "grad";

        // Make the assertions
        assertEquals("https://localhost/x509/api/org/urn:mrn:mcp:org:mcc:grad/null/", this.mcpConfigService.constructMcpDeviceEndpointUrl(null));
        assertEquals("https://localhost/x509/api/org/urn:mrn:mcp:org:mcc:grad//", this.mcpConfigService.constructMcpDeviceEndpointUrl(""));
        assertEquals("https://localhost/x509/api/org/urn:mrn:mcp:org:mcc:grad/test/", this.mcpConfigService.constructMcpDeviceEndpointUrl("test"));
        assertEquals("https://localhost/x509/api/org/urn:mrn:mcp:org:mcc:grad/test2/", this.mcpConfigService.constructMcpDeviceEndpointUrl("test2"));
        assertEquals("https://localhost/x509/api/org/urn:mrn:mcp:org:mcc:grad/test3/", this.mcpConfigService.constructMcpDeviceEndpointUrl("test3"));
    }

    /**
     * Test that we correctly construct the MCP devices MRNs.
     */
    @Test
    void testConstructMcpDeviceMrn() {
        // First set the host and the organization registered to the MCP
        this.mcpConfigService.host = "localhost";
        this.mcpConfigService.mcpDevicePrefix = "urn:mrn:mcp:device:mcc";
        this.mcpConfigService.organisation = "grad";

        // Make the assertions
        assertEquals("urn:mrn:mcp:device:mcc:grad:null", this.mcpConfigService.constructMcpDeviceMrn(null));
        assertEquals("urn:mrn:mcp:device:mcc:grad:", this.mcpConfigService.constructMcpDeviceMrn(""));
        assertEquals("urn:mrn:mcp:device:mcc:grad:test", this.mcpConfigService.constructMcpDeviceMrn("test"));
        assertEquals("urn:mrn:mcp:device:mcc:grad:test2", this.mcpConfigService.constructMcpDeviceMrn("test2"));
        assertEquals("urn:mrn:mcp:device:mcc:grad:test3", this.mcpConfigService.constructMcpDeviceMrn("test3"));
    }

}