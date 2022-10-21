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
import org.grad.eNav.cKeeper.models.dtos.mcp.McpVesselDto;
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
 * REST controller for managing MCP Vessel Operations.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@RestController()
@RequestMapping("/api/mcp/vessel")
@Slf4j
public class McpVesselController {

    /**
     * The MCP Service.
     */
    @Autowired
    McpService mcpService;

    /**
     * GET /api/mcp/vessel/{mrn} : Retrieves a single existing MCP vessel from
     * the MCP MIR.
     *
     * @param mrn the MRN of the MCP vessel to be retrieved
     * @return the ResponseEntity with status 200 (OK) if successful, or with
     * status 400 (Bad Request)
     */
    @GetMapping(value = "/{mrn}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<McpVesselDto> getMcpVessel(@PathVariable String mrn) {
        log.debug("REST request to get MCP vessel : {}", mrn);
        try {
            return ResponseEntity.ok()
                    .body(this.mcpService.getMcpEntity(mrn, null, McpVesselDto.class));
        } catch (McpConnectivityException ex) {
            throw new InvalidRequestException(ex.getMessage());
        }
    }

    /**
     * POST /api/mcp/vessel : Create a new MCP vessel.
     *
     * @param mcpEntityDto the MCP vessel to be created
     * @return the ResponseEntity with status 201 (Created) and with body the
     * new MCP vessel, or with status 400 (Bad Request) if the MCP vessel has
     * already an ID or mo MRN
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<McpVesselDto> createMcpVessel(@RequestBody McpVesselDto mcpEntityDto) throws URISyntaxException {
        log.debug("REST request to create MCP vessel : {}", mcpEntityDto);
        if (mcpEntityDto.getId() != null) {
            throw new InvalidRequestException("A new MCP vessel cannot already have an ID");
        } else if (mcpEntityDto.getMrn() == null) {
            throw new InvalidRequestException("Cannot create a new MCP vessel without an MRN");
        }

        // Save the MRN Entity
        try {
            mcpEntityDto = this.mcpService.createMcpEntity(mcpEntityDto);
        } catch (McpConnectivityException ex) {
            throw new InvalidRequestException(ex.getMessage());
        }

        // Build the response
        return ResponseEntity.created(new URI("/api/mcp/vessel/" + mcpEntityDto.getMrn()))
                .body(mcpEntityDto);
    }

    /**
     * PUT /api/mcp/vessel/{mrn} : Update an existing MCP vessel.
     *
     * @param mrn the ID of the MCP vessel to be updated
     * @param mcpEntityDto the MCP vessel to update
     * @return the ResponseEntity with status 201 (Created) and with body the
     * new MRN entity, or with status 400 (Bad Request) if the MCP vessel has
     * already an ID
     */
    @PutMapping(value = "/{mrn}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<McpVesselDto> updateMcpVessel(@PathVariable String mrn,
                                                        @RequestBody McpVesselDto mcpEntityDto) {
        log.debug("REST request to update MCP vessel : {}", mcpEntityDto);
        if (mcpEntityDto.getId() == null) {
            throw new InvalidRequestException("Cannot update an MCP vessel without an ID");
        } else if (mcpEntityDto.getMrn() == null) {
            throw new InvalidRequestException("Cannot update an MCP vessel without an MRN");
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
     * DELETE /api/mcp/vessel/{mrn} : Delete the "MRN" MCP vessel.
     *
     * @param mrn the MRN of the MCP vessel to be deleted
     * @return the ResponseEntity with status 200 (OK), or with status 404
     * (Not Found) if the MCP vessel was not found
     */
    @DeleteMapping(value = "/{mrn}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteMcpVessel(@PathVariable String mrn) {
        log.debug("REST request to delete MCP vessel : {}", mrn);

        // Delete the MRN Entity
        try {
            this.mcpService.deleteMcpEntity(mrn, null, McpVesselDto.class);
        } catch (McpConnectivityException ex) {
            throw new InvalidRequestException(ex.getMessage());
        }

        // Build the response
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityDeletionAlert("mrnEntity", mrn))
                .build();
    }

}
