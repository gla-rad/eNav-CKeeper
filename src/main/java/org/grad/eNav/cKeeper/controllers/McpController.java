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

import lombok.extern.slf4j.Slf4j;
import org.grad.eNav.cKeeper.models.dtos.McpDeviceDto;
import org.grad.eNav.cKeeper.services.McpService;
import org.grad.eNav.cKeeper.utils.HeaderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * REST controller for managing MCP Certificate Operations.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@RestController()
@RequestMapping("/api/mcp")
@Slf4j
public class McpController {

    /**
     * The MCP Service.
     */
    @Autowired
    McpService mcpService;

    /**
     * GET /api/mcp/devices/{id} : Registers a new entity into the MCP MIR.
     *
     * @return the ResponseEntity with status 200 (OK) if successful, or with
     * status 400 (Bad Request)
     */
    @GetMapping(value = "/devices/{mrn}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<McpDeviceDto> getMcpDevice(@PathVariable String mrn) throws IOException {
        log.debug("REST request to get MCP device : {}", mrn);
        return ResponseEntity.ok()
                .body(this.mcpService.getMcpDevice(mrn));
    }

    /**
     * POST /api/mcp/devices : Create a new MCP device.
     *
     * @param mcpDevice the MCP device to create
     * @return the ResponseEntity with status 201 (Created) and with body the
     * new MCP device, or with status 400 (Bad Request) if the MCP device has
     * already an ID or mo MRN
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping(value = "/devices", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<McpDeviceDto> createMcpDevice(@RequestBody McpDeviceDto mcpDevice) throws URISyntaxException {
        log.debug("REST request to create MCP device : {}", mcpDevice);
        if (mcpDevice.getId() != null) {
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert("mcpDevice", "idexists", "A new MCP device cannot already have an ID"))
                    .build();
        } else if (mcpDevice.getMrn() == null) {
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert("mcpDevice", "nomrn", "Cannot create a new MCP device without an MRN"))
                    .build();
        }

        // Save the MRN Entity
        try {
            mcpDevice = this.mcpService.createMcpDevice(mcpDevice);
        } catch (Exception ex) {
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert("mcpDevice", ex.getMessage(), ex.toString()))
                    .body(mcpDevice);
        }

        // Build the response
        return ResponseEntity.created(new URI("/api/mcp/devices/" + mcpDevice.getMrn()))
                .body(mcpDevice);
    }

    /**
     * PUT /api/mcp/devices/{mrn} : Update an existing MCP device.
     *
     * @param mrn the ID of the MCP device to update
     * @param mcpDevice the MCP device to update
     * @return the ResponseEntity with status 201 (Created) and with body the
     * new MRN entity, or with status 400 (Bad Request) if the MCP device has
     * already an ID
     */
    @PutMapping(value = "/devices/{mrn}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<McpDeviceDto> updateMcpDevice(@PathVariable String mrn,
                                                        @RequestBody McpDeviceDto mcpDevice) {
        log.debug("REST request to update MCP device : {}", mcpDevice);
        if (mcpDevice.getId() == null) {
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert("mcpDevice", "noid", "Cannot update an MCP device without an ID"))
                    .build();
        } else if (mcpDevice.getMrn() == null) {
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert("mcpDevice", "nomrn", "Cannot update an MCP device without an MRN"))
                    .build();
        }

        // Save the MRN Entity
        try {
            this.mcpService.updateMcpDevice(mrn, mcpDevice);
        } catch (Exception ex) {
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert("mcpDevice", ex.getMessage(), ex.toString()))
                    .body(mcpDevice);
        }

        // Build the response
        return ResponseEntity.ok()
                .body(mcpDevice);
    }

    /**
     * DELETE /api/mcp/devices/{mrn} : Delete the "MRN" MCP device.
     *
     * @param mrn the MRN of the MCP device to delete
     * @return the ResponseEntity with status 200 (OK), or with status 404
     * (Not Found) if the MCP device was not found
     */
    @DeleteMapping(value = "/devices/{mrn}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteMcpDevice(@PathVariable String mrn) {
        log.debug("REST request to delete MCP device : {}", mrn);

        // Delete the MRN Entity
        try {
            this.mcpService.deleteMcpDevice(mrn);
        } catch (Exception ex) {
            return ResponseEntity.notFound()
                    .build();
        }

        // Build the response
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityDeletionAlert("mrnEntity", mrn))
                .build();
    }

}
