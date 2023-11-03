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

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.grad.eNav.cKeeper.exceptions.*;
import org.grad.eNav.cKeeper.models.domain.Pair;
import org.grad.eNav.cKeeper.models.domain.mcp.McpEntityType;
import org.grad.eNav.cKeeper.models.dtos.mcp.McpCertitifateDto;
import org.grad.eNav.cKeeper.models.dtos.mcp.McpDeviceDto;
import org.grad.eNav.cKeeper.models.dtos.mcp.McpServiceDto;
import org.grad.eNav.cKeeper.utils.X509Utils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class McpServiceTest {

    /**
     * The Tested Service.
     */
    @InjectMocks
    @Spy
    McpService mcpService;

    /**
     * The MCP Base Service Mock.
     */
    @Mock
    McpConfigService mcpConfigService;

    // The Test Mock Web Server (to test the webClient)
    public static MockWebServer mockBackEnd;

    // Test Variables
    private ObjectMapper objectMapper;
    private McpDeviceDto mcpDeviceDto;
    private McpServiceDto mcpServiceDto;
    private KeyPair keypair;
    private PKCS10CertificationRequest csr;
    private X509Certificate cert;
    private McpCertitifateDto mcpCertitifateDto;

    /**
     * Before all tests start a mock web-server.
     */
    @BeforeAll
    static void backEndSetUp() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
    }

    /**
     * Shutdown the mock web-server after all tests are done.
     */
    @AfterAll
    static void backEndTearDown() throws IOException {
        mockBackEnd.shutdown();
    }

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setUp() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, CertificateException, OperatorCreationException, IOException {
        // Setup an MCP device for testing
        this.mcpDeviceDto = new McpDeviceDto("Test","urn:mrn:mcp:device:mcc:grad:test");
        this.mcpServiceDto = new McpServiceDto("Test", "urn:mrn:mcp:service:mcc:grad:instance:test");
        this.mcpServiceDto.setInstanceVersion("0.0.1");

        // Create a new MCP Certificate DTO object
        this.keypair = X509Utils.generateKeyPair(null);
        this.csr = X509Utils.generateX509CSR(keypair, "CN=Test", null);
        this.cert = X509Utils.generateX509Certificate(this.keypair, "CN=Test", new Date(), new Date(), null);
        this.mcpCertitifateDto = new McpCertitifateDto();
        this.mcpCertitifateDto.setId(BigInteger.ONE);
        this.mcpCertitifateDto.setSerialNumber(String.valueOf(cert.getSerialNumber()));
        this.mcpCertitifateDto.setStart(cert.getNotBefore());
        this.mcpCertitifateDto.setEnd(cert.getNotAfter());
        this.mcpCertitifateDto.setCertificate(X509Utils.formatCertificate(this.cert));

        // Add a real object mapper to the service
        this.objectMapper = new ObjectMapper();

        // Mock the MCP Base URL to point to the mock-server
        doReturn(String.format("http://localhost:%s", mockBackEnd.getPort())).when(this.mcpConfigService).constructMcpBaseUrl();
    }

    /**
     * Test that the MCP service can initialise correctly and read the MCP
     * X.509 certificate for accessing the MIR functionality.
     */
    @Test
    void testInit() throws UnrecoverableKeyException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, CertificateException {
        this.mcpService.keyStore = "keystore.jks";
        this.mcpService.keyStorePassword = "password";
        this.mcpService.init();

        // Make sure we've got a connection to the MCP MIR
        assertNotNull(this.mcpService.mcpMirClient);
    }

    /**
     * Test that we can retrieve a specific MCP device based on the provided
     * MRN number. Note that the last MRN section (device ID) can also be
     * provided since the previous MRN sections are pretty standard, based
     * on your organisation.
     */
    @Test
    void testGetMcpDevice() throws McpConnectivityException, UnrecoverableKeyException, CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        // Mock the MCP Base Service
        mockBackEnd.enqueue(new MockResponse()
                .setBody(this.objectMapper.writeValueAsString(this.mcpDeviceDto))
                .addHeader("Content-Type", "application/json")
                .setResponseCode(HttpStatus.OK.value()));

        // Mock the service secondary calls
        doNothing().when(this.mcpService).checkMcpMirConnectivity();
        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());

        // Init the service
        this.mcpService.init();

        // Perform the service call
        McpDeviceDto result = this.mcpService.getMcpEntity(this.mcpDeviceDto.getMrn(), null, McpDeviceDto.class);

        // Make sure the response is correct
        assertNotNull(result);
        assertNotNull(result);
        assertEquals(this.mcpDeviceDto.getId(), result.getId());
        assertEquals(this.mcpDeviceDto.getCreatedAt(), result.getCreatedAt());
        assertEquals(this.mcpDeviceDto.getUpdatedAt(), result.getUpdatedAt());
        assertEquals(this.mcpDeviceDto.getName(), result.getName());
        assertEquals(this.mcpDeviceDto.getMrn(), result.getMrn());
        assertEquals(this.mcpDeviceDto.getIdOrganization(), result.getIdOrganization());
    }

    /**
     * Test that if the provided MRN is not valid and the MCP does not return
     * a valid result, the service will throw a DataNotFoundException.
     */
    @Test
    void testGetMcpDeviceNotFound() throws McpConnectivityException, UnrecoverableKeyException, CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        // Mock the MCP Base Service
        mockBackEnd.enqueue(new MockResponse()
                .setBody(StringUtils.EMPTY)
                .addHeader("Content-Type", "application/json")
                .setResponseCode(HttpStatus.NOT_FOUND.value()));

        // Mock the service secondary calls
        doNothing().when(this.mcpService).checkMcpMirConnectivity();
        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());

        // Init the service
        this.mcpService.init();

        // Perform the service call
        assertThrows(DataNotFoundException.class, () ->
            this.mcpService.getMcpEntity(this.mcpDeviceDto.getMrn(), null, McpDeviceDto.class)
        );
    }

    /**
     * Test that we can successfully create a new MCP device based on the
     * provided object. That means that both name and MRN fields are required
     * to be populated.
     */
    @Test
    void testCreateMcpDevice() throws McpConnectivityException, UnrecoverableKeyException, CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        // Mock the MCP Base Service
        mockBackEnd.enqueue(new MockResponse()
                .setBody(this.objectMapper.writeValueAsString(this.mcpDeviceDto))
                .addHeader("Content-Type", "application/json")
                .setResponseCode(HttpStatus.CREATED.value()));

        // Mock the service secondary calls
        doNothing().when(this.mcpService).checkMcpMirConnectivity();
        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());

        // Init the service
        this.mcpService.init();

        // Perform the service call
        McpDeviceDto result = this.mcpService.createMcpEntity(this.mcpDeviceDto);

        // Make sure the response is correct
        assertNotNull(result);
        assertEquals(this.mcpDeviceDto.getId(), result.getId());
        assertEquals(this.mcpDeviceDto.getCreatedAt(), result.getCreatedAt());
        assertEquals(this.mcpDeviceDto.getUpdatedAt(), result.getUpdatedAt());
        assertEquals(this.mcpDeviceDto.getName(), result.getName());
        assertEquals(this.mcpDeviceDto.getMrn(), result.getMrn());
        assertEquals(this.mcpDeviceDto.getIdOrganization(), result.getIdOrganization());
    }

    /**
     * Test that if we cannot successfully create a new MCP device based on the
     * provided object, the service will throw a SavingFailedException.
     */
    @Test
    void testCreateMcpDeviceFailed() throws McpConnectivityException, UnrecoverableKeyException, CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        // Mock the MCP Base Service
        mockBackEnd.enqueue(new MockResponse()
                .setBody(StringUtils.EMPTY)
                .addHeader("Content-Type", "application/json")
                .setResponseCode(HttpStatus.BAD_REQUEST.value()));

        // Mock the service secondary calls
        doNothing().when(this.mcpService).checkMcpMirConnectivity();
        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());

        // Init the service
        this.mcpService.init();

        // Perform the service call
        assertThrows(SavingFailedException.class, () ->
                this.mcpService.createMcpEntity(this.mcpDeviceDto)
        );
    }

    /**
     * Test that we can successfully update an existing MCP device based on the
     * provided MRN and object. That means that both name and MRN fields are
     * required to be populated.
     */
    @Test
    void testUpdateMcpDevice() throws McpConnectivityException, UnrecoverableKeyException, CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        // Mock the MCP Base Service
        // One response enqueued for the update and one for the get
        mockBackEnd.enqueue(new MockResponse()
                .setBody(StringUtils.EMPTY)
                .addHeader("Content-Type", "application/json")
                .setResponseCode(HttpStatus.OK.value()));
        mockBackEnd.enqueue(new MockResponse()
                .setBody(this.objectMapper.writeValueAsString(this.mcpDeviceDto))
                .addHeader("Content-Type", "application/json")
                .setResponseCode(HttpStatus.OK.value()));

        // Mock the service secondary calls
        doNothing().when(this.mcpService).checkMcpMirConnectivity();
        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());

        // Init the service
        this.mcpService.init();

        // Perform the service call
        McpDeviceDto result = this.mcpService.updateMcpEntity(this.mcpDeviceDto.getMrn(), this.mcpDeviceDto);

        // Make sure the response is correct
        assertNotNull(result);
        assertEquals(this.mcpDeviceDto.getId(), result.getId());
        assertEquals(this.mcpDeviceDto.getCreatedAt(), result.getCreatedAt());
        assertEquals(this.mcpDeviceDto.getUpdatedAt(), result.getUpdatedAt());
        assertEquals(this.mcpDeviceDto.getName(), result.getName());
        assertEquals(this.mcpDeviceDto.getMrn(), result.getMrn());
        assertEquals(this.mcpDeviceDto.getIdOrganization(), result.getIdOrganization());
    }

    /**
     * Test that if we cannot successfully update an existing MCP device based
     * on the provided MRN and object, the service will throw a
     * SavingFailedException.
     */
    @Test
    void testUpdateMcpDeviceFailed() throws McpConnectivityException, UnrecoverableKeyException, CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        // Mock the MCP Base Service
        mockBackEnd.enqueue(new MockResponse()
                .setBody(StringUtils.EMPTY)
                .addHeader("Content-Type", "application/json")
                .setResponseCode(HttpStatus.BAD_REQUEST.value()));

        // Mock the service secondary calls
        doNothing().when(this.mcpService).checkMcpMirConnectivity();
        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());

        // Init the service
        this.mcpService.init();

        // Perform the service call
        assertThrows(SavingFailedException.class, () ->
                this.mcpService.updateMcpEntity(this.mcpDeviceDto.getMrn(), this.mcpDeviceDto)
        );
    }

    /**
     * Test that we can successfully delete an existing MCP device based on the
     * provided MRN.
     */
    @Test
    void testDeleteMcpDevice() throws IOException, McpConnectivityException, UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        // Mock the MCP Base Service
        mockBackEnd.enqueue(new MockResponse()
                .setBody(StringUtils.EMPTY)
                .addHeader("Content-Type", "application/json")
                .setResponseCode(HttpStatus.OK.value()));

        // Mock the service secondary calls
        doNothing().when(this.mcpService).checkMcpMirConnectivity();
        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());

        // Init the service
        this.mcpService.init();

        // Perform the service call
        boolean result = this.mcpService.deleteMcpEntity(this.mcpDeviceDto.getMrn(), null, McpDeviceDto.class);

        // Make sure the response is correct
        assertTrue(result);
    }

    /**
     * Test that if we cannot successfully delete an existing MCP device based
     * on the provided MRN, the service will throw a  DeletingFailedException.
     */
    @Test
    void testDeleteMcpDeviceFailed() throws IOException, McpConnectivityException, UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        // Mock the MCP Base Service
        mockBackEnd.enqueue(new MockResponse()
                .setBody(StringUtils.EMPTY)
                .addHeader("Content-Type", "application/json")
                .setResponseCode(HttpStatus.BAD_REQUEST.value()));

        // Mock the service secondary calls
        doNothing().when(this.mcpService).checkMcpMirConnectivity();
        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());

        // Init the service
        this.mcpService.init();

        // Perform the service call
        assertThrows(DeletingFailedException.class, () ->
                this.mcpService.deleteMcpEntity(this.mcpDeviceDto.getMrn(),null, McpDeviceDto.class)
        );
    }

    /**
     * Test that we can correctly retrieve and translate the X509 certificates
     * for an MCP device from the MCP Identity Registry.
     */
    @Test
    void testGetMcpDeviceCertificates() throws McpConnectivityException, UnrecoverableKeyException, CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        // Add a certificate onto the MCP entity
        this.mcpDeviceDto.setCertificates(Collections.singletonList(this.mcpCertitifateDto));

        // Mock the MCP Base Service
        mockBackEnd.enqueue(new MockResponse()
                .setBody(this.objectMapper.writeValueAsString(this.mcpDeviceDto))
                .addHeader("Content-Type", "application/json")
                .setResponseCode(HttpStatus.OK.value()));

        // Mock the service secondary calls
        doNothing().when(this.mcpService).checkMcpMirConnectivity();
        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());

        // Init the service
        this.mcpService.init();

        // Perform the service call
        Map<String, X509Certificate> certificates = this.mcpService.getMcpEntityCertificates(McpEntityType.DEVICE, this.mcpDeviceDto.getMrn(), null);

        // Make sure the result looks OK
        assertNotNull(certificates);
        assertEquals(1, certificates.size());

        // Assert the retrieved certificates
        assertTrue(certificates.containsKey(this.mcpCertitifateDto.getSerialNumber()));
        assertEquals(this.mcpCertitifateDto.getStart(), certificates.get(this.mcpCertitifateDto.getSerialNumber()).getNotBefore());
        assertEquals(this.mcpCertitifateDto.getEnd(), certificates.get(this.mcpCertitifateDto.getSerialNumber()).getNotAfter());
        assertEquals(this.mcpCertitifateDto.getCertificate(), X509Utils.formatCertificate(certificates.get(this.mcpCertitifateDto.getSerialNumber())));
    }

    /**
     * Test that we can successfully retrieve the X509 certificates for an MCP
     * device from the MCP Identity Registry, a DataNotFoundException will be
     * thrown.
     */
    @Test
    void testGetMcpDeviceCertificatesFailed() throws McpConnectivityException, UnrecoverableKeyException, CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        // Mock the MCP Base Service
        mockBackEnd.enqueue(new MockResponse()
                .setBody(StringUtils.EMPTY)
                .addHeader("Content-Type", "application/json")
                .setResponseCode(HttpStatus.BAD_REQUEST.value()));

        // Mock the service secondary calls
        doNothing().when(this.mcpService).checkMcpMirConnectivity();
        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());

        // Init the service
        this.mcpService.init();

        // Perform the service call
        assertThrows(DataNotFoundException.class, () ->
                this.mcpService.getMcpEntityCertificates(McpEntityType.DEVICE, this.mcpDeviceDto.getMrn(), null)
        );
    }

    /**
     * Test if we cannot successfully retrieve and translate the certificates
     * for an MCP device from the MCP Identity Registry, and the response is
     * invalid, the certificate will be omitted.
     */
    @Test
    void testGetMcpDeviceCertificatesInvalid() throws McpConnectivityException, UnrecoverableKeyException, CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        // Add an invalid certificate onto the MCP entity
        this.mcpCertitifateDto.setCertificate("INVALID");
        this.mcpDeviceDto.setCertificates(Collections.singletonList(this.mcpCertitifateDto));

        // Mock the MCP Base Service
        mockBackEnd.enqueue(new MockResponse()
                .setBody(this.objectMapper.writeValueAsString(this.mcpDeviceDto))
                .addHeader("Content-Type", "application/json")
                .setResponseCode(HttpStatus.OK.value()));

        // Mock the service secondary calls
        doNothing().when(this.mcpService).checkMcpMirConnectivity();
        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());

        // Init the service
        this.mcpService.init();

        // Perform the service call
        assertTrue(this.mcpService.getMcpEntityCertificates(McpEntityType.DEVICE, this.mcpDeviceDto.getMrn(), null).isEmpty());
    }

    /**
     * Test that we can correctly retrieve and translate the X509 certificates
     * for an MCP service from the MCP Identity Registry.
     */
    @Test
    void testGetMcpServiceCertificates() throws McpConnectivityException, UnrecoverableKeyException, CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        // Add a certificate onto the MCP entity
        this.mcpDeviceDto.setCertificates(Collections.singletonList(this.mcpCertitifateDto));

        // Mock the MCP Base Service
        mockBackEnd.enqueue(new MockResponse()
                .setBody(this.objectMapper.writeValueAsString(this.mcpDeviceDto))
                .addHeader("Content-Type", "application/json")
                .setResponseCode(HttpStatus.OK.value()));

        // Mock the service secondary calls
        doNothing().when(this.mcpService).checkMcpMirConnectivity();
        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());

        // Init the service
        this.mcpService.init();

        // Perform the service call
        Map<String, X509Certificate> certificates = this.mcpService.getMcpEntityCertificates(McpEntityType.SERVICE, this.mcpServiceDto.getMrn(), this.mcpServiceDto.getInstanceVersion());

        // Make sure the result looks OK
        assertNotNull(certificates);
        assertEquals(1, certificates.size());

        // Assert the retrieved certificates
        assertTrue(certificates.containsKey(this.mcpCertitifateDto.getSerialNumber()));
        assertEquals(this.mcpCertitifateDto.getStart(), certificates.get(this.mcpCertitifateDto.getSerialNumber()).getNotBefore());
        assertEquals(this.mcpCertitifateDto.getEnd(), certificates.get(this.mcpCertitifateDto.getSerialNumber()).getNotAfter());
        assertEquals(this.mcpCertitifateDto.getCertificate(), X509Utils.formatCertificate(certificates.get(this.mcpCertitifateDto.getSerialNumber())));
    }

    /**
     * Test that we can successfully retrieve the X509 certificates for an MCP
     * device from the MCP Identity Registry, a DataNotFoundException will be
     * thrown.
     */
    @Test
    void testGetMcpServiceCertificatesFailed() throws McpConnectivityException, UnrecoverableKeyException, CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        // Mock the MCP Base Service
        mockBackEnd.enqueue(new MockResponse()
                .setBody(StringUtils.EMPTY)
                .addHeader("Content-Type", "application/json")
                .setResponseCode(HttpStatus.BAD_REQUEST.value()));

        // Mock the service secondary calls
        doNothing().when(this.mcpService).checkMcpMirConnectivity();
        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());

        // Init the service
        this.mcpService.init();

        // Perform the service call
        assertThrows(DataNotFoundException.class, () ->
                this.mcpService.getMcpEntityCertificates(McpEntityType.SERVICE, this.mcpDeviceDto.getMrn(), null)
        );
    }

    /**
     * Test if we cannot successfully retrieve and translate the certificates
     * for an MCP service from the MCP Identity Registry, and the response is
     * invalid, the certificate will be omitted.
     */
    @Test
    void testGetMcpServiceCertificatesInvalid() throws McpConnectivityException, UnrecoverableKeyException, CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        // Add an invalid certificate onto the MCP entity
        this.mcpCertitifateDto.setCertificate("INVALID");
        this.mcpDeviceDto.setCertificates(Collections.singletonList(this.mcpCertitifateDto));

        // Mock the MCP Base Service
        mockBackEnd.enqueue(new MockResponse()
                .setBody(this.objectMapper.writeValueAsString(this.mcpDeviceDto))
                .addHeader("Content-Type", "application/json")
                .setResponseCode(HttpStatus.OK.value()));

        // Mock the service secondary calls
        doNothing().when(this.mcpService).checkMcpMirConnectivity();
        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());

        // Init the service
        this.mcpService.init();

        // Perform the service call
        assertTrue(this.mcpService.getMcpEntityCertificates(McpEntityType.SERVICE, this.mcpServiceDto.getMrn(), this.mcpServiceDto.getInstanceVersion()).isEmpty());
    }

    /**
     * Test that we can successfully issue a new X.509 certificate for an MCP
     * device through the MCP MIR by submitting a certificate signature request
     * (CSR) for a given MCP device (based on its MRN).
     */
    @Test
    void testIssueMcpDeviceCertificate() throws McpConnectivityException, UnrecoverableKeyException, CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        // Mock the MCP Base Service
        mockBackEnd.enqueue(new MockResponse()
                .setBody(X509Utils.formatCertificate(cert))
                .addHeader("Location", "http://localhost/certificates/1")
                .addHeader("Content-Type", "text/plain")
                .setResponseCode(HttpStatus.OK.value()));

        // Mock the service secondary calls
        doNothing().when(this.mcpService).checkMcpMirConnectivity();
        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());

        // Init the service
        this.mcpService.init();

        // Perform the service call
        Pair<String, X509Certificate> result = this.mcpService.issueMcpEntityCertificate(McpEntityType.DEVICE, this.mcpDeviceDto.getMrn(), null, this.csr);

        // Make sure the response is correct
        assertNotNull(result);
        assertEquals("1", result.getKey());
        assertEquals(cert, result.getValue());
    }

    /**
     * Test that we can not successfully issue a new X.509 certificate for an MCP
     * device through the MCP MIR by submitting a certificate signature request
     * (CSR), an InvalidRequestException will be thrown.
     */
    @Test
    void testIssueMcpDeviceCertificateFailed() throws McpConnectivityException, UnrecoverableKeyException, CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        // Mock the MCP Base Service
        mockBackEnd.enqueue(new MockResponse()
                .setBody(StringUtils.EMPTY)
                .setResponseCode(HttpStatus.BAD_REQUEST.value()));

        // Mock the service secondary calls
        doNothing().when(this.mcpService).checkMcpMirConnectivity();
        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());

        // Init the service
        this.mcpService.init();

        // Perform the service call
        assertThrows(InvalidRequestException.class, () ->
                this.mcpService.issueMcpEntityCertificate(McpEntityType.DEVICE, this.mcpDeviceDto.getMrn(), null, this.csr)
        );
    }

    /**
     * Test if we cannot successfully issue a new device certificate through the
     * provided CSR, and the response is invalid, an InvalidRequestException
     * will be thrown.
     */
    @Test
    void testIssueMcpDeviceCertificateInvalid() throws McpConnectivityException, UnrecoverableKeyException, CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        // Mock the MCP Base Service
        mockBackEnd.enqueue(new MockResponse()
                .setBody("INVALID")
                .addHeader("Location", "http://localhost/certificates/1")
                .addHeader("Content-Type", "text/plain")
                .setResponseCode(HttpStatus.OK.value()));

        // Mock the service secondary calls
        doNothing().when(this.mcpService).checkMcpMirConnectivity();
        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());

        // Init the service
        this.mcpService.init();

        // Perform the service call
        assertThrows(InvalidRequestException.class, () ->
            this.mcpService.issueMcpEntityCertificate(McpEntityType.DEVICE, this.mcpDeviceDto.getMrn(), null, this.csr)
        );
    }

    /**
     * Test that we can successfully issue a new X.509 certificate for an MCP
     * service through the MCP MIR by submitting a certificate signature request
     * (CSR) for a given MCP device (based on its MRN).
     */
    @Test
    void testIssueMcpServiceCertificate() throws McpConnectivityException, UnrecoverableKeyException, CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        // Add an invalid certificate onto the MCP entity
        this.mcpCertitifateDto.setCertificate("INVALID");
        this.mcpDeviceDto.setCertificates(Collections.singletonList(this.mcpCertitifateDto));

        // Mock the MCP Base Service
        mockBackEnd.enqueue(new MockResponse()
                .setBody(X509Utils.formatCertificate(cert))
                .addHeader("Location", "http://localhost/certificates/1")
                .addHeader("Content-Type", "text/plain")
                .setResponseCode(HttpStatus.OK.value()));

        // Mock the service secondary calls
        doNothing().when(this.mcpService).checkMcpMirConnectivity();
        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());

        // Init the service
        this.mcpService.init();

        // Perform the service call
        Pair<String, X509Certificate> result = this.mcpService.issueMcpEntityCertificate(McpEntityType.SERVICE, this.mcpDeviceDto.getMrn(), null, this.csr);

        // Make sure the response is correct
        assertNotNull(result);
        assertEquals("1", result.getKey());
        assertEquals(cert, result.getValue());
    }

    /**
     * Test that we can not successfully issue a new X.509 certificate for an MCP
     * service through the MCP MIR by submitting a certificate signature request
     * (CSR), an InvalidRequestException will be thrown.
     */
    @Test
    void testIssueMcpServiceCertificateFailed() throws McpConnectivityException, UnrecoverableKeyException, CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        // Mock the MCP Base Service
        mockBackEnd.enqueue(new MockResponse()
                .setBody(StringUtils.EMPTY)
                .setResponseCode(HttpStatus.BAD_REQUEST.value()));

        // Mock the service secondary calls
        doNothing().when(this.mcpService).checkMcpMirConnectivity();
        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());

        // Init the service
        this.mcpService.init();

        // Perform the service call
        assertThrows(InvalidRequestException.class, () ->
                this.mcpService.issueMcpEntityCertificate(McpEntityType.SERVICE, this.mcpDeviceDto.getMrn(), null, this.csr)
        );
    }

    /**
     * Test if we cannot successfully issue a new service certificate through
     * the provided CSR, and the response is invalid, an InvalidRequestException
     * will be thrown.
     */
    @Test
    void testIssueMcpServiceCertificateInvalid() throws McpConnectivityException, UnrecoverableKeyException, CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        // Mock the MCP Base Service
        mockBackEnd.enqueue(new MockResponse()
                .setBody("INVALID")
                .addHeader("Location", "http://localhost/certificates/1")
                .addHeader("Content-Type", "text/plain")
                .setResponseCode(HttpStatus.OK.value()));

        // Mock the service secondary calls
        doNothing().when(this.mcpService).checkMcpMirConnectivity();
        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());

        // Init the service
        this.mcpService.init();

        // Perform the service call
        assertThrows(InvalidRequestException.class, () ->
                this.mcpService.issueMcpEntityCertificate(McpEntityType.SERVICE, this.mcpDeviceDto.getMrn(), null, this.csr)
        );
    }

    /**
     * Test that we can successfully revoke an existing X.509 certificate from
     * an MCP device through the MCP MIR.
     */
    @Test
    void testRevokeMcpDeviceCertificate() throws McpConnectivityException, UnrecoverableKeyException, CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        // Mock the MCP Base Service
        mockBackEnd.enqueue(new MockResponse()
                .setBody(StringUtils.EMPTY)
                .setResponseCode(HttpStatus.OK.value()));

        // Mock the service secondary calls
        doNothing().when(this.mcpService).checkMcpMirConnectivity();
        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());

        // Init the service
        this.mcpService.init();

        // Perform the service call
        this.mcpService.revokeMcpEntityCertificate(McpEntityType.DEVICE, this.mcpDeviceDto.getMrn(), null, "1234567890");
    }

    /**
     * Test that if we fail to revoke an existing device certificate from the
     * MCP MIR for any reason, an InvalidRequestException will be thrown.
     */
    @Test
    void testRevokeMcpDeviceCertificateFailed() throws McpConnectivityException, UnrecoverableKeyException, CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        // Mock the MCP Base Service
        mockBackEnd.enqueue(new MockResponse()
                .setBody(StringUtils.EMPTY)
                .setResponseCode(HttpStatus.NOT_FOUND.value()));

        // Mock the service secondary calls
        doNothing().when(this.mcpService).checkMcpMirConnectivity();
        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());

        // Init the service
        this.mcpService.init();

        // Perform the service call
        assertThrows(InvalidRequestException.class, () ->
            this.mcpService.revokeMcpEntityCertificate(McpEntityType.DEVICE, this.mcpDeviceDto.getMrn(), null, "1234567890")
        );
    }

    /**
     * Test that we can successfully revoke an existing X.509 certificate from
     * an MCP service through the MCP MIR.
     */
    @Test
    void testRevokeMcpServiceCertificate() throws McpConnectivityException, UnrecoverableKeyException, CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        // Mock the MCP Base Service
        mockBackEnd.enqueue(new MockResponse()
                .setBody(StringUtils.EMPTY)
                .setResponseCode(HttpStatus.OK.value()));

        // Mock the service secondary calls
        doNothing().when(this.mcpService).checkMcpMirConnectivity();
        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());

        // Init the service
        this.mcpService.init();

        // Perform the service call
        this.mcpService.revokeMcpEntityCertificate(McpEntityType.SERVICE, this.mcpServiceDto.getMrn(), this.mcpServiceDto.getInstanceVersion(), "1234567890");
    }

    /**
     * Test that if we fail to revoke an existing service certificate from the
     * MCP MIR for any reason, an InvalidRequestException will be thrown.
     */
    @Test
    void testRevokeMcpServiceCertificateFailed() throws McpConnectivityException, UnrecoverableKeyException, CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        // Mock the MCP Base Service
        mockBackEnd.enqueue(new MockResponse()
                .setBody(StringUtils.EMPTY)
                .setResponseCode(HttpStatus.NOT_FOUND.value()));

        // Mock the service secondary calls
        doNothing().when(this.mcpService).checkMcpMirConnectivity();
        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());

        // Init the service
        this.mcpService.init();

        // Perform the service call
        assertThrows(InvalidRequestException.class, () ->
                this.mcpService.revokeMcpEntityCertificate(McpEntityType.SERVICE, this.mcpServiceDto.getMrn(), this.mcpServiceDto.getInstanceVersion(), "1234567890")
        );
    }

    /**
     * Test that we can correctly check the connectivity to the MCP environment.
     */
    @Test
    void testCheckMcpMirConnectivity() throws UnrecoverableKeyException, CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, McpConnectivityException {
        mockBackEnd.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.OK.value()));

        // Init the service
        this.mcpService.init();

        // Perform the service call
        this.mcpService.checkMcpMirConnectivity();
    }

    /**
     * Test that we can correctly detect the disconnections from the MCP
     * environment.
     */
    @Test
    void testCheckMcpMirConnectivityFailed() throws UnrecoverableKeyException, CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        // Mock some data
        this.mockBackEnd.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.NOT_FOUND.value()));

        // Init the service
        this.mcpService.init();

        // Perform the service call
        assertThrows(McpConnectivityException.class, () ->
                this.mcpService.checkMcpMirConnectivity()
        );
    }

}