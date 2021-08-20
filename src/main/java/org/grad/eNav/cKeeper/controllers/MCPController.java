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
import org.grad.eNav.cKeeper.models.dtos.MRNEntityDto;
import org.grad.eNav.cKeeper.services.MCPService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * REST controller for managing MCP Certificate Operations.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@RestController()
@RequestMapping("/api/mcp")
@Slf4j
public class MCPController {

    /**
     * The MCP Service.
     */
    @Autowired
    MCPService mcpService;

    /**
     * GET /api/mcp/register : Registers a new entity into the MCP MIR.
     *
     * @return the ResponseEntity with status 200 (OK) if successful, or with status 400 (Bad Request)
     */
    @ResponseStatus
    @GetMapping(value = "/register", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<MRNEntityDto>> getMrnEntities() throws IOException, NoSuchAlgorithmException, KeyManagementException {
        log.debug("REST request to get page of MRN Entities");
        this.mcpService.registerEntity();
        return ResponseEntity.ok()
                .build();
    }

}
