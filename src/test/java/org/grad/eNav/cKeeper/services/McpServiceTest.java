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
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.grad.eNav.cKeeper.exceptions.DataNotFoundException;
import org.grad.eNav.cKeeper.exceptions.DeletingFailedException;
import org.grad.eNav.cKeeper.exceptions.InvalidRequestException;
import org.grad.eNav.cKeeper.exceptions.SavingFailedException;
import org.grad.eNav.cKeeper.models.dtos.McpDeviceDto;
import org.grad.eNav.cKeeper.utils.X509Utils;
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
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class McpServiceTest {

    /**
     * The Tested Service.
     */
    @InjectMocks
    @Spy
    McpService mcpService;

    /**
     * The MRN Entity Service mock.
     */
    @Mock
    MrnEntityService mrnEntityService;

    // Test Variables
    private ObjectMapper objectMapper;
    private McpDeviceDto mcpDevice;
    private CloseableHttpClient httpClient;
    private CloseableHttpResponse httpResponse;
    private StatusLine statusLine;
    private HttpEntity httpEntity;

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setUp() {
        // Setup an MCP device for testing
        this.mcpDevice = new McpDeviceDto("Test","urn:mrn:mcp:device:mcc:grad:test");

        // Add a real object mapper to the service
        this.objectMapper = new ObjectMapper();
        this.mcpService.objectMapper = this.objectMapper;

        // Initialise the absolutely necessary parameters
        this.mcpService.mcpDevicePrefix = "urn:mrn:mcp:device:mcc:";

        // Mock an HTTP client returning from the builder
        this.httpClient = mock(CloseableHttpClient.class);

        // Mock an HTTP response
        this.httpResponse = mock(CloseableHttpResponse.class);

        // Mock the HTTP response status line
        this.statusLine = mock(StatusLine.class);

        // Mock the HTTP entity
        this.httpEntity = mock(HttpEntity.class);
    }

    /**
     * Test that the MCP service can initialise correctly and read the MCP
     * X.509 certificate for accessing the MIR functionality.
     */
    @Test
    void testInit() throws UnrecoverableKeyException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        this.mcpService.keyStore = "cKeeperKeystore";
        this.mcpService.keyStorePass = "password";
        this.mcpService.init();

        // Make sure we've got an SSL Context
        assertNotNull(this.mcpService.sslContext);
    }

    /**
     * Test that we can retrieve a specific MCP device based on the provided
     * MRN number. Note that the last MRN section (device ID) can also be
     * provided since the previous MRN sections are pretty standard, based
     * on your organisation.
     */
    @Test
    void testGetMcpDevice() throws IOException {
        // Mock the HTTP request
        this.mcpService.clientBuilder = mock(HttpClientBuilder.class);
        doReturn(this.httpClient).when(this.mcpService.clientBuilder).build();

        // Mock the HTTP response
        doReturn(HttpStatus.OK.value()).when(this.statusLine).getStatusCode();
        doReturn(this.statusLine).when(this.httpResponse).getStatusLine();
        doReturn(IOUtils.toInputStream(this.objectMapper.writeValueAsString(this.mcpDevice))).when(this.httpEntity).getContent();
        doReturn(this.httpEntity).when(this.httpResponse).getEntity();
        doReturn(this.httpResponse).when(this.httpClient).execute(any());

        // Perform the service call
        McpDeviceDto result = this.mcpService.getMcpDevice(this.mcpDevice.getMrn());

        // Make sure the response is correct
        assertNotNull(result);
        assertEquals(this.mcpDevice, result);
    }

    /**
     * Test that if the provided MRN is not valid and the MCP does not return
     * a valid result, the service will throw a DataNotFoundException.
     */
    @Test
    void testGetMcpDeviceNotFound() throws IOException {
        // Mock the HTTP request
        this.mcpService.clientBuilder = mock(HttpClientBuilder.class);
        doReturn(this.httpClient).when(this.mcpService.clientBuilder).build();

        // Mock the HTTP response
        doReturn(HttpStatus.NOT_FOUND.value()).when(this.statusLine).getStatusCode();
        doReturn(this.statusLine).when(this.httpResponse).getStatusLine();
        doReturn(this.httpResponse).when(this.httpClient).execute(any());

        // Perform the service call
        assertThrows(DataNotFoundException.class, () ->
            this.mcpService.getMcpDevice(this.mcpDevice.getMrn())
        );
    }

    /**
     * Test that we can successfully create a new MCP device based on the
     * provided object. That means that both name and MRN fields are required
     * to be populated.
     */
    @Test
    void testCreateMcpDevice() throws IOException {
        // Mock the HTTP request
        this.mcpService.clientBuilder = mock(HttpClientBuilder.class);
        doReturn(this.httpClient).when(this.mcpService.clientBuilder).build();

        // Mock the HTTP response
        doReturn(HttpStatus.OK.value()).when(this.statusLine).getStatusCode();
        doReturn(this.statusLine).when(this.httpResponse).getStatusLine();
        doReturn(IOUtils.toInputStream(this.objectMapper.writeValueAsString(this.mcpDevice))).when(this.httpEntity).getContent();
        doReturn(this.httpEntity).when(this.httpResponse).getEntity();
        doReturn(this.httpResponse).when(this.httpClient).execute(any());

        // Perform the service call
        McpDeviceDto result = this.mcpService.createMcpDevice(this.mcpDevice);

        // Make sure the response is correct
        assertNotNull(result);
        assertEquals(this.mcpDevice, result);
    }

    /**
     * Test that if we cannot successfully create a new MCP device based on the
     * provided object, the service will throw a SavingFailedException.
     */
    @Test
    void testCreateMcpDeviceFailure() throws IOException {
        // Mock the HTTP request
        this.mcpService.clientBuilder = mock(HttpClientBuilder.class);
        doReturn(this.httpClient).when(this.mcpService.clientBuilder).build();

        // Mock the HTTP response
        doReturn(HttpStatus.BAD_REQUEST.value()).when(this.statusLine).getStatusCode();
        doReturn(this.statusLine).when(this.httpResponse).getStatusLine();
        doReturn(this.httpResponse).when(this.httpClient).execute(any());

        // Perform the service call
        assertThrows(SavingFailedException.class, () ->
                this.mcpService.createMcpDevice(this.mcpDevice)
        );
    }

    /**
     * Test that we can successfully update an existing MCP device based on the
     * provided MRN and object. That means that both name and MRN fields are
     * required to be populated.
     */
    @Test
    void testUpdateMcpDevice() throws IOException {
        // Mock the HTTP request
        this.mcpService.clientBuilder = mock(HttpClientBuilder.class);
        doReturn(this.httpClient).when(this.mcpService.clientBuilder).build();

        // Mock the HTTP response
        doReturn(HttpStatus.OK.value()).when(this.statusLine).getStatusCode();
        doReturn(this.statusLine).when(this.httpResponse).getStatusLine();
        doReturn(IOUtils.toInputStream(this.objectMapper.writeValueAsString(this.mcpDevice))).when(this.httpEntity).getContent();
        doReturn(this.httpEntity).when(this.httpResponse).getEntity();
        doReturn(this.httpResponse).when(this.httpClient).execute(any());

        // Perform the service call
        McpDeviceDto result = this.mcpService.updateMcpDevice(this.mcpDevice.getMrn(), this.mcpDevice);

        // Make sure the response is correct
        assertNotNull(result);
        assertEquals(this.mcpDevice, result);
    }

    /**
     * Test that if we cannot successfully update an existing MCP device based
     * on the provided MRN and object, the service will throw a
     * SavingFailedException.
     */
    @Test
    void testUpdateMcpDeviceFailure() throws IOException {
        // Mock the HTTP request
        this.mcpService.clientBuilder = mock(HttpClientBuilder.class);
        doReturn(this.httpClient).when(this.mcpService.clientBuilder).build();

        // Mock the HTTP response
        doReturn(HttpStatus.BAD_REQUEST.value()).when(this.statusLine).getStatusCode();
        doReturn(this.statusLine).when(this.httpResponse).getStatusLine();
        doReturn(this.httpResponse).when(this.httpClient).execute(any());

        // Perform the service call
        assertThrows(SavingFailedException.class, () ->
                this.mcpService.updateMcpDevice(this.mcpDevice.getMrn(), this.mcpDevice)
        );
    }

    /**
     * Test that we can successfully delete an existing MCP device based on the
     * provided MRN.
     */
    @Test
    void testDeleteMcpDevice() throws IOException {
        // Mock the HTTP request
        this.mcpService.clientBuilder = mock(HttpClientBuilder.class);
        doReturn(this.httpClient).when(this.mcpService.clientBuilder).build();

        // Mock the HTTP response
        doReturn(HttpStatus.OK.value()).when(this.statusLine).getStatusCode();
        doReturn(this.statusLine).when(this.httpResponse).getStatusLine();
        doReturn(this.httpResponse).when(this.httpClient).execute(any());

        // Perform the service call
        boolean result = this.mcpService.deleteMcpDevice(this.mcpDevice.getMrn());

        // Make sure the response is correct
        assertTrue(result);
    }

    /**
     * Test that if we cannot successfully delete an existing MCP device based
     * on the provided MRN, the service will throw a  DeletingFailedException.
     */
    @Test
    void testDeleteMcpDeviceFailure() throws IOException {
        // Mock the HTTP request
        this.mcpService.clientBuilder = mock(HttpClientBuilder.class);
        doReturn(this.httpClient).when(this.mcpService.clientBuilder).build();

        // Mock the HTTP response
        doReturn(HttpStatus.BAD_REQUEST.value()).when(this.statusLine).getStatusCode();
        doReturn(this.statusLine).when(this.httpResponse).getStatusLine();
        doReturn(this.httpResponse).when(this.httpClient).execute(any());

        // Perform the service call
        assertThrows(DeletingFailedException.class, () ->
                this.mcpService.deleteMcpDevice(this.mcpDevice.getMrn())
        );
    }

    /**
     * Test that we can successfully issue a new X.509 certificate through the
     * MCP MIR by submitting a certificate signature request (CSR) for a
     * given MCP device (based on its MRN).
     */
    @Test
    void testIssueMcpDeviceCertificate() throws IOException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, OperatorCreationException, CertificateException {
        // Spin up a CSR and a generate X.509 certificate
        final KeyPair keypair = X509Utils.generateKeyPair(null);
        final PKCS10CertificationRequest csr = X509Utils.generateX509CSR(keypair, "CN=Test", null);
        final X509Certificate cert = X509Utils.generateX509Certificate(keypair, "CN=Test", new Date(), new Date(), null);

        // Mock the HTTP request
        this.mcpService.clientBuilder = mock(HttpClientBuilder.class);
        doReturn(this.httpClient).when(this.mcpService.clientBuilder).build();

        // Mock the HTTP response
        doReturn(HttpStatus.OK.value()).when(this.statusLine).getStatusCode();
        doReturn(this.statusLine).when(this.httpResponse).getStatusLine();
        doReturn(IOUtils.toInputStream(X509Utils.formatCertificate(cert))).when(this.httpEntity).getContent();
        doReturn(this.httpEntity).when(this.httpResponse).getEntity();
        doReturn(this.httpResponse).when(this.httpClient).execute(any());

        // Perform the service call
        this.mcpService.issueMcpDeviceCertificate(this.mcpDevice.getMrn(), csr);
    }

    /**
     * Test if we cannot successfully issue a new certificate through the
     * provided CSR, and the response is invalid, an InvalidRequestException
     * will be thrown.
     */
    @Test
    void testIssueMcpDeviceCertificateFailure() throws IOException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, OperatorCreationException, CertificateException {
        // Spin up a CSR and a generate X.509 certificate
        final KeyPair keypair = X509Utils.generateKeyPair(null);
        final PKCS10CertificationRequest csr = X509Utils.generateX509CSR(keypair, "CN=Test", null);

        // Mock the HTTP request
        this.mcpService.clientBuilder = mock(HttpClientBuilder.class);
        doReturn(this.httpClient).when(this.mcpService.clientBuilder).build();

        // Mock the HTTP response
        doReturn(HttpStatus.BAD_REQUEST.value()).when(this.statusLine).getStatusCode();
        doReturn(this.statusLine).when(this.httpResponse).getStatusLine();
        doReturn(this.httpResponse).when(this.httpClient).execute(any());

        // Perform the service call
        assertThrows(InvalidRequestException.class, () ->
            this.mcpService.issueMcpDeviceCertificate(this.mcpDevice.getMrn(), csr)
        );
    }

    /**
     * Test that we can successfully revoke an existing X.509 certificate
     * through the MCP MIR.
     */
    @Test
    void testRevokeMcpDeviceCertificate() throws IOException {
        // Mock the HTTP request
        this.mcpService.clientBuilder = mock(HttpClientBuilder.class);
        doReturn(this.httpClient).when(this.mcpService.clientBuilder).build();

        // Mock the HTTP response
        doReturn(HttpStatus.OK.value()).when(this.statusLine).getStatusCode();
        doReturn(this.statusLine).when(this.httpResponse).getStatusLine();
        doReturn(this.httpResponse).when(this.httpClient).execute(any());

        // Perform the service call
        this.mcpService.revokeMcpDeviceCertificate(this.mcpDevice.getMrn(), BigInteger.ONE);
    }

    /**
     * Test that if we fail to revoke an existing certificate from the MCP
     * MIR for any reason, an InvalidRequestException will be thrown.
     */
    @Test
    void testRevokeMcpDeviceCertificateFailure() throws IOException {
        // Mock the HTTP request
        this.mcpService.clientBuilder = mock(HttpClientBuilder.class);
        doReturn(this.httpClient).when(this.mcpService.clientBuilder).build();

        // Mock the HTTP response
        doReturn(HttpStatus.BAD_REQUEST.value()).when(this.statusLine).getStatusCode();
        doReturn(this.statusLine).when(this.httpResponse).getStatusLine();
        doReturn(this.httpResponse).when(this.httpClient).execute(any());

        // Perform the service call
        assertThrows(InvalidRequestException.class, () ->
            this.mcpService.revokeMcpDeviceCertificate(this.mcpDevice.getMrn(), BigInteger.ONE)
        );
    }

    /**
     * Test that we correctly construct the endpoint URLs for the MCP.
     */
    @Test
    void testConstructMcpDeviceEndpointUrl() {
        // First set the host and the organization registered to the MCP
        this.mcpService.host = "localhost";
        this.mcpService.mcpOrgPrefix = "urn:mrn:mcp:org:mcc";
        this.mcpService.organisation = "grad";

        // Make the assertions
        assertEquals("https://localhost/x509/api/org/urn:mrn:mcp:org:mcc:grad/null/", this.mcpService.constructMcpDeviceEndpointUrl(null));
        assertEquals("https://localhost/x509/api/org/urn:mrn:mcp:org:mcc:grad//", this.mcpService.constructMcpDeviceEndpointUrl(""));
        assertEquals("https://localhost/x509/api/org/urn:mrn:mcp:org:mcc:grad/test/", this.mcpService.constructMcpDeviceEndpointUrl("test"));
        assertEquals("https://localhost/x509/api/org/urn:mrn:mcp:org:mcc:grad/test2/", this.mcpService.constructMcpDeviceEndpointUrl("test2"));
        assertEquals("https://localhost/x509/api/org/urn:mrn:mcp:org:mcc:grad/test3/", this.mcpService.constructMcpDeviceEndpointUrl("test3"));
    }

    /**
     * Test that we correctly construct the MCP devices MRNs.
     */
    @Test
    void testConstructMcpDeviceMrn() {
        // First set the host and the organization registered to the MCP
        this.mcpService.host = "localhost";
        this.mcpService.mcpDevicePrefix = "urn:mrn:mcp:device:mcc";
        this.mcpService.organisation = "grad";

        // Make the assertions
        assertEquals("urn:mrn:mcp:device:mcc:grad:null", this.mcpService.constructMcpDeviceMrn(null));
        assertEquals("urn:mrn:mcp:device:mcc:grad:", this.mcpService.constructMcpDeviceMrn(""));
        assertEquals("urn:mrn:mcp:device:mcc:grad:test", this.mcpService.constructMcpDeviceMrn("test"));
        assertEquals("urn:mrn:mcp:device:mcc:grad:test2", this.mcpService.constructMcpDeviceMrn("test2"));
        assertEquals("urn:mrn:mcp:device:mcc:grad:test3", this.mcpService.constructMcpDeviceMrn("test3"));
    }

    /**
     * Test that we can correclty parse the error messages returned by the
     * HTTP calls to the MCP MIR.
     */
    @Test
    void testParseMCPStatusLineError() {
        // Test for a random error message
        doReturn("Random Error").when(this.statusLine).getReasonPhrase();
        assertEquals("Random Error", this.mcpService.parseMCPStatusLineError(this.statusLine));

        // Test for no error message
        doReturn("").when(this.statusLine).getReasonPhrase();
        assertEquals("Unknown error returned by the MCP MIR.", this.mcpService.parseMCPStatusLineError(this.statusLine));
    }

}