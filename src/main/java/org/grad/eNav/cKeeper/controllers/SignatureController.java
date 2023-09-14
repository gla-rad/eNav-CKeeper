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

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.grad.eNav.cKeeper.components.DomainDtoMapper;
import org.grad.eNav.cKeeper.models.domain.SignatureCertificate;
import org.grad.eNav.cKeeper.models.domain.mcp.McpEntityType;
import org.grad.eNav.cKeeper.models.dtos.SignatureCertificateDto;
import org.grad.eNav.cKeeper.models.dtos.SignatureVerificationRequestDto;
import org.grad.eNav.cKeeper.services.CertificateService;
import org.grad.eNav.cKeeper.services.SignatureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
     * The X.509 Certificate Algorithm.
     */
    @Value("${gla.rad.ckeeper.x509.cert.algorithm:SHA256withCVC-ECDSA}")
    String defaultSigningtAlgorithm;

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

    /**
     * GET /api/signature/certificate : Requests the certificate to be used for
     * singing payload of a specific entity based on its name, MMSI and type.
     *
     * @return the ResponseEntity with status 200 (OK) if successful, or with
     * status 400 (Bad Request)
     */
    @GetMapping(value = "/certificate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SignatureCertificateDto> getCertificate(@RequestParam(value = "entityName") String entityName,
                                                                  @RequestParam(value = "mmsi", required = false) String mmsi,
                                                                  @RequestParam(value = "version", required = false) String version,
                                                                  @RequestParam(value = "entityType", required = false, defaultValue="device") McpEntityType entityType) {
        log.debug("REST request to get signature certificate for entity with name : {} and version (not required) {}", entityName, version);
        final SignatureCertificate signatureCertificate = this.signatureService.getSignatureCertificate(entityName, version, mmsi, entityType);
        return ResponseEntity.ok()
                .body(this.signatureCertificateDomainToDtoMapper.convertTo(signatureCertificate, SignatureCertificateDto.class));
    }

    /**
     * POST /api/signature/certificate/{certificateId} : Requests a signature
     * for the provided payload based on the provided certificate ID.
     *
     * @return the ResponseEntity with status 200 (OK) if successful, or with
     * status 400 (Bad Request)
     */
    @ApiResponses(value = {
            @ApiResponse(
                content = {
                        @Content(mediaType="text/plain", schema = @Schema(type = "string", format = "byte"))
                })
    })
    @PostMapping(value = "/certificate/{certificateId}", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<byte[]> generateCertificateSignature(@PathVariable BigInteger certificateId,
                                                               @RequestParam(value = "algorithm", required = false) String algorithm,
                                                               @RequestBody @Schema(type = "string", format = "byte") byte[] signaturePayload) {
        log.debug("REST request to get a signature for certificate with ID : {}", certificateId);
        final byte[] result = signatureService.generateEntitySignature(
                certificateId,
                Optional.ofNullable(algorithm).orElse(this.defaultSigningtAlgorithm),
                signaturePayload);
        return ResponseEntity.ok()
                .body(result);
    }

    /**
     * POST /api/signature/entity/generate/{entityName} : Requests a signature
     * for the provided payload based on the entity name.
     *
     * @return the ResponseEntity with status 200 (OK) if successful, or with
     * status 400 (Bad Request)
     */
    @ApiResponses(value = {
            @ApiResponse(
                    content = {
                            @Content(mediaType="text/plain", schema = @Schema(type = "string", format = "byte"))
                    })
    })
    @PostMapping(value = "/entity/generate/{entityName}", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<byte[]> generateEntitySignature(@PathVariable String entityName,
                                                          @RequestParam(value = "mmsi", required = false) String mmsi,
                                                          @RequestParam(value = "version", required = false) String version,
                                                          @RequestParam(value = "entityType", required = false, defaultValue = "device") McpEntityType entityType,
                                                          @RequestParam(value = "algorithm", required = false) String algorithm,
                                                          @RequestBody @Schema(type = "string", format = "byte") byte[] signaturePayload) {
        log.debug("REST request to get a signature for entity with name : {}", entityName);
        final SignatureCertificate signatureCertificate =  this.signatureService.getSignatureCertificate(
                entityName,
                version,
                mmsi,
                entityType);
        final byte[] result = signatureService.generateEntitySignature(
                signatureCertificate.getCertificateId(),
                Optional.ofNullable(algorithm).orElse(this.defaultSigningtAlgorithm),
                signaturePayload);
        return ResponseEntity.ok()
                .body(result);
    }

    /**
     * POST /api/signature/entity/verify/{entityMrn} : Verify the provided
     * content based on the provided entity MRN.
     *
     * @return the ResponseEntity with status 200 (OK) if successful, or with
     * status 400 (Bad Request)
     */
    @PostMapping(value = "/entity/verify/{entityMrn}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> verifyEntitySignatureByMrn(@PathVariable String entityMrn,
                                                           @RequestBody SignatureVerificationRequestDto svr) {
        log.debug("REST request to get verify the signed content for entity with name : {}", entityMrn);
        // Verify the posted signature
        if(this.signatureService.verifyEntitySignatureByMrn(entityMrn, svr.getAlgorithm(), svr.getContent(), svr.getSignature())) {
            return ResponseEntity.ok().build();
        }
        // Otherwise, always return a bad request
        return ResponseEntity.badRequest().build();
    }

    /**
     * POST /api/signature/mmsi/verify/{mmsi} : Verify the provided content
     * based on the provided entity MMSI.
     *
     * @return the ResponseEntity with status 200 (OK) if successful, or with
     * status 400 (Bad Request)
     */
    @PostMapping(value = "/mmsi/verify/{mmsi}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> verifyEntitySignatureByMmsi(@PathVariable String mmsi,
                                                            @RequestBody SignatureVerificationRequestDto svr) {
        log.debug("REST request to get verify the signed content for AtoN with MMSI : {}", mmsi);
        // Verify the posted signature
        if(this.signatureService.verifyEntitySignatureByMmsi(mmsi, svr.getAlgorithm(), svr.getContent(), svr.getSignature())) {
            return ResponseEntity.ok().build();
        }
        // Otherwise, always return a bad request
        return ResponseEntity.badRequest().build();
    }

}
