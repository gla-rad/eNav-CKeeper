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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * The MCP Configuration Service
 *
 * This service provides a basic functionality for accessing the configuration
 * of the MCP integration.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Service
@Slf4j
public class McpConfigService {

    /**
     * The MCP Host Address.
     */
    @Value("${gla.rad.ckeeper.mcp.host}")
    String host;

    /**
     * The MCP Registered Organisation Name.
     */
    @Value("${gla.rad.ckeeper.mcp.organisation:grad}")
    String organisation;

    /**
     * The MCP Organisation Sting Prefix.
     */
    @Value("${gla.rad.ckeeper.mcp.mrnOrgPrefix:urn:mrn:mcp:org:mcc}")
    String mcpOrgPrefix;

    /**
     * The MCP Device Sting Prefix.
     */
    @Value("${gla.rad.ckeeper.mcp.mrnDevicePrefix:urn:mrn:mcp:device:mcc}")
    String mcpDevicePrefix;

    /**
     * The MCP Keystore File Location.
     */
    @Value("${gla.rad.ckeeper.mcp.keyStore:mcp/keystore.p12}")
    String keyStore;

    /**
     * The MCP keystore File Password.
     */
    @Value("${gla.rad.ckeeper.mcp.keyStorePass:keyStorePass}")
    String keyStorePass;

    /**
     * A helper function to construct the appropriate MCP endpoint URL, based
     * on the currently loaded host, registered organisation and endpoint to
     * be reached.
     *
     * @param endpoint  The MCP endpoint to be reached
     * @return the complete MCP endpoint URL
     */
    public String constructMcpDeviceEndpointUrl(String endpoint) {
        return String.format("https://%s/x509/api/org/%s:%s/%s/", this.host, this.mcpOrgPrefix, this.organisation, endpoint);
    }

    /**
     * A helper function to construct the appropriate device MRN, based on the
     * provided device ID.
     *
     * @param deviceId  The ID of the device to construct the MRN from
     * @return The constructed device MRN
     */
    public String constructMcpDeviceMrn(String deviceId) {
        return Optional.ofNullable(deviceId).orElse("").startsWith(this.mcpDevicePrefix) ? deviceId : String.format("%s:%s:%s", this.mcpDevicePrefix, this.organisation, deviceId);
    }

}
