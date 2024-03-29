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

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.operator.OperatorCreationException;
import org.grad.eNav.cKeeper.components.DomainDtoMapper;
import org.grad.eNav.cKeeper.exceptions.InvalidRequestException;
import org.grad.eNav.cKeeper.exceptions.McpConnectivityException;
import org.grad.eNav.cKeeper.models.domain.Certificate;
import org.grad.eNav.cKeeper.models.domain.MrnEntity;
import org.grad.eNav.cKeeper.models.dtos.CertificateDto;
import org.grad.eNav.cKeeper.models.dtos.MrnEntityDto;
import org.grad.eNav.cKeeper.models.dtos.datatables.DtPage;
import org.grad.eNav.cKeeper.models.dtos.datatables.DtPagingRequest;
import org.grad.eNav.cKeeper.services.CertificateService;
import org.grad.eNav.cKeeper.services.MrnEntityService;
import org.grad.eNav.cKeeper.utils.HeaderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.Set;

/**
 * REST controller for managing MRN Entities.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@RestController
@RequestMapping("/api/mrn-entity")
@Slf4j
public class MrnEntityController {

    /**
     * The MRN Entity Service
     */
    @Autowired
    MrnEntityService mrnEntityService;

    /**
     * The Certificate Service.
     */
    @Autowired
    CertificateService certificateService;

    /**
     * MRN Entity Mapper from Domain to DTO.
     */
    @Autowired
    DomainDtoMapper<MrnEntity, MrnEntityDto> mrnEntityDomainToDtoMapper;

    /**
     * MRN Entity Mapper from DTO to Domain.
     */
    @Autowired
    DomainDtoMapper<MrnEntityDto, MrnEntity> mrnEntityDtoToDomainMapper;

    /**
     * Certificate Mapper from Domain to DTO.
     */
    @Autowired
    DomainDtoMapper<Certificate, CertificateDto> certificateDomainToDtoMapper;

    /**
     * GET /api/mrn-entity : Returns a paged list of all current MRN entities.
     *
     * @param page the page number to be retrieved
     * @param size the number of entries on each page
     * @return the ResponseEntity with status 200 (OK) and the list of MRN entities in body
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<MrnEntityDto>> getMrnEntities(@RequestParam("page") Optional<Integer> page,
                                                             @RequestParam("size") Optional<Integer> size) {
        log.debug("REST request to get page of MRN Entities");
        int currentPage = page.orElse(1);
        int pageSize = size.orElse(10);
        final Page<MrnEntity> result = this.mrnEntityService.findAll(PageRequest.of(currentPage - 1, pageSize));
        return ResponseEntity.ok()
                .body(this.mrnEntityDomainToDtoMapper.convertToPage(result, MrnEntityDto.class));
    }

    /**
     * POST /api/mrn-entity/dt : Returns a paged list of all current MRN entities.
     *
     * @param dtPagingRequest the datatables paging request
     * @return the ResponseEntity with status 200 (OK) and the list of MRN entities in body
     */
    @PostMapping(value = "/dt", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DtPage<MrnEntityDto>> getMrnEntitiesForDatatables(@RequestBody DtPagingRequest dtPagingRequest) {
        log.debug("REST request to get page of MRN Entities for datatables");
        final Page<MrnEntity> result = this.mrnEntityService.handleDatatablesPagingRequest(dtPagingRequest);
        return ResponseEntity.ok()
                .body(this.mrnEntityDomainToDtoMapper.convertToDtPage(result, dtPagingRequest, MrnEntityDto.class));
    }

    /**
     * GET /api/mrn-entity/{id} : get the "ID" MRN entity.
     *
     * @param id the ID of the MRN entity to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the MRN entity, or with status 404 (Not Found)
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MrnEntityDto> getMrnEntity(@PathVariable BigInteger id) {
        log.debug("REST request to get MRN Entity : {}", id);
        final MrnEntity result = this.mrnEntityService.findOne(id);
        return ResponseEntity.ok()
                .body(this.mrnEntityDomainToDtoMapper.convertTo(result, MrnEntityDto.class));
    }

    /**
     * POST /api/mrn-entity : Create a new MRN entity.
     *
     * @param entity the MRN entity to create
     * @return the ResponseEntity with status 201 (Created) and with body the new MRN entity, or with status 400 (Bad Request) if the MRN entity has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MrnEntityDto> createMrnEntity(@RequestBody @Valid MrnEntityDto entity) throws Exception, URISyntaxException {
        log.debug("REST request to save MRN Entity : {}", entity);
        if (entity.getId() != null) {
            throw new InvalidRequestException("A new MRN entity cannot already have an ID");
        }

        // Save the MRN Entity
        final MrnEntity result = this.mrnEntityService.save(this.mrnEntityDtoToDomainMapper.convertTo(entity, MrnEntity.class));

        // Build the response
        return ResponseEntity.created(new URI("/api/mrn-entities/" + entity.getId()))
                .body(this.mrnEntityDomainToDtoMapper.convertTo(result, MrnEntityDto.class));
    }

    /**
     * PUT /api/mrn-entity/{id} : Update an existing MRN entity.
     *
     * @param id the ID of the MRN entity to update
     * @param entity the MRN entity to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated MRN entity, or with status 400 (Bad Request) if the MRN entity does not have an ID
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MrnEntityDto> updateMrnEntity(@PathVariable BigInteger id,
                                                        @RequestBody @Valid MrnEntityDto entity) {
        log.debug("REST request to update MRN Entity : {}", entity);

        // Save the MRN Entity
        entity.setId(id);
        final MrnEntity result = this.mrnEntityService.save(this.mrnEntityDtoToDomainMapper.convertTo(entity, MrnEntity.class));

        // Build the response
        return ResponseEntity.ok()
                .body(this.mrnEntityDomainToDtoMapper.convertTo(result, MrnEntityDto.class));
    }

    /**
     * DELETE /api/mrn-entities/{id} : Delete the "ID" MRN entity.
     *
     * @param id the ID of the MRN entity to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteMrnEntity(@PathVariable BigInteger id) {
        log.debug("REST request to delete MRN Entity : {}", id);
        this.mrnEntityService.delete(id);
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityDeletionAlert("mrnEntity", id.toString()))
                .build();
    }

    /**
     * GET /api/mrn-entities/{id}/certificates : get the "ID" MRN entity
     * certificates.
     *
     * @param id the ID of the MRN entity to retrieve the certificates for
     * @return the ResponseEntity with status 200 (OK) and with body the certificates set, or with status 404 (Not Found)
     */
    @GetMapping(value = "/{id}/certificates", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Set<CertificateDto>> getMrnEntityCertificates(@PathVariable BigInteger id) {
        log.debug("REST request to get MRN Entity certificates: {}", id);
        final Set<Certificate> result = this.certificateService.findAllByMrnEntityId(id);
        return ResponseEntity.ok()
                .body(this.certificateDomainToDtoMapper.convertToSet(result, CertificateDto.class));
    }

    /**
     * PUT /api/mrn-entities/{id}/certificates : put a new certificate in the
     * "ID" MRN entity.
     *
     * @param id the ID of the MRN entity to generate the certificates for
     * @return the ResponseEntity with status 200 (OK) and with body the new certificates, or with status 404 (Not Found)
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping(value = "/{id}/certificates", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CertificateDto> putMrnEntityCertificate(@PathVariable BigInteger id) {
        log.debug("REST request to generate a new certificates for MRN Entity : {}", id);
        final Certificate result;
        try {
            result = this.certificateService.generateMrnEntityCertificate(id);
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException | OperatorCreationException | IOException | McpConnectivityException ex) {
            throw new InvalidRequestException(ex.getMessage());
        }
        return ResponseEntity.ok()
                .body(this.certificateDomainToDtoMapper.convertTo(result, CertificateDto.class));
    }

}
