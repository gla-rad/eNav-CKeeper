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
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.grad.eNav.cKeeper.exceptions.*;
import org.grad.eNav.cKeeper.models.domain.mcp.McpEntityType;
import org.grad.eNav.cKeeper.models.dtos.mcp.McpCertitifateDto;
import org.grad.eNav.cKeeper.models.dtos.mcp.McpDeviceDto;
import org.grad.eNav.cKeeper.models.dtos.mcp.McpServiceDto;
import org.grad.eNav.cKeeper.utils.X509Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
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

    // Test Variables
    private ObjectMapper objectMapper;
    private McpDeviceDto mcpDeviceDto;
    private McpServiceDto mcpServiceDto;
    private KeyPair keypair;
    private PKCS10CertificationRequest csr;
    private X509Certificate cert;
    private McpCertitifateDto mcpCertitifateDto;
    private CloseableHttpClient httpClient;
    private CloseableHttpResponse httpResponse;
    private StatusLine statusLine;
    private Header locationHeader;
    private HttpEntity httpEntity;

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
        this.mcpService.objectMapper = this.objectMapper;

        // Mock an HTTP client returning from the builder
        this.httpClient = mock(CloseableHttpClient.class);

        // Mock an HTTP response
        this.httpResponse = mock(CloseableHttpResponse.class);

        // Mock the HTTP response status line
        this.statusLine = mock(StatusLine.class);

        // Mock the HTTP response header
        this.locationHeader = mock(Header.class);

        // Mock the HTTP entity
        this.httpEntity = mock(HttpEntity.class);
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
//    @Test
//    void testGetMcpDevice() throws IOException, McpConnectivityException {
//        // Don't check for the MCP Identify Registry connectivity
//        doNothing().when(this.mcpService).checkMcpMirConnectivity();
//
//        // Mock the MCP Base Service
//        doAnswer(inv -> "https://host/x509/api/org/grad:urn:mrn:mcp:org:mcc:grad/" + inv.getArgument(0)).when(this.mcpConfigService).constructMcpEndpointUrl(any());
//        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());
//
//        // Mock the HTTP request
//        this.mcpService.clientBuilder = mock(HttpClientBuilder.class);
//        doReturn(this.httpClient).when(this.mcpService.clientBuilder).build();
//
//        // Mock the HTTP response
//        doReturn(HttpStatus.OK.value()).when(this.statusLine).getStatusCode();
//        doReturn(this.statusLine).when(this.httpResponse).getStatusLine();
//        doReturn(IOUtils.toInputStream(this.objectMapper.writeValueAsString(this.mcpDeviceDto))).when(this.httpEntity).getContent();
//        doReturn(this.httpEntity).when(this.httpResponse).getEntity();
//        doReturn(this.httpResponse).when(this.httpClient).execute(any());
//
//        // Perform the service call
//        McpDeviceDto result = this.mcpService.getMcpEntity(this.mcpDeviceDto.getMrn(), null, McpDeviceDto.class);
//
//        // Make sure the response is correct
//        assertNotNull(result);
//        assertEquals(this.mcpDeviceDto, result);
//    }
//
//    /**
//     * Test that if the provided MRN is not valid and the MCP does not return
//     * a valid result, the service will throw a DataNotFoundException.
//     */
//    @Test
//    void testGetMcpDeviceNotFound() throws IOException, McpConnectivityException {
//        // Don't check for the MCP Identify Registry connectivity
//        doNothing().when(this.mcpService).checkMcpMirConnectivity();
//
//        // Mock the MCP Base Service
//        doAnswer(inv -> "https://host/x509/api/org/grad:urn:mrn:mcp:org:mcc:grad/" + inv.getArgument(0)).when(this.mcpConfigService).constructMcpEndpointUrl(any());
//        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());
//
//        // Mock the HTTP request
//        this.mcpService.clientBuilder = mock(HttpClientBuilder.class);
//        doReturn(this.httpClient).when(this.mcpService.clientBuilder).build();
//
//        // Mock the HTTP response
//        doReturn(HttpStatus.NOT_FOUND.value()).when(this.statusLine).getStatusCode();
//        doReturn(this.statusLine).when(this.httpResponse).getStatusLine();
//        doReturn(this.httpResponse).when(this.httpClient).execute(any());
//
//        // Perform the service call
//        assertThrows(DataNotFoundException.class, () ->
//            this.mcpService.getMcpEntity(this.mcpDeviceDto.getMrn(), null, McpDeviceDto.class)
//        );
//    }
//
//    /**
//     * Test that we can successfully create a new MCP device based on the
//     * provided object. That means that both name and MRN fields are required
//     * to be populated.
//     */
//    @Test
//    void testCreateMcpDevice() throws IOException, McpConnectivityException {
//        // Don't check for the MCP Identify Registry connectivity
//        doNothing().when(this.mcpService).checkMcpMirConnectivity();
//
//        // Mock the MCP Base Service
//        doAnswer(inv -> "https://host/x509/api/org/grad:urn:mrn:mcp:org:mcc:grad/" + inv.getArgument(0)).when(this.mcpConfigService).constructMcpEndpointUrl(any());
//        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());
//
//        // Mock the HTTP request
//        this.mcpService.clientBuilder = mock(HttpClientBuilder.class);
//        doReturn(this.httpClient).when(this.mcpService.clientBuilder).build();
//
//        // Mock the HTTP response
//        doReturn(HttpStatus.CREATED.value()).when(this.statusLine).getStatusCode();
//        doReturn(this.statusLine).when(this.httpResponse).getStatusLine();
//        doReturn(IOUtils.toInputStream(this.objectMapper.writeValueAsString(this.mcpDeviceDto))).when(this.httpEntity).getContent();
//        doReturn(this.httpEntity).when(this.httpResponse).getEntity();
//        doReturn(this.httpResponse).when(this.httpClient).execute(any());
//
//        // Perform the service call
//        McpDeviceDto result = this.mcpService.createMcpEntity(this.mcpDeviceDto);
//
//        // Make sure the response is correct
//        assertNotNull(result);
//        assertEquals(this.mcpDeviceDto.getId(), result.getId());
//        assertEquals(this.mcpDeviceDto.getCreatedAt(), result.getCreatedAt());
//        assertEquals(this.mcpDeviceDto.getUpdatedAt(), result.getUpdatedAt());
//        assertEquals(this.mcpDeviceDto.getName(), result.getName());
//        assertEquals(this.mcpDeviceDto.getMrn(), result.getMrn());
//        assertEquals(this.mcpDeviceDto.getIdOrganization(), result.getIdOrganization());
//    }
//
//    /**
//     * Test that if we cannot successfully create a new MCP device based on the
//     * provided object, the service will throw a SavingFailedException.
//     */
//    @Test
//    void testCreateMcpDeviceFailure() throws IOException, McpConnectivityException {
//        // Don't check for the MCP Identify Registry connectivity
//        doNothing().when(this.mcpService).checkMcpMirConnectivity();
//
//        // Mock the MCP Base Service
//        doAnswer(inv -> "https://host/x509/api/org/grad:urn:mrn:mcp:device:mcc:grad/" + inv.getArgument(0)).when(this.mcpConfigService).constructMcpEndpointUrl(any());
//        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());
//
//        // Mock the HTTP request
//        this.mcpService.clientBuilder = mock(HttpClientBuilder.class);
//        doReturn(this.httpClient).when(this.mcpService.clientBuilder).build();
//
//        // Mock the HTTP response
//        doReturn(HttpStatus.BAD_REQUEST.value()).when(this.statusLine).getStatusCode();
//        doReturn(this.statusLine).when(this.httpResponse).getStatusLine();
//        doReturn(this.httpResponse).when(this.httpClient).execute(any());
//
//        // Perform the service call
//        assertThrows(SavingFailedException.class, () ->
//                this.mcpService.createMcpEntity(this.mcpDeviceDto)
//        );
//    }
//
//    /**
//     * Test that we can successfully update an existing MCP device based on the
//     * provided MRN and object. That means that both name and MRN fields are
//     * required to be populated.
//     */
//    @Test
//    void testUpdateMcpDevice() throws IOException, McpConnectivityException {
//        // Don't check for the MCP Identify Registry connectivity
//        doNothing().when(this.mcpService).checkMcpMirConnectivity();
//
//        // Mock the MCP Base Service
//        doAnswer(inv -> "https://host/x509/api/org/grad:urn:mrn:mcp:device:mcc:grad/" + inv.getArgument(0)).when(this.mcpConfigService).constructMcpEndpointUrl(any());
//        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());
//
//        // Mock the HTTP request
//        this.mcpService.clientBuilder = mock(HttpClientBuilder.class);
//        doReturn(this.httpClient).when(this.mcpService.clientBuilder).build();
//
//        // Mock the HTTP response
//        doReturn(HttpStatus.OK.value()).when(this.statusLine).getStatusCode();
//        doReturn(this.statusLine).when(this.httpResponse).getStatusLine();
//        doReturn(IOUtils.toInputStream(this.objectMapper.writeValueAsString(this.mcpDeviceDto))).when(this.httpEntity).getContent();
//        doReturn(this.httpEntity).when(this.httpResponse).getEntity();
//        doReturn(this.httpResponse).when(this.httpClient).execute(any());
//
//        // Perform the service call
//        McpDeviceDto result = this.mcpService.updateMcpEntity(this.mcpDeviceDto.getMrn(), this.mcpDeviceDto);
//
//        // Make sure the response is correct
//        assertNotNull(result);
//        assertEquals(this.mcpDeviceDto, result);
//    }
//
//    /**
//     * Test that if we cannot successfully update an existing MCP device based
//     * on the provided MRN and object, the service will throw a
//     * SavingFailedException.
//     */
//    @Test
//    void testUpdateMcpDeviceFailure() throws IOException, McpConnectivityException {
//        // Don't check for the MCP Identify Registry connectivity
//        doNothing().when(this.mcpService).checkMcpMirConnectivity();
//
//        // Mock the MCP Base Service
//        doAnswer(inv -> "https://host/x509/api/org/grad:urn:mrn:mcp:device:mcc:grad/" + inv.getArgument(0)).when(this.mcpConfigService).constructMcpEndpointUrl(any());
//        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());
//
//        // Mock the HTTP request
//        this.mcpService.clientBuilder = mock(HttpClientBuilder.class);
//        doReturn(this.httpClient).when(this.mcpService.clientBuilder).build();
//
//        // Mock the HTTP response
//        doReturn(HttpStatus.BAD_REQUEST.value()).when(this.statusLine).getStatusCode();
//        doReturn(this.statusLine).when(this.httpResponse).getStatusLine();
//        doReturn(this.httpResponse).when(this.httpClient).execute(any());
//
//        // Perform the service call
//        assertThrows(SavingFailedException.class, () ->
//                this.mcpService.updateMcpEntity(this.mcpDeviceDto.getMrn(), this.mcpDeviceDto)
//        );
//    }
//
//    /**
//     * Test that we can successfully delete an existing MCP device based on the
//     * provided MRN.
//     */
//    @Test
//    void testDeleteMcpDevice() throws IOException, McpConnectivityException {
//        // Don't check for the MCP Identify Registry connectivity
//        doNothing().when(this.mcpService).checkMcpMirConnectivity();
//
//        // Mock the MCP Base Service
//        doAnswer(inv -> "https://host/x509/api/org/grad:urn:mrn:mcp:device:mcc:grad/" + inv.getArgument(0)).when(this.mcpConfigService).constructMcpEndpointUrl(any());
//        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());
//
//        // Mock the HTTP request
//        this.mcpService.clientBuilder = mock(HttpClientBuilder.class);
//        doReturn(this.httpClient).when(this.mcpService.clientBuilder).build();
//
//        // Mock the HTTP response
//        doReturn(HttpStatus.OK.value()).when(this.statusLine).getStatusCode();
//        doReturn(this.statusLine).when(this.httpResponse).getStatusLine();
//        doReturn(this.httpResponse).when(this.httpClient).execute(any());
//
//        // Perform the service call
//        boolean result = this.mcpService.deleteMcpEntity(this.mcpDeviceDto.getMrn(), null, McpDeviceDto.class);
//
//        // Make sure the response is correct
//        assertTrue(result);
//    }
//
//    /**
//     * Test that if we cannot successfully delete an existing MCP device based
//     * on the provided MRN, the service will throw a  DeletingFailedException.
//     */
//    @Test
//    void testDeleteMcpDeviceFailure() throws IOException, McpConnectivityException {
//        // Don't check for the MCP Identify Registry connectivity
//        doNothing().when(this.mcpService).checkMcpMirConnectivity();
//
//        // Mock the MCP Base Service
//        doAnswer(inv -> "https://host/x509/api/org/grad:urn:mrn:mcp:device:mcc:grad/" + inv.getArgument(0)).when(this.mcpConfigService).constructMcpEndpointUrl(any());
//        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());
//
//        // Mock the HTTP request
//        this.mcpService.clientBuilder = mock(HttpClientBuilder.class);
//        doReturn(this.httpClient).when(this.mcpService.clientBuilder).build();
//
//        // Mock the HTTP response
//        doReturn(HttpStatus.BAD_REQUEST.value()).when(this.statusLine).getStatusCode();
//        doReturn(this.statusLine).when(this.httpResponse).getStatusLine();
//        doReturn(this.httpResponse).when(this.httpClient).execute(any());
//
//        // Perform the service call
//        assertThrows(DeletingFailedException.class, () ->
//                this.mcpService.deleteMcpEntity(this.mcpDeviceDto.getMrn(),null, McpDeviceDto.class)
//        );
//    }
//
//    /**
//     * Test that we can correctly retrieve and translate the X509 certificates
//     * for an MCP device from the MCP Identity Registry.
//     */
//    @Test
//    void testGetMcpDeviceCertificates() throws IOException, McpConnectivityException, CertificateException {
//        // Don't check for the MCP Identify Registry connectivity
//        doNothing().when(this.mcpService).checkMcpMirConnectivity();
//
//        // Mock the MCP Base Service
//        doAnswer(inv -> "https://host/x509/api/org/grad:urn:mrn:mcp:device:mcc:grad/" + inv.getArgument(0)).when(this.mcpConfigService).constructMcpEndpointUrl(any());
//        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());
//
//        // Initialise the service certificate factory
//        this.mcpService.certificateFactory = CertificateFactory.getInstance("X.509");
//
//        // Mock the HTTP request
//        this.mcpService.clientBuilder = mock(HttpClientBuilder.class);
//        doReturn(this.httpClient).when(this.mcpService.clientBuilder).build();
//
//        // Mock the HTTP response
//        doReturn(HttpStatus.OK.value()).when(this.statusLine).getStatusCode();
//        doReturn(this.statusLine).when(this.httpResponse).getStatusLine();
//        doReturn(IOUtils.toInputStream(this.objectMapper.writeValueAsString(Collections.singletonMap("certificates", Collections.singleton(this.mcpCertitifateDto))))).when(this.httpEntity).getContent();
//        doReturn(this.httpEntity).when(this.httpResponse).getEntity();
//        doReturn(this.httpResponse).when(this.httpClient).execute(any());
//
//        // Perform the service call
//        Map<String, X509Certificate> certificates = this.mcpService.getMcpEntityCertificates(McpEntityType.DEVICE, this.mcpDeviceDto.getMrn(), null);
//
//        // Make sure the result looks OK
//        assertNotNull(certificates);
//        assertEquals(1, certificates.size());
//
//        // Assert the retrieved certificates
//        assertTrue(certificates.containsKey(this.mcpCertitifateDto.getSerialNumber()));
//        assertEquals(this.mcpCertitifateDto.getStart(), certificates.get(this.mcpCertitifateDto.getSerialNumber()).getNotBefore());
//        assertEquals(this.mcpCertitifateDto.getEnd(), certificates.get(this.mcpCertitifateDto.getSerialNumber()).getNotAfter());
//        assertEquals(this.mcpCertitifateDto.getCertificate(), X509Utils.formatCertificate(certificates.get(this.mcpCertitifateDto.getSerialNumber())));
//    }
//
//    /**
//     * Test if we cannot successfully retrieve and translate the certificates
//     * for an MCP device from the MCP Identity Registry, and the response is
//     * invalid, the certificate will be omitted.
//     */
//    @Test
//    void testGetMcpDeviceCertificatesFailure() throws IOException, McpConnectivityException, CertificateException {
//        // Don't check for the MCP Identify Registry connectivity
//        doNothing().when(this.mcpService).checkMcpMirConnectivity();
//
//        // Mock the MCP Base Service
//        doAnswer(inv -> "https://host/x509/api/org/grad:urn:mrn:mcp:device:mcc:grad/" + inv.getArgument(0)).when(this.mcpConfigService).constructMcpEndpointUrl(any());
//        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());
//
//        // Initialise the service certificate factory
//        this.mcpService.certificateFactory = CertificateFactory.getInstance("X.509");
//
//        // Mock the HTTP request
//        this.mcpService.clientBuilder = mock(HttpClientBuilder.class);
//        doReturn(this.httpClient).when(this.mcpService.clientBuilder).build();
//
//        // Mess up a bit the certificate test
//        this.mcpCertitifateDto.setCertificate("INVALID");
//
//        // Mock the HTTP response
//        doReturn(HttpStatus.OK.value()).when(this.statusLine).getStatusCode();
//        doReturn(this.statusLine).when(this.httpResponse).getStatusLine();
//        doReturn(IOUtils.toInputStream(this.objectMapper.writeValueAsString(Collections.singletonMap("certificates", Collections.singleton(this.mcpCertitifateDto))))).when(this.httpEntity).getContent();
//        doReturn(this.httpEntity).when(this.httpResponse).getEntity();
//        doReturn(this.httpResponse).when(this.httpClient).execute(any());
//
//        // Perform the service call
//        assertTrue(this.mcpService.getMcpEntityCertificates(McpEntityType.DEVICE, this.mcpDeviceDto.getMrn(), null).isEmpty());
//    }
//
//    /**
//     * Test that we can correctly retrieve and translate the X509 certificates
//     * for an MCP service from the MCP Identity Registry.
//     */
//    @Test
//    void testGetMcpServiceCertificates() throws IOException, McpConnectivityException, CertificateException {
//        // Don't check for the MCP Identify Registry connectivity
//        doNothing().when(this.mcpService).checkMcpMirConnectivity();
//
//        // Mock the MCP Base Service
//        doAnswer(inv -> "https://host/x509/api/org/grad:urn:mrn:mcp:service:mcc:instance:grad//" + inv.getArgument(0)).when(this.mcpConfigService).constructMcpEndpointUrl(any());
//        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());
//
//        // Initialise the service certificate factory
//        this.mcpService.certificateFactory = CertificateFactory.getInstance("X.509");
//
//        // Mock the HTTP request
//        this.mcpService.clientBuilder = mock(HttpClientBuilder.class);
//        doReturn(this.httpClient).when(this.mcpService.clientBuilder).build();
//
//        // Mock the HTTP response
//        doReturn(HttpStatus.OK.value()).when(this.statusLine).getStatusCode();
//        doReturn(this.statusLine).when(this.httpResponse).getStatusLine();
//        doReturn(IOUtils.toInputStream(this.objectMapper.writeValueAsString(Collections.singletonMap("certificates", Collections.singleton(this.mcpCertitifateDto))))).when(this.httpEntity).getContent();
//        doReturn(this.httpEntity).when(this.httpResponse).getEntity();
//        doReturn(this.httpResponse).when(this.httpClient).execute(any());
//
//        // Perform the service call
//        Map<String, X509Certificate> certificates = this.mcpService.getMcpEntityCertificates(McpEntityType.SERVICE, this.mcpServiceDto.getMrn(), this.mcpServiceDto.getInstanceVersion());
//
//        // Make sure the result looks OK
//        assertNotNull(certificates);
//        assertEquals(1, certificates.size());
//
//        // Assert the retrieved certificates
//        assertTrue(certificates.containsKey(this.mcpCertitifateDto.getSerialNumber()));
//        assertEquals(this.mcpCertitifateDto.getStart(), certificates.get(this.mcpCertitifateDto.getSerialNumber()).getNotBefore());
//        assertEquals(this.mcpCertitifateDto.getEnd(), certificates.get(this.mcpCertitifateDto.getSerialNumber()).getNotAfter());
//        assertEquals(this.mcpCertitifateDto.getCertificate(), X509Utils.formatCertificate(certificates.get(this.mcpCertitifateDto.getSerialNumber())));
//    }
//
//    /**
//     * Test if we cannot successfully retrieve and translate the certificates
//     * for an MCP service from the MCP Identity Registry, and the response is
//     * invalid, the certificate will be omitted.
//     */
//    @Test
//    void testGetMcpServiceCertificatesFailure() throws IOException, McpConnectivityException, CertificateException {
//        // Don't check for the MCP Identify Registry connectivity
//        doNothing().when(this.mcpService).checkMcpMirConnectivity();
//
//        // Mock the MCP Base Service
//        doAnswer(inv -> "https://host/x509/api/org/grad:urn:mrn:mcp:service:mcc:instance:grad/" + inv.getArgument(0)).when(this.mcpConfigService).constructMcpEndpointUrl(any());
//        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());
//
//        // Initialise the service certificate factory
//        this.mcpService.certificateFactory = CertificateFactory.getInstance("X.509");
//
//        // Mock the HTTP request
//        this.mcpService.clientBuilder = mock(HttpClientBuilder.class);
//        doReturn(this.httpClient).when(this.mcpService.clientBuilder).build();
//
//        // Mess up a bit the certificate test
//        this.mcpCertitifateDto.setCertificate("INVALID");
//
//        // Mock the HTTP response
//        doReturn(HttpStatus.OK.value()).when(this.statusLine).getStatusCode();
//        doReturn(this.statusLine).when(this.httpResponse).getStatusLine();
//        doReturn(IOUtils.toInputStream(this.objectMapper.writeValueAsString(Collections.singletonMap("certificates", Collections.singleton(this.mcpCertitifateDto))))).when(this.httpEntity).getContent();
//        doReturn(this.httpEntity).when(this.httpResponse).getEntity();
//        doReturn(this.httpResponse).when(this.httpClient).execute(any());
//
//        // Perform the service call
//        assertTrue(this.mcpService.getMcpEntityCertificates(McpEntityType.SERVICE, this.mcpServiceDto.getMrn(), this.mcpServiceDto.getInstanceVersion()).isEmpty());
//    }
//
//    /**
//     * Test that we can successfully issue a new X.509 certificate for an MCP
//     * device through the MCP MIR by submitting a certificate signature request
//     * (CSR) for a given MCP device (based on its MRN).
//     */
//    @Test
//    void testIssueMcpDeviceCertificate() throws IOException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, OperatorCreationException, CertificateException, McpConnectivityException {
//        // Don't check for the MCP Identify Registry connectivity
//        doNothing().when(this.mcpService).checkMcpMirConnectivity();
//
//        // Mock the MCP Base Service
//        doAnswer(inv -> "https://host/x509/api/org/grad:urn:mrn:mcp:device:mcc:grad/" + inv.getArgument(0)).when(this.mcpConfigService).constructMcpEndpointUrl(any());
//        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());
//
//        // Initialise the service certificate factory
//        this.mcpService.certificateFactory = CertificateFactory.getInstance("X.509");
//
//        // Mock the HTTP request
//        this.mcpService.clientBuilder = mock(HttpClientBuilder.class);
//        doReturn(this.httpClient).when(this.mcpService.clientBuilder).build();
//
//        // Mock the HTTP response
//        doReturn(HttpStatus.CREATED.value()).when(this.statusLine).getStatusCode();
//        doReturn(this.statusLine).when(this.httpResponse).getStatusLine();
//        doReturn("https://api-x509.maritimeconnectivity.net/x509/api/org/urn:mrn:mcp:org:mcc:grad/device/urn:mrn:mcp:device:mcc:grad:test/certificate/1234567890").when(locationHeader).getValue();
//        doReturn(locationHeader).when(this.httpResponse).getFirstHeader("Location");
//        doReturn(IOUtils.toInputStream(X509Utils.formatCertificate(cert))).when(this.httpEntity).getContent();
//        doReturn(this.httpEntity).when(this.httpResponse).getEntity();
//        doReturn(this.httpResponse).when(this.httpClient).execute(any());
//
//        // Perform the service call
//        this.mcpService.issueMcpEntityCertificate(McpEntityType.DEVICE, this.mcpDeviceDto.getMrn(), null, this.csr);
//    }
//
//    /**
//     * Test if we cannot successfully issue a new device certificate through the
//     * provided CSR, and the response is invalid, an InvalidRequestException
//     * will be thrown.
//     */
//    @Test
//    void testIssueMcpDeviceCertificateFailure() throws IOException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, OperatorCreationException, CertificateException, McpConnectivityException {
//        // Don't check for the MCP Identify Registry connectivity
//        doNothing().when(this.mcpService).checkMcpMirConnectivity();
//
//        // Mock the MCP Base Service
//        doAnswer(inv -> "https://host/x509/api/org/grad:urn:mrn:mcp:device:mcc:grad/" + inv.getArgument(0)).when(this.mcpConfigService).constructMcpEndpointUrl(any());
//        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());
//
//        // Initialise the service certificate factory
//        this.mcpService.certificateFactory = CertificateFactory.getInstance("X.509");
//
//        // Mock the HTTP request
//        this.mcpService.clientBuilder = mock(HttpClientBuilder.class);
//        doReturn(this.httpClient).when(this.mcpService.clientBuilder).build();
//
//        // Mock the HTTP response
//        doReturn(HttpStatus.BAD_REQUEST.value()).when(this.statusLine).getStatusCode();
//        doReturn(this.statusLine).when(this.httpResponse).getStatusLine();
//        doReturn(this.httpResponse).when(this.httpClient).execute(any());
//
//        // Perform the service call
//        assertThrows(InvalidRequestException.class, () ->
//            this.mcpService.issueMcpEntityCertificate(McpEntityType.DEVICE, this.mcpDeviceDto.getMrn(), null, this.csr)
//        );
//    }
//
//    /**
//     * Test that we can successfully issue a new X.509 certificate for an MCP
//     * service through the MCP MIR by submitting a certificate signature request
//     * (CSR) for a given MCP device (based on its MRN).
//     */
//    @Test
//    void testIssueMcpServiceCertificate() throws IOException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, OperatorCreationException, CertificateException, McpConnectivityException {
//        // Don't check for the MCP Identify Registry connectivity
//        doNothing().when(this.mcpService).checkMcpMirConnectivity();
//
//        // Mock the MCP Base Service
//        doAnswer(inv -> "https://host/x509/api/org/grad:urn:mrn:mcp:service:mcc:instance:grad/" + inv.getArgument(0)).when(this.mcpConfigService).constructMcpEndpointUrl(any());
//        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());
//
//        // Initialise the service certificate factory
//        this.mcpService.certificateFactory = CertificateFactory.getInstance("X.509");
//
//        // Mock the HTTP request
//        this.mcpService.clientBuilder = mock(HttpClientBuilder.class);
//        doReturn(this.httpClient).when(this.mcpService.clientBuilder).build();
//
//        // Mock the HTTP response
//        doReturn(HttpStatus.CREATED.value()).when(this.statusLine).getStatusCode();
//        doReturn(this.statusLine).when(this.httpResponse).getStatusLine();
//        doReturn("https://api-x509.maritimeconnectivity.net/x509/api/org/urn:mrn:mcp:org:mcc:grad/service/urn:mrn:mcp:service:mcc:grad:instance:test/0.0.1/certificate/1234567890").when(locationHeader).getValue();
//        doReturn(locationHeader).when(this.httpResponse).getFirstHeader("Location");
//        doReturn(IOUtils.toInputStream(X509Utils.formatCertificate(cert))).when(this.httpEntity).getContent();
//        doReturn(this.httpEntity).when(this.httpResponse).getEntity();
//        doReturn(this.httpResponse).when(this.httpClient).execute(any());
//
//        // Perform the service call
//        this.mcpService.issueMcpEntityCertificate(McpEntityType.SERVICE, this.mcpServiceDto.getMrn(), this.mcpServiceDto.getInstanceVersion(), this.csr);
//    }
//
//    /**
//     * Test if we cannot successfully issue a new service certificate through
//     * the provided CSR, and the response is invalid, an InvalidRequestException
//     * will be thrown.
//     */
//    @Test
//    void testIssueMcpServiceCertificateFailure() throws IOException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, OperatorCreationException, CertificateException, McpConnectivityException {
//        // Don't check for the MCP Identify Registry connectivity
//        doNothing().when(this.mcpService).checkMcpMirConnectivity();
//
//        // Mock the MCP Base Service
//        doAnswer(inv -> "https://host/x509/api/grad:urn:mrn:mcp:service:mcc:instance:grad/" + inv.getArgument(0)).when(this.mcpConfigService).constructMcpEndpointUrl(any());
//        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());
//
//        // Initialise the service certificate factory
//        this.mcpService.certificateFactory = CertificateFactory.getInstance("X.509");
//
//        // Mock the HTTP request
//        this.mcpService.clientBuilder = mock(HttpClientBuilder.class);
//        doReturn(this.httpClient).when(this.mcpService.clientBuilder).build();
//
//        // Mock the HTTP response
//        doReturn(HttpStatus.BAD_REQUEST.value()).when(this.statusLine).getStatusCode();
//        doReturn(this.statusLine).when(this.httpResponse).getStatusLine();
//        doReturn(this.httpResponse).when(this.httpClient).execute(any());
//
//        // Perform the service call
//        assertThrows(InvalidRequestException.class, () ->
//                this.mcpService.issueMcpEntityCertificate(McpEntityType.SERVICE, this.mcpServiceDto.getMrn(), this.mcpServiceDto.getInstanceVersion(), this.csr)
//        );
//    }
//
//    /**
//     * Test that we can successfully revoke an existing X.509 certificate from
//     * an MCP device through the MCP MIR.
//     */
//    @Test
//    void testRevokeMcpDeviceCertificate() throws IOException, McpConnectivityException {
//        // Don't check for the MCP Identify Registry connectivity
//        doNothing().when(this.mcpService).checkMcpMirConnectivity();
//
//        // Mock the MCP Base Service
//        doAnswer(inv -> "https://host/x509/api/org/grad:urn:mrn:mcp:device:mcc:grad/" + inv.getArgument(0)).when(this.mcpConfigService).constructMcpEndpointUrl(any());
//        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());
//
//        // Mock the HTTP request
//        this.mcpService.clientBuilder = mock(HttpClientBuilder.class);
//        doReturn(this.httpClient).when(this.mcpService.clientBuilder).build();
//
//        // Mock the HTTP response
//        doReturn(HttpStatus.OK.value()).when(this.statusLine).getStatusCode();
//        doReturn(this.statusLine).when(this.httpResponse).getStatusLine();
//        doReturn(this.httpResponse).when(this.httpClient).execute(any());
//
//        // Perform the service call
//        this.mcpService.revokeMcpEntityCertificate(McpEntityType.DEVICE, this.mcpDeviceDto.getMrn(), null, "1234567890");
//    }
//
//    /**
//     * Test that if we fail to revoke an existing device certificate from the
//     * MCP MIR for any reason, an InvalidRequestException will be thrown.
//     */
//    @Test
//    void testRevokeMcpDeviceCertificateFailure() throws IOException, McpConnectivityException {
//        // Don't check for the MCP Identify Registry connectivity
//        doNothing().when(this.mcpService).checkMcpMirConnectivity();
//
//        // Mock the MCP Base Service
//        doAnswer(inv -> "https://host/x509/api/org/grad:urn:mrn:mcp:device:mcc:grad/" + inv.getArgument(0)).when(this.mcpConfigService).constructMcpEndpointUrl(any());
//        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());
//
//        // Mock the HTTP request
//        this.mcpService.clientBuilder = mock(HttpClientBuilder.class);
//        doReturn(this.httpClient).when(this.mcpService.clientBuilder).build();
//
//        // Mock the HTTP response
//        doReturn(HttpStatus.BAD_REQUEST.value()).when(this.statusLine).getStatusCode();
//        doReturn(this.statusLine).when(this.httpResponse).getStatusLine();
//        doReturn(this.httpResponse).when(this.httpClient).execute(any());
//
//        // Perform the service call
//        assertThrows(InvalidRequestException.class, () ->
//            this.mcpService.revokeMcpEntityCertificate(McpEntityType.DEVICE, this.mcpDeviceDto.getMrn(), null, "1234567890")
//        );
//    }
//
//    /**
//     * Test that we can successfully revoke an existing X.509 certificate from
//     * an MCP service through the MCP MIR.
//     */
//    @Test
//    void testRevokeMcpServiceCertificate() throws IOException, McpConnectivityException {
//        // Don't check for the MCP Identify Registry connectivity
//        doNothing().when(this.mcpService).checkMcpMirConnectivity();
//
//        // Mock the MCP Base Service
//        doAnswer(inv -> "https://host/x509/api/org/grad:urn:mrn:mcp:device:mcc:grad/" + inv.getArgument(0)).when(this.mcpConfigService).constructMcpEndpointUrl(any());
//        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());
//
//        // Mock the HTTP request
//        this.mcpService.clientBuilder = mock(HttpClientBuilder.class);
//        doReturn(this.httpClient).when(this.mcpService.clientBuilder).build();
//
//        // Mock the HTTP response
//        doReturn(HttpStatus.OK.value()).when(this.statusLine).getStatusCode();
//        doReturn(this.statusLine).when(this.httpResponse).getStatusLine();
//        doReturn(this.httpResponse).when(this.httpClient).execute(any());
//
//        // Perform the service call
//        this.mcpService.revokeMcpEntityCertificate(McpEntityType.SERVICE, this.mcpServiceDto.getMrn(), this.mcpServiceDto.getInstanceVersion(), "1234567890");
//    }
//
//    /**
//     * Test that if we fail to revoke an existing service certificate from the
//     * MCP MIR for any reason, an InvalidRequestException will be thrown.
//     */
//    @Test
//    void testRevokeMcpServiceCertificateFailure() throws IOException, McpConnectivityException {
//        // Don't check for the MCP Identify Registry connectivity
//        doNothing().when(this.mcpService).checkMcpMirConnectivity();
//
//        // Mock the MCP Base Service
//        doAnswer(inv -> "https://host/x509/api/org/grad:urn:mrn:mcp:service:mcc:grad:instance/" + inv.getArgument(0)).when(this.mcpConfigService).constructMcpEndpointUrl(any());
//        doAnswer(inv -> inv.getArgument(1)).when(this.mcpConfigService).constructMcpEntityMrn(any(), any());
//
//        // Mock the HTTP request
//        this.mcpService.clientBuilder = mock(HttpClientBuilder.class);
//        doReturn(this.httpClient).when(this.mcpService.clientBuilder).build();
//
//        // Mock the HTTP response
//        doReturn(HttpStatus.BAD_REQUEST.value()).when(this.statusLine).getStatusCode();
//        doReturn(this.statusLine).when(this.httpResponse).getStatusLine();
//        doReturn(this.httpResponse).when(this.httpClient).execute(any());
//
//        // Perform the service call
//        assertThrows(InvalidRequestException.class, () ->
//                this.mcpService.revokeMcpEntityCertificate(McpEntityType.SERVICE, this.mcpServiceDto.getMrn(), this.mcpServiceDto.getInstanceVersion(), "1234567890")
//        );
//    }

//    /**
//     * Test that we can correctly check the connectivity to the MCP environment.
//     */
//    @Test
//    void testCheckMcpMirConnectivity() throws McpConnectivityException {
//        // Mock some data
//        this.mcpService.restTemplate = mock(RestTemplate.class);
//        MultiValueMap<String, String> params= new LinkedMultiValueMap<>();
//        params.add("test", "test");
//
//        // Mock the MCP Base Service
//        doAnswer(inv -> "https://host/x509/api/org/grad:urn:mrn:mcp:device:mcc:grad/" + inv.getArgument(0)).when(this.mcpConfigService).constructMcpEndpointUrl(any());
//
//        // Mock the Springboot REST template that checks the connectivity
//        doReturn(new HttpHeaders(params)).when(this.mcpService.restTemplate).headForHeaders(anyString());
//
//        // Perform the service call
//        this.mcpService.checkMcpMirConnectivity();
//    }

//    /**
//     * Test that we can correctly detect the disconnections from the MCP
//     * environment.
//     */
//    @Test
//    void testCheckMcpMirConnectivityFailed() {
//        // Mock some data
//        this.mcpService.restTemplate = mock(RestTemplate.class);
//
//        // Mock the MCP Base Service
//        doAnswer(inv -> "https://host/x509/api/org/grad:urn:mrn:mcp:device:mcc:grad/" + inv.getArgument(0)).when(this.mcpConfigService).constructMcpEndpointUrl(any());
//
//        // Mock the Springboot REST template that checks the connectivity
//        doReturn(null).when(this.mcpService.restTemplate).headForHeaders(anyString());
//
//        // Perform the service call
//        assertThrows(McpConnectivityException.class, () ->
//                this.mcpService.checkMcpMirConnectivity()
//        );
//    }

}