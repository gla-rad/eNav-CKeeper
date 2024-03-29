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
import org.grad.eNav.cKeeper.components.DomainDtoMapper;
import org.grad.eNav.cKeeper.exceptions.InvalidRequestException;
import org.grad.eNav.cKeeper.exceptions.McpConnectivityException;
import org.grad.eNav.cKeeper.models.domain.Certificate;
import org.grad.eNav.cKeeper.models.domain.MrnEntity;
import org.grad.eNav.cKeeper.models.dtos.CertificateDto;
import org.grad.eNav.cKeeper.models.dtos.MrnEntityDto;
import org.grad.eNav.cKeeper.services.CertificateService;
import org.grad.eNav.cKeeper.utils.HeaderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigInteger;

/**
 * REST controller for managing Certificate Operations.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@RestController()
@RequestMapping("/api/certificate")
@Slf4j
public class CertificateController {

    /**
     * The Certificate Service.
     */
    @Autowired
    CertificateService certificateService;

    /**
     * Model Mapper from Domain to DTO.
     */
    @Autowired
    DomainDtoMapper<Certificate, CertificateDto> certificateDomainToDtoMapper;

    /**
     * PUT /api/certificate/{id}/revoke : Revoke the "ID" certificate.
     *
     * @param id the ID of the certificate to be revoked
     * @return the ResponseEntity with status 200 (OK)
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping(value = "/{id}/revoke", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CertificateDto> revokeCertificate(@PathVariable BigInteger id) {
        log.debug("REST request to revoke Certificate : {}", id);
        try {
            return ResponseEntity.ok()
                    .body(this.certificateDomainToDtoMapper.convertTo(this.certificateService.revoke(id), CertificateDto.class));
        } catch (IOException | McpConnectivityException ex) {
            throw new InvalidRequestException(ex.getMessage());
        }
    }

    /**
     * DELETE /api/certificate/{id} : Delete the "ID" certificate.
     *
     * @param id the ID of the certificate to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteCertificate(@PathVariable BigInteger id) {
        log.debug("REST request to delete Certificate : {}", id);
        this.certificateService.delete(id);
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityDeletionAlert("certificate", id.toString()))
                .build();
    }

}
