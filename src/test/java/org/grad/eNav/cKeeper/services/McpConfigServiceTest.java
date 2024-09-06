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

package org.grad.eNav.cKeeper.services;

import org.grad.eNav.cKeeper.models.domain.mcp.McpEntityType;
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

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setUp() {
        // Initialise the absolutely necessary parameters
        this.mcpConfigService.mcpEntityPrefix = "urn:mrn:mcp:device:mcc:";
    }

    /**
     * Test that we correctly construct the check URLs for the MCP.
     */
    @Test
    void testConstructMcpCheckUrl() {
        // First set the host and the organization registered to the MCP
        this.mcpConfigService.host = "localhost";
        this.mcpConfigService.mcpOrgPrefix = "urn:mrn:mcp:org:mcc";
        this.mcpConfigService.organisation = "grad";

        // Make the assertions
        assertEquals("https://localhost/x509/api/org/urn:mrn:mcp:org:mcc:grad", this.mcpConfigService.constructMcpCheckUrl());
    }

    /**
     * Test that we correctly construct the base URLs for the MCP.
     */
    @Test
    void testConstructMcpBaseUrl() {
        // First set the host and the organization registered to the MCP
        this.mcpConfigService.host = "localhost";
        this.mcpConfigService.mcpOrgPrefix = "urn:mrn:mcp:org:mcc";
        this.mcpConfigService.organisation = "grad";

        // Make the assertions
        assertEquals("https://localhost/x509/api/org/urn:mrn:mcp:org:mcc:grad/", this.mcpConfigService.constructMcpBaseUrl());
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
        assertEquals("https://localhost/x509/api/org/urn:mrn:mcp:org:mcc:grad/null/", this.mcpConfigService.constructMcpEndpointUrl(null));
        assertEquals("https://localhost/x509/api/org/urn:mrn:mcp:org:mcc:grad//", this.mcpConfigService.constructMcpEndpointUrl(""));
        assertEquals("https://localhost/x509/api/org/urn:mrn:mcp:org:mcc:grad/test/", this.mcpConfigService.constructMcpEndpointUrl("test"));
        assertEquals("https://localhost/x509/api/org/urn:mrn:mcp:org:mcc:grad/test2/", this.mcpConfigService.constructMcpEndpointUrl("test2"));
        assertEquals("https://localhost/x509/api/org/urn:mrn:mcp:org:mcc:grad/test3/", this.mcpConfigService.constructMcpEndpointUrl("test3"));
    }

    /**
     * Test that we correctly construct the MCP device MRNs.
     */
    @Test
    void testConstructMcpDeviceMrn() {
        // First set the host and the organization registered to the MCP
        this.mcpConfigService.host = "localhost";
        this.mcpConfigService.mcpEntityPrefix = "urn:mrn:mcp";
        this.mcpConfigService.mcpEntitySuffix = "mcc";
        this.mcpConfigService.organisation = "grad";

        // Make the assertions for device
        assertEquals("urn:mrn:mcp:device:mcc:grad:", this.mcpConfigService.constructMcpEntityMrn(McpEntityType.DEVICE, null));
        assertEquals("urn:mrn:mcp:device:mcc:grad:", this.mcpConfigService.constructMcpEntityMrn(McpEntityType.DEVICE, ""));
        assertEquals("urn:mrn:mcp:device:mcc:grad:test", this.mcpConfigService.constructMcpEntityMrn(McpEntityType.DEVICE, "test"));
        assertEquals("urn:mrn:mcp:device:mcc:grad:test2", this.mcpConfigService.constructMcpEntityMrn(McpEntityType.DEVICE, "test2"));
        assertEquals("urn:mrn:mcp:device:mcc:grad:test3", this.mcpConfigService.constructMcpEntityMrn(McpEntityType.DEVICE, "test3"));
        assertEquals("urn:mrn:mcp:device:mcc:grad:test_test", this.mcpConfigService.constructMcpEntityMrn(McpEntityType.DEVICE,"Test_Test"));
        assertEquals("urn:mrn:mcp:device:mcc:grad:test.test", this.mcpConfigService.constructMcpEntityMrn(McpEntityType.DEVICE,"Test.Test"));
        assertEquals("urn:mrn:mcp:device:mcc:grad:test-test", this.mcpConfigService.constructMcpEntityMrn(McpEntityType.DEVICE,"Test Test"));
    }

    /**
     * Test that we correctly construct the MCP service MRNs.
     */
    @Test
    void testConstructMcpServiceMrn() {
        // First set the host and the organization registered to the MCP
        this.mcpConfigService.host = "localhost";
        this.mcpConfigService.mcpEntityPrefix = "urn:mrn:mcp";
        this.mcpConfigService.mcpEntitySuffix = "mcc";
        this.mcpConfigService.organisation = "grad";

        // Make the assertions for service
        assertEquals("urn:mrn:mcp:service:mcc:grad:instance:", this.mcpConfigService.constructMcpEntityMrn(McpEntityType.SERVICE,null));
        assertEquals("urn:mrn:mcp:service:mcc:grad:instance:", this.mcpConfigService.constructMcpEntityMrn(McpEntityType.SERVICE,""));
        assertEquals("urn:mrn:mcp:service:mcc:grad:instance:test", this.mcpConfigService.constructMcpEntityMrn(McpEntityType.SERVICE,"test"));
        assertEquals("urn:mrn:mcp:service:mcc:grad:instance:test2", this.mcpConfigService.constructMcpEntityMrn(McpEntityType.SERVICE,"test2"));
        assertEquals("urn:mrn:mcp:service:mcc:grad:instance:test3", this.mcpConfigService.constructMcpEntityMrn(McpEntityType.SERVICE,"test3"));
        assertEquals("urn:mrn:mcp:service:mcc:grad:instance:test_test", this.mcpConfigService.constructMcpEntityMrn(McpEntityType.SERVICE,"Test_Test"));
        assertEquals("urn:mrn:mcp:service:mcc:grad:instance:test.test", this.mcpConfigService.constructMcpEntityMrn(McpEntityType.SERVICE,"Test.Test"));
        assertEquals("urn:mrn:mcp:service:mcc:grad:instance:test-test", this.mcpConfigService.constructMcpEntityMrn(McpEntityType.SERVICE,"Test Test"));
    }

    /**
     * Test that we correctly construct the MCP user MRNs.
     */
    @Test
    void testConstructMcpUserMrn() {
        // First set the host and the organization registered to the MCP
        this.mcpConfigService.host = "localhost";
        this.mcpConfigService.mcpEntityPrefix = "urn:mrn:mcp";
        this.mcpConfigService.mcpEntitySuffix = "mcc";
        this.mcpConfigService.organisation = "grad";

        // Make the assertions for user
        assertEquals("urn:mrn:mcp:user:mcc:grad:", this.mcpConfigService.constructMcpEntityMrn(McpEntityType.USER,null));
        assertEquals("urn:mrn:mcp:user:mcc:grad:", this.mcpConfigService.constructMcpEntityMrn(McpEntityType.USER,""));
        assertEquals("urn:mrn:mcp:user:mcc:grad:test", this.mcpConfigService.constructMcpEntityMrn(McpEntityType.USER,"test"));
        assertEquals("urn:mrn:mcp:user:mcc:grad:test2", this.mcpConfigService.constructMcpEntityMrn(McpEntityType.USER,"test2"));
        assertEquals("urn:mrn:mcp:user:mcc:grad:test3", this.mcpConfigService.constructMcpEntityMrn(McpEntityType.USER,"test3"));
        assertEquals("urn:mrn:mcp:user:mcc:grad:test_test", this.mcpConfigService.constructMcpEntityMrn(McpEntityType.USER,"Test_Test"));
        assertEquals("urn:mrn:mcp:user:mcc:grad:test.test", this.mcpConfigService.constructMcpEntityMrn(McpEntityType.USER,"Test.Test"));
        assertEquals("urn:mrn:mcp:user:mcc:grad:test-test", this.mcpConfigService.constructMcpEntityMrn(McpEntityType.USER,"Test Test"));
    }

    /**
     * Test that we correctly construct the MCP vessel MRNs.
     */
    @Test
    void testConstructMcpVesselMrn() {
        // First set the host and the organization registered to the MCP
        this.mcpConfigService.host = "localhost";
        this.mcpConfigService.mcpEntityPrefix = "urn:mrn:mcp";
        this.mcpConfigService.mcpEntitySuffix = "mcc";
        this.mcpConfigService.organisation = "grad";

        // Make the assertions for vessel
        assertEquals("urn:mrn:mcp:vessel:mcc:grad:", this.mcpConfigService.constructMcpEntityMrn(McpEntityType.VESSEL,null));
        assertEquals("urn:mrn:mcp:vessel:mcc:grad:", this.mcpConfigService.constructMcpEntityMrn(McpEntityType.VESSEL,""));
        assertEquals("urn:mrn:mcp:vessel:mcc:grad:test", this.mcpConfigService.constructMcpEntityMrn(McpEntityType.VESSEL,"test"));
        assertEquals("urn:mrn:mcp:vessel:mcc:grad:test2", this.mcpConfigService.constructMcpEntityMrn(McpEntityType.VESSEL,"test2"));
        assertEquals("urn:mrn:mcp:vessel:mcc:grad:test3", this.mcpConfigService.constructMcpEntityMrn(McpEntityType.VESSEL,"test3"));
        assertEquals("urn:mrn:mcp:vessel:mcc:grad:test_test", this.mcpConfigService.constructMcpEntityMrn(McpEntityType.VESSEL,"Test_Test"));
        assertEquals("urn:mrn:mcp:vessel:mcc:grad:test.test", this.mcpConfigService.constructMcpEntityMrn(McpEntityType.VESSEL,"Test.Test"));
        assertEquals("urn:mrn:mcp:vessel:mcc:grad:test-test", this.mcpConfigService.constructMcpEntityMrn(McpEntityType.VESSEL,"Test Test"));
    }

    /**
     * Test that we correctly construct the MCP role MRNs.
     */
    @Test
    void testConstructMcpRoleMrn() {
        // First set the host and the organization registered to the MCP
        this.mcpConfigService.host = "localhost";
        this.mcpConfigService.mcpEntityPrefix = "urn:mrn:mcp";
        this.mcpConfigService.mcpEntitySuffix = "mcc";
        this.mcpConfigService.organisation = "grad";

        // Make the assertions for role
        assertEquals("urn:mrn:mcp:role:mcc:grad:", this.mcpConfigService.constructMcpEntityMrn(McpEntityType.ROLE,null));
        assertEquals("urn:mrn:mcp:role:mcc:grad:", this.mcpConfigService.constructMcpEntityMrn(McpEntityType.ROLE,""));
        assertEquals("urn:mrn:mcp:role:mcc:grad:test", this.mcpConfigService.constructMcpEntityMrn(McpEntityType.ROLE,"test"));
        assertEquals("urn:mrn:mcp:role:mcc:grad:test2", this.mcpConfigService.constructMcpEntityMrn(McpEntityType.ROLE,"test2"));
        assertEquals("urn:mrn:mcp:role:mcc:grad:test3", this.mcpConfigService.constructMcpEntityMrn(McpEntityType.ROLE,"test3"));
        assertEquals("urn:mrn:mcp:role:mcc:grad:test_test", this.mcpConfigService.constructMcpEntityMrn(McpEntityType.ROLE,"Test_Test"));
        assertEquals("urn:mrn:mcp:role:mcc:grad:test.test", this.mcpConfigService.constructMcpEntityMrn(McpEntityType.ROLE,"Test.Test"));
        assertEquals("urn:mrn:mcp:role:mcc:grad:test-test", this.mcpConfigService.constructMcpEntityMrn(McpEntityType.ROLE,"Test Test"));
    }

}