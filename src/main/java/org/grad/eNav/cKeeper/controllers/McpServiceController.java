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
import org.grad.eNav.cKeeper.exceptions.InvalidRequestException;
import org.grad.eNav.cKeeper.exceptions.McpConnectivityException;
import org.grad.eNav.cKeeper.models.dtos.mcp.McpServiceDto;
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
 * REST controller for managing MCP Service Operations.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@RestController()
@RequestMapping("/api/mcp/service")
@Slf4j
public class McpServiceController {

    /**
     * The MCP Service.
     */
    @Autowired
    McpService mcpService;

    /**
     * GET /api/mcp/service/{mrn}/{version} : Retrieves a single existing MCP
     * service from the MCP MIR.
     *
     * @param mrn the MRN of the MCP service to be retrieved
     * @param version the instance version of the MCP service to be retrieved
     * @return the ResponseEntity with status 200 (OK) if successful, or with
     * status 400 (Bad Request)
     */
    @GetMapping(value = "/{mrn}/{version}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<McpServiceDto> getMcpService(@PathVariable String mrn,
                                                       @PathVariable String version) {
        log.debug("REST request to get MCP service : {}", mrn);
        try {
            return ResponseEntity.ok()
                    .body(this.mcpService.getMcpEntity(mrn, version, McpServiceDto.class));
        } catch (IOException | McpConnectivityException ex) {
            throw new InvalidRequestException(ex.getMessage());
        }
    }

    /**
     * POST /api/mcp/service : Create a new MCP service.
     *
     * @param mcpServiceDto the MCP service to be created
     * @return the ResponseEntity with status 201 (Created) and with body the
     * new MCP service, or with status 400 (Bad Request) if the MCP service has
     * already an ID or mo MRN
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<McpServiceDto> createMcpService(@RequestBody McpServiceDto mcpServiceDto) throws URISyntaxException {
        log.debug("REST request to create MCP service : {}", mcpServiceDto);
        if (mcpServiceDto.getId() != null) {
            throw new InvalidRequestException("A new MCP service cannot already have an ID");
        } else if (mcpServiceDto.getMrn() == null) {
            throw new InvalidRequestException("Cannot create a new MCP service without an MRN");
        }

        // Save the MRN Entity
        try {
            mcpServiceDto = this.mcpService.createMcpEntity(mcpServiceDto);
        } catch (IOException | McpConnectivityException ex) {
            throw new InvalidRequestException(ex.getMessage());
        }

        // Build the response
        return ResponseEntity.created(new URI("/api/mcp/service/" + mcpServiceDto.getMrn()))
                .body(mcpServiceDto);
    }

    /**
     * PUT /api/mcp/service/{mrn}/{version} : Update an existing MCP service.
     *
     * @param mrn the ID of the MCP service to be updated
     * @param mcpServiceDto the MCP service to update
     * @return the ResponseEntity with status 201 (Created) and with body the
     * new MRN entity, or with status 400 (Bad Request) if the MCP service has
     * already an ID
     */
    @PutMapping(value = "/{mrn}/{version}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<McpServiceDto> updateMcpService(@PathVariable String mrn,
                                                          @RequestBody McpServiceDto mcpServiceDto) {
        log.debug("REST request to update MCP service : {}", mcpServiceDto);
        if (mcpServiceDto.getId() == null) {
            throw new InvalidRequestException("Cannot update an MCP service without an ID");
        } else if (mcpServiceDto.getMrn() == null) {
            throw new InvalidRequestException("Cannot update an MCP service without an MRN");
        }

        // Save the MRN Entity
        try {
            this.mcpService.updateMcpEntity(mrn, mcpServiceDto);
        } catch (IOException | McpConnectivityException ex) {
            throw new InvalidRequestException(ex.getMessage());
        }

        // Build the response
        return ResponseEntity.ok()
                .body(mcpServiceDto);
    }

    /**
     * DELETE /api/mcp/service/{mrn}/{version} : Delete the "MRN" MCP service.
     *
     * @param mrn the MRN of the MCP service to be deleted
     * @param version the instance version of the MCP service to be deleted
     * @return the ResponseEntity with status 200 (OK), or with status 404
     * (Not Found) if the MCP service was not found
     */
    @DeleteMapping(value = "/{mrn}/{version}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteMcpService(@PathVariable String mrn,
                                                 @PathVariable String version) {
        log.debug("REST request to delete MCP service : {}", mrn);

        // Delete the MRN Entity
        try {
            this.mcpService.deleteMcpEntity(mrn, version, McpServiceDto.class);
        } catch (IOException | McpConnectivityException ex) {
            throw new InvalidRequestException(ex.getMessage());
        }

        // Build the response
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityDeletionAlert("mrnEntity", mrn))
                .build();
    }

}
