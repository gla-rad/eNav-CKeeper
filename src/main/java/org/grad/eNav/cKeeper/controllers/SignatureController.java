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
import org.grad.eNav.cKeeper.components.DomainDtoMapper;
import org.grad.eNav.cKeeper.models.domain.SignatureCertificate;
import org.grad.eNav.cKeeper.models.domain.mcp.McpEntityType;
import org.grad.eNav.cKeeper.models.dtos.SignatureCertificateDto;
import org.grad.eNav.cKeeper.models.dtos.SignatureVerificationRequestDto;
import org.grad.eNav.cKeeper.services.CertificateService;
import org.grad.eNav.cKeeper.services.SignatureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.Optional;

/**
 * REST controller for managing signatures.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@RestController()
@RequestMapping("/api/signature")
@Slf4j
public class SignatureController {

    /**
     * The Certificate Service.
     */
    @Autowired
    CertificateService certificateService;

    /**
     * The Signature Service.
     */
    @Autowired
    SignatureService signatureService;

    /**
     * Certificate Mapper from Domain to DTO.
     */
    @Autowired
    DomainDtoMapper<SignatureCertificate, SignatureCertificateDto> signatureCertificateDomainToDtoMapper;

    // Class Variables
    final public static String CKEEPER_PUBLIC_CERTIFICATE_HEADER = "PUBLIC_CERTIFICATE";
    final public static String CKEEPER_SIGNATURE_ALGORITHM = "SIGNATURE_ALGORITHM";
    final public static String CKEEPER_ROOT_CERTIFICATE_THUMBPRINT = "ROOT_CERTIFICATE_THUMBPRINT";

    /**
     * GET /api/signature/entity/certificate/{entityName} : Requests a signature
     * for the provided payload based on the entity ID.
     *
     * @return the ResponseEntity with status 200 (OK) if successful, or with
     * status 400 (Bad Request)
     */
    @GetMapping(value = "/entity/certificate/{entityName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SignatureCertificateDto> getSignatureCertificate(@PathVariable String entityName,
                                                                           @RequestParam(value = "mmsi", required = false) String mmsi,
                                                                           @RequestParam("entityType") McpEntityType entityType) {
        log.debug("REST request to get Signature Certificate for : {}", entityName);
        return ResponseEntity.ok()
                .body(this.signatureCertificateDomainToDtoMapper.convertTo(this.signatureService.getSignatureCertificate(entityName, mmsi, entityType), SignatureCertificateDto.class));
    }

    /**
     * POST /api/signature/entity/generate/{entityName} : Requests a signature
     * for the provided payload based on the entity name.
     *
     * @return the ResponseEntity with status 200 (OK) if successful, or with
     * status 400 (Bad Request)
     */
    @PostMapping(value = "/entity/generate/{entityName}", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<byte[]> generateEntitySignature(@PathVariable String entityName,
                                                          @RequestParam(value = "mmsi", required = false) String mmsi,
                                                          @RequestParam(value = "entityType", required = false) McpEntityType entityType,
                                                          @RequestParam(value = "certificateId", required = false) BigInteger certificateId,
                                                          @RequestBody byte[] signaturePayload) {
        log.debug("REST request to get a signature for entity with ID : {}", entityName);
        final byte[] result = signatureService.generateEntitySignature(entityName,
                mmsi, Optional.ofNullable(entityType).orElse(McpEntityType.DEVICE), certificateId, signaturePayload);
        return ResponseEntity.ok()
                .body(result);
    }

    /**
     * POST /api/signature/entity/verify/{entityName} : Verify the provided
     * content based on the provided entity name.
     *
     * @return the ResponseEntity with status 200 (OK) if successful, or with
     * status 400 (Bad Request)
     */
    @PostMapping(value = "/entity/verify/{entityName}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> verifyEntitySignature(@PathVariable String entityName,
                                                      @RequestBody SignatureVerificationRequestDto svr) {
        log.debug("REST request to get verify the signed content for entity with ID : {}", entityName);
        // Verify the posted signature
        if(this.signatureService.verifyEntitySignature(entityName, svr.getContent(), svr.getSignature())) {
            return ResponseEntity.ok().build();
        }
        // Otherwise, always return a bad request
        return ResponseEntity.badRequest().build();
    }

    /**
     * POST /api/signature/mmsi/verify/{mmsi} : Verify the provided content
     * based on the provided MMSI.
     *
     * @return the ResponseEntity with status 200 (OK) if successful, or with
     * status 400 (Bad Request)
     */
    @PostMapping(value = "/mmsi/verify/{mmsi}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> verifyEntitySignatureByMmsi(@PathVariable String mmsi,
                                                            @RequestBody SignatureVerificationRequestDto svr) {
        log.debug("REST request to get verify the signed content for AtoN with MMSI : {}", mmsi);
        // Verify the posted signature
        if(this.signatureService.verifyEntitySignatureByMmsi(mmsi, svr.getContent(), svr.getSignature())) {
            return ResponseEntity.ok().build();
        }
        // Otherwise, always return a bad request
        return ResponseEntity.badRequest().build();
    }

}
