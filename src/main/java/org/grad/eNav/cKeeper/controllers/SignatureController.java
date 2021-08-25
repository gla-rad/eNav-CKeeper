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
import org.grad.eNav.cKeeper.models.dtos.McpDeviceDto;
import org.grad.eNav.cKeeper.services.SignatureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

/**
 * REST controller for managing signatures.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@RestController()
@RequestMapping("/api/signatures")
@Slf4j
public class SignatureController {

    /**
     * The Signature Service.
     */
    @Autowired
    SignatureService signatureService;

    /**
     * POST /api/signatures/generate : Requests a signature for the provided payload
     * based on the
     *
     * @return the ResponseEntity with status 200 (OK) if successful, or with
     * status 400 (Bad Request)
     */
    @PostMapping(value = "/generate", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<byte[]> generateAtoNSignature(@RequestParam("atonUID") String atonUID,
                                                              @RequestBody byte[] signaturePayload) throws IOException {
        log.debug("REST request to get a signature for AtoN with UID : {}", atonUID);
        try {
            return ResponseEntity.ok()
                    .body(signatureService.generateAtonSignature(atonUID, signaturePayload));
        } catch(IOException | NoSuchAlgorithmException | SignatureException | InvalidKeyException | InvalidKeySpecException ex) {
            throw new InvalidRequestException(ex.getMessage());
        }
    }

    /**
     * POST /api/signatures/generate : Requests a signature for the provided payload
     * based on the
     *
     * @return the ResponseEntity with status 200 (OK) if successful, or with
     * status 400 (Bad Request)
     */
    @PostMapping(value = "/verify", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<McpDeviceDto> verifyAtoNSignature(@RequestParam("atonUID") String atonUID,
                                                            @RequestBody byte[] signedContent) throws IOException {
        log.debug("REST request to get verify the signed content for AtoN with UID : {}", atonUID);
        // Verify the posted signature
        if(this.signatureService.verifyAtonSignature(atonUID, signedContent)) {
            return ResponseEntity.ok().build();
        }
        // Otherwise, always return a bad request
        return ResponseEntity.badRequest().build();
    }

}
