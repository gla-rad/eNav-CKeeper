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

import lombok.extern.slf4j.Slf4j;
import org.grad.eNav.cKeeper.exceptions.InvalidRequestException;
import org.grad.eNav.cKeeper.exceptions.McpConnectivityException;
import org.grad.eNav.cKeeper.models.dtos.mcp.McpDeviceDto;
import org.grad.eNav.cKeeper.services.McpService;
import org.grad.eNav.cKeeper.utils.HeaderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * REST controller for managing MCP Device Operations.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@RestController()
@RequestMapping("/api/mcp/device")
@Slf4j
public class McpDeviceController {

    /**
     * The MCP Service.
     */
    @Autowired
    McpService mcpService;

    /**
     * GET /api/mcp/device/{mrn} : Retrieves a single MCP device from the MCP
     * MIR.
     *
     * @param mrn the MRN of the MCP device to be retrieved
     * @return the ResponseEntity with status 200 (OK) if successful, or with
     * status 400 (Bad Request)
     */
    @GetMapping(value = "/{mrn}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<McpDeviceDto> getMcpDevice(@PathVariable String mrn) {
        log.debug("REST request to get MCP device : {}", mrn);
        try {
            return ResponseEntity.ok()
                    .body(this.mcpService.getMcpEntity(mrn, null, McpDeviceDto.class));
        } catch (McpConnectivityException ex) {
            throw new InvalidRequestException(ex.getMessage());
        }
    }

    /**
     * POST /api/mcp/device : Create a new MCP device.
     *
     * @param mcpEntityDto the MCP device to be created
     * @return the ResponseEntity with status 201 (Created) and with body the
     * new MCP device, or with status 400 (Bad Request) if the MCP device has
     * already an ID or mo MRN
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<McpDeviceDto> createMcpDevice(@RequestBody McpDeviceDto mcpEntityDto) throws URISyntaxException {
        log.debug("REST request to create MCP device : {}", mcpEntityDto);
        if (mcpEntityDto.getId() != null) {
            throw new InvalidRequestException("A new MCP device cannot already have an ID");
        } else if (mcpEntityDto.getMrn() == null) {
            throw new InvalidRequestException("Cannot create a new MCP device without an MRN");
        }

        // Save the MRN Entity
        try {
            mcpEntityDto = this.mcpService.createMcpEntity(mcpEntityDto);
        } catch (McpConnectivityException ex) {
            throw new InvalidRequestException(ex.getMessage());
        }

        // Build the response
        return ResponseEntity.created(new URI("/api/mcp/device/" + mcpEntityDto.getMrn()))
                .body(mcpEntityDto);
    }

    /**
     * PUT /api/mcp/device/{mrn} : Update an existing MCP device.
     *
     * @param mrn the ID of the MCP device to be updated
     * @param mcpEntityDto the MCP device to update
     * @return the ResponseEntity with status 201 (Created) and with body the
     * new MRN entity, or with status 400 (Bad Request) if the MCP device has
     * already an ID
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping(value = "/{mrn}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<McpDeviceDto> updateMcpDevice(@PathVariable String mrn,
                                                        @RequestBody McpDeviceDto mcpEntityDto) {
        log.debug("REST request to update MCP device : {}", mcpEntityDto);
        if (mcpEntityDto.getId() == null) {
            throw new InvalidRequestException("Cannot update an MCP device without an ID");
        } else if (mcpEntityDto.getMrn() == null) {
            throw new InvalidRequestException("Cannot update an MCP device without an MRN");
        }

        // Save the MRN Entity
        try {
            this.mcpService.updateMcpEntity(mrn, mcpEntityDto);
        } catch (McpConnectivityException ex) {
            throw new InvalidRequestException(ex.getMessage());
        }

        // Build the response
        return ResponseEntity.ok()
                .body(mcpEntityDto);
    }

    /**
     * DELETE /api/mcp/device/{mrn} : Delete the "MRN" MCP device.
     *
     * @param mrn the MRN of the MCP device to be deleted
     * @return the ResponseEntity with status 200 (OK), or with status 404
     * (Not Found) if the MCP device was not found
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping(value = "/{mrn}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteMcpDevice(@PathVariable String mrn) {
        log.debug("REST request to delete MCP device : {}", mrn);

        // Delete the MRN Entity
        try {
            this.mcpService.deleteMcpEntity(mrn, null, McpDeviceDto.class);
        } catch (McpConnectivityException ex) {
            throw new InvalidRequestException(ex.getMessage());
        }

        // Build the response
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityDeletionAlert("mrnEntity", mrn))
                .build();
    }

}
