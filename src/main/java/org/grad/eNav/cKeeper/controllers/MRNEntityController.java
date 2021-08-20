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
import org.grad.eNav.cKeeper.models.dtos.datatables.DtPage;
import org.grad.eNav.cKeeper.models.dtos.datatables.DtPagingRequest;
import org.grad.eNav.cKeeper.services.MRNEntityService;
import org.grad.eNav.cKeeper.utils.HeaderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing MRN Entities.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@RestController
@RequestMapping("/api/mrn-entities")
@Slf4j
public class MRNEntityController {

    /**
     * The MRN Entity Service
     */
    @Autowired
    MRNEntityService mrnEntityService;

    /**
     * GET /api/mrn-entities : Returns a paged list of all current MRN entities.
     *
     * @param page the page number to be retrieved
     * @param size the number of entries on each page
     * @return the ResponseEntity with status 200 (OK) and the list of MRN entities in body
     */
    @ResponseStatus
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<MRNEntityDto>> getMrnEntities(@RequestParam("page") Optional<Integer> page,
                                                           @RequestParam("size") Optional<Integer> size) {
        log.debug("REST request to get page of MRN Entities");
        int currentPage = page.orElse(1);
        int pageSize = size.orElse(10);
        Page<MRNEntityDto> mrnEntityPage = this.mrnEntityService.findAll(PageRequest.of(currentPage - 1, pageSize));
        return ResponseEntity.ok()
                .body(mrnEntityPage.getContent());
    }

    /**
     * POST /api/mrn-entities/dt : Returns a paged list of all current MRN entities.
     *
     * @param dtPagingRequest the datatables paging request
     * @return the ResponseEntity with status 200 (OK) and the list of MRN entities in body
     */
    @PostMapping(value = "/dt", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DtPage<MRNEntityDto>> getMrnEntitiesForDatatables(@RequestBody DtPagingRequest dtPagingRequest) {
        log.debug("REST request to get page of MRN Entities for datatables");
        return ResponseEntity.ok()
                .body(this.mrnEntityService.handleDatatablesPagingRequest(dtPagingRequest));
    }

    /**
     * GET /api/mrn-entities/:id : get the "id" MRN entity.
     *
     * @param id the ID of the MRN entity to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the MRN entity, or with status 404 (Not Found)
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MRNEntityDto> getMrnEntity(@PathVariable BigInteger id) {
        log.debug("REST request to get MRN Entity : {}", id);
        return ResponseEntity.ok()
                .body(this.mrnEntityService.findOne(id));
    }

    /**
     * POST /api/mrn-entities : Create a new MRN entity.
     *
     * @param entity the MRN entity to create
     * @return the ResponseEntity with status 201 (Created) and with body the new MRN entity, or with status 400 (Bad Request) if the MRN entity has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MRNEntityDto> createMrnEntity(@RequestBody MRNEntityDto entity) throws Exception, URISyntaxException {
        log.debug("REST request to save MRN Entity : {}", entity);
        if (entity.getId() != null) {
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert("mrnEntity", "idexists", "A new MRN entity cannot already have an ID"))
                    .body(null);
        }

        // Save the MRN Entity
        try {
            entity = this.mrnEntityService.save(entity);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert("mrnEntity", e.getMessage(), e.toString()))
                    .body(entity);
        }

        // Build the response
        return ResponseEntity.created(new URI("/api/mrn-entities/" + entity.getId())).body(entity);
    }

    /**
     * PUT /api/mrn-entities : Update an existing MRN entity.
     *
     * @param id the ID of the MRN entity to update
     * @param entity the MRN entity to update
     * @return the ResponseEntity with status 201 (Created) and with body the new MRN entity, or with status 400 (Bad Request) if the MRN entity has already an ID
     */
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MRNEntityDto> updateMrnEntity(@PathVariable BigInteger id,
                                                        @RequestBody MRNEntityDto entity) {
        log.debug("REST request to update MRN Entity : {}", entity);
        if (id == null) {
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert("mrnEntity", "noid", "Cannot update an MRN entity without an ID"))
                    .body(null);
        } else {
            entity.setId(id);
        }

        // Save the MRN Entity
        try {
            this.mrnEntityService.save(entity);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert("mrnEntity", e.getMessage(), e.toString()))
                    .body(entity);
        }

        // Build the response
        return ResponseEntity.ok().body(entity);
    }

    /**
     * DELETE /api/mrn-entities/:id : Delete the "ID" MRN entity.
     *
     * @param id the ID of the MRN entity to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteMrnEntity(@PathVariable BigInteger id) {
        log.debug("REST request to delete MRN Entity : {}", id);
        this.mrnEntityService.delete(id);
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityDeletionAlert("mrnEntity", id.toString()))
                .build();
    }

}
