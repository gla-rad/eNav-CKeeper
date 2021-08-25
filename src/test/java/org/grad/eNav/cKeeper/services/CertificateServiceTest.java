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

package org.grad.eNav.cKeeper.services;

import org.bouncycastle.operator.OperatorCreationException;
import org.grad.eNav.cKeeper.exceptions.DataNotFoundException;
import org.grad.eNav.cKeeper.exceptions.SavingFailedException;
import org.grad.eNav.cKeeper.models.domain.Certificate;
import org.grad.eNav.cKeeper.models.domain.MRNEntity;
import org.grad.eNav.cKeeper.models.dtos.CertificateDto;
import org.grad.eNav.cKeeper.repos.CertificateRepo;
import org.grad.eNav.cKeeper.repos.MRNEntityRepo;
import org.grad.eNav.cKeeper.utils.X509Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CertificateServiceTest {

    /**
     * The Tested Service.
     */
    @InjectMocks
    @Spy
    CertificateService certificateService;

    /**
     * The Certificate Repo mock.
     */
    @Mock
    CertificateRepo certificateRepo;

    /**
     * The MRN Entity Repo mock.
     */
    @Mock
    MRNEntityRepo mrnEntityRepo;

    /**
     * The MCP Service mock.
     */
    @Mock
    McpService mcpService;

    // Test Variables
    private Certificate certificate;
    private MRNEntity mrnEntity;

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setUp() {
        // Create an existing MRN entity
        this.mrnEntity = new MRNEntity();
        this.mrnEntity.setId(BigInteger.ONE);
        this.mrnEntity.setName("Existing Entity Name");
        this.mrnEntity.setMrn("urn:mrn:mcp:device:mcc:grad:test");

        // Create an existing certificate object
        this.certificate = new Certificate();
        this.certificate.setId(BigInteger.ONE);
        this.certificate.setMrnEntity(this.mrnEntity);
        this.certificate.setCertificate("CERTIFICATE");
        this.certificate.setPublicKey("PUBLIC KEY");
        this.certificate.setPrivateKey("PRIVATE KEY");
        this.certificate.setStartDate(new Date());
        this.certificate.setEndDate(new Date());
        this.certificate.setRevoked(Boolean.FALSE);
    }

    /**
     * Test that we can retrieve all the certificates associated with a specific
     * MRN entity, using the MRN entity ID.
     */
    @Test
    void testFindAllByMrnEntityId() {
        doReturn(Collections.singleton(this.certificate)).when(certificateRepo).findAllByMrnEntityId(this.mrnEntity.getId());

        // Perform the service call
        Set<CertificateDto> result = this.certificateService.findAllByMrnEntityId(this.mrnEntity.getId());

        // Make sure the response is correct
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(new CertificateDto(this.certificate), result.toArray()[0]);
    }

    /**
     * Test that we can correctly generate a new X.509 certificate for a given
     * MRN entity, based on the provided MRN entity ID.
     */
    @Test
    void testGenerateMrnEntityCertificate() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, CertificateException, OperatorCreationException, IOException {
        // Spin up a self-signed
        this.certificateService.certDirName="CN=Test";
        KeyPair keyPair = X509Utils.generateKeyPair(null);
        X509Certificate x509Certificate = X509Utils.generateX509Certificate(keyPair, this.certificateService.certDirName, new Date(), new Date(), null);

        doReturn(Optional.of(this.mrnEntity)).when(this.mrnEntityRepo).findById(this.mrnEntity.getId());
        doReturn(x509Certificate).when(this.mcpService).issueMcpDeviceCertificate(eq(this.mrnEntity.getMrn()), any());
        doReturn(this.certificate).when(this.certificateRepo).save(any());

        // Perform the service call
        CertificateDto result = this.certificateService.generateMrnEntityCertificate(this.mrnEntity.getId());

        // Make sure the response is correct
        assertNotNull(result);
        assertEquals(this.certificate.getId(), result.getId());
        assertEquals(this.certificate.getMrnEntity().getId(), result.getMrnEntityId());
        assertEquals(this.certificate.getPublicKey(), result.getPublicKey());
        assertEquals(this.certificate.getStartDate(), result.getStartDate());
        assertEquals(this.certificate.getEndDate(), result.getEndDate());
        assertEquals(this.certificate.getRevoked(), result.getRevoked());
    }

    /**
     * Test that if we attempt to generate a certificate for an MRN entity that
     * does not exist, a DataNotFoundException will be thrown.
     */
    @Test
    void testGenerateMrnEntityCertificateNoEntity() {
        doReturn(Optional.empty()).when(this.mrnEntityRepo).findById(any());

        // Perform the service call
        assertThrows(DataNotFoundException.class, () ->
                this.certificateService.generateMrnEntityCertificate(this.mrnEntity.getId())
        );
    }

    /**
     * Test that even if we manage to generate a successful X.509 certificate
     * for our MRN entity, if the service fails to save the result in the
     * database, a SavingFailedException will be thrown.
     */
    @Test
    void testGenerateMrnEntityCertificateSaveFailed() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, CertificateException, OperatorCreationException, IOException {
        // Spin up a self-signed
        this.certificateService.certDirName="CN=Test";
        KeyPair keyPair = X509Utils.generateKeyPair(null);
        X509Certificate x509Certificate = X509Utils.generateX509Certificate(keyPair, this.certificateService.certDirName, new Date(), new Date(), null);

        doReturn(Optional.of(this.mrnEntity)).when(this.mrnEntityRepo).findById(this.mrnEntity.getId());
        doReturn(x509Certificate).when(this.mcpService).issueMcpDeviceCertificate(eq(this.mrnEntity.getMrn()), any());

        // Perform the service call
        assertThrows(SavingFailedException.class, () ->
                this.certificateService.generateMrnEntityCertificate(this.mrnEntity.getId())
        );
    }

    /**
     * Test that we can successfully delete an existing certificate entry from
     * the database.
     *
     * TODO: This operation should also revoke the certificate from the MCP MIR
     * but for now let's go with this.
     */
    @Test
    void testDelete() {
        doReturn(Boolean.TRUE).when(this.certificateRepo).existsById(this.certificate.getId());

        // Perform the service call
        this.certificateService.delete(this.certificate.getId());
    }

    /**
     * Test that if the certificate to be deleted is not found, based on the
     * provided ID, a DataNotFoundException will be thrown.
     */
    @Test
    void testDeleteNotFound() {
        doReturn(Boolean.FALSE).when(this.certificateRepo).existsById(this.certificate.getId());

        // Perform the service call
        assertThrows(DataNotFoundException.class, () ->
                this.certificateService.delete(this.certificate.getId())
        );
    }

    /**
     * Test that we can actually revoke a certificate based on the provided
     * certificate ID. Note that this operation should also revoke the
     * certificate from the MCP MIR, but currently we don't have the MIR's
     * certificate ID, so we can't actually do that.
     */
    @Test
    void testRevoke() throws IOException {
        doReturn(Optional.of(this.certificate)).when(this.certificateRepo).findById(this.certificate.getId());
        doReturn(this.certificate).when(this.certificateRepo).save(any());

        // Perform the service call
        CertificateDto result = this.certificateService.revoke(this.mrnEntity.getId());

        // Make sure the response is correct
        assertNotNull(result);
        assertEquals(this.certificate.getId(), result.getId());
        assertEquals(this.certificate.getMrnEntity().getId(), result.getMrnEntityId());
        assertEquals(this.certificate.getPublicKey(), result.getPublicKey());
        assertEquals(this.certificate.getStartDate(), result.getStartDate());
        assertEquals(this.certificate.getEndDate(), result.getEndDate());
        assertEquals(Boolean.TRUE, result.getRevoked());
    }

    /**
     * Test that if we try revoke a certificate based on the provided
     * certificate ID and this does NOT exist, then a DataNotFoundException
     * will be thrown.
     */
    @Test
    void testRevokeNotFound() throws IOException {
        doReturn(Optional.empty()).when(this.certificateRepo).findById(this.certificate.getId());

        // Perform the service call
        assertThrows(DataNotFoundException.class, () ->
            this.certificateService.revoke(this.mrnEntity.getId())
        );
    }

    /**
     * Test that even if we actually revoke a certificate based on the provided
     * certificate ID, but the local database entry fails to be updates, a
     * SavingFailedException will be thrown.
     */
    @Test
    void testRevokeSaveFailed() throws IOException {
        doReturn(Optional.of(this.certificate)).when(this.certificateRepo).findById(this.certificate.getId());

        // Perform the service call
        assertThrows(SavingFailedException.class, () ->
                this.certificateService.revoke(this.mrnEntity.getId())
        );
    }

}