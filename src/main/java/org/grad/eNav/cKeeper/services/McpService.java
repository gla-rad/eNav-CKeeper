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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.grad.eNav.cKeeper.exceptions.DataNotFoundException;
import org.grad.eNav.cKeeper.exceptions.DeletingFailedException;
import org.grad.eNav.cKeeper.exceptions.InvalidRequestException;
import org.grad.eNav.cKeeper.exceptions.SavingFailedException;
import org.grad.eNav.cKeeper.models.dtos.McpDeviceDto;
import org.grad.eNav.cKeeper.utils.X509Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLContext;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Optional;

/**
 * The MRN Entity Service Class
 *
 * Service Implementation for the MCP Certificate Operations.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Service
@Slf4j
public class McpService {

    /**
     * The MCP Host Address.
     */
    @Value("${gla.rad.ckeeper.mcp.host:api-x509.maritimeconnectivity.net}")
    String host;

    /**
     * The MCP Registered Organisation Name.
     */
    @Value("${gla.rad.ckeeper.mcp.organisation:grad}")
    String organisation;

    /**
     * The MCP Organisation Sting Prefix.
     */
    @Value("${gla.rad.ckeeper.mcp.mrnOrgPrefix:urn:mrn:mcp:org:mcc}")
    String mcpOrgPrefix;

    /**
     * The MCP Device Sting Prefix.
     */
    @Value("${gla.rad.ckeeper.mcp.mrnDevicePrefix:urn:mrn:mcp:device:mcc}")
    String mcpDevicePrefix;

    /**
     * The MCP Keystore File Location.
     */
    @Value("${gla.rad.ckeeper.mcp.keyStore:mcp/keystore.p12}")
    String keyStore;

    /**
     * The MCP keystore File Password.
     */
    @Value("${gla.rad.ckeeper.mcp.keyStorePass:keyStorePass}")
    String keyStorePass;

    /**
     * The JSON Object Mapper
     */
    @Autowired
    ObjectMapper objectMapper;

    // Apache HTTP Client SSL Context
    protected SSLContext sslContext;
    protected HttpClientBuilder clientBuilder;

    /**
     * Once the service has been initialised, it needs to register the
     * MCP keystore with our MCP X.509 certificate into the Java truststore.
     * This was it will be used during the communication with the MCP server,
     * and it will have access to perform the updating operations. We will
     * use the "javax.net.ssl.trustStore" environment variable for that.
     *
     * For more information see: https://www.baeldung.com/java-ssl
     */
    @PostConstruct
    public void init() throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, UnrecoverableKeyException {
        //Loading the Keystore file
        SSLContextBuilder SSLBuilder = SSLContexts.custom();
        ClassPathResource keyStoreResource = new ClassPathResource(this.keyStore);
        SSLBuilder = SSLBuilder.loadKeyMaterial(this.loadKeyMaterial(keyStoreResource.getInputStream(), this.keyStorePass.toCharArray()), this.keyStorePass.toCharArray());

        //Building the SSLContext
        this.sslContext = SSLBuilder.build();

        // Creating SSLConnectionSocketFactory object
        // To allow all hosts, create SSLConnectionSocketFactory object by
        // passing a SSLContext object and a NoopHostnameVerifier object.
        SSLConnectionSocketFactory sslConSocFactory = new SSLConnectionSocketFactory(this.sslContext, new NoopHostnameVerifier());

        //Creating HttpClientBuilder
        this.clientBuilder = HttpClients.custom();
        this.clientBuilder = clientBuilder.setSSLSocketFactory(sslConSocFactory);
    }

    /**
     * Retrieves the MCP device object identified by the provided MRN from the
     * MCP MIR if successful. Otherwise, a DataNotFoundException will be thrown.
     *
     * @param mrn       The MRN of the MCP device to b retrieved
     * @return the retrieved MCP device object
     * @throws IOException if the HTTP call failed to execute
     */
    public McpDeviceDto getMcpDevice(@NotNull String mrn) throws IOException {
        this.log.debug("Request to get all MCP Devices");

        // Make sure the MCP device MRN has the right prefix
        if(!mrn.startsWith(this.mcpDevicePrefix)) {
            mrn = String.format("%s:%s:%s", this.mcpDevicePrefix, this.organisation, mrn);
        }

        //Building the CloseableHttpClient
        CloseableHttpClient httpClient = this.clientBuilder.build();
        HttpGet httpGet = new HttpGet(this.constructMcpDeviceEndpointUrl("device") + mrn);

        //Executing the request
        HttpResponse httpResponse = httpClient.execute(httpGet);

        // Construct and return the MCP device object through JSON
        return Optional.of(httpResponse)
                .filter(r -> r.getStatusLine().getStatusCode() == HttpStatus.OK.value())
                .map(HttpResponse::getEntity)
                .map(e -> {
                    try {
                        return this.objectMapper.readValue(e.getContent(), McpDeviceDto.class);
                    } catch (IOException ex) {
                        throw new DataNotFoundException("Unable to parse MCP response");
                    }
                })
                .orElseThrow(() -> new DataNotFoundException(this.parseMCPStatusLineError(httpResponse.getStatusLine())));
    }

    /**
     * Creates a new MCP device object into the MCP MIR and returns the created
     * object if successful. Otherwise, a SavingFailedException will be thrown.
     *
     * @param mcpDevice     The MCP device object to be created
     * @return the created MCP device object
     * @throws IOException if the HTTP call failed to execute
     */
    public McpDeviceDto createMcpDevice(@NotNull McpDeviceDto mcpDevice) throws IOException {
        this.log.debug("Request to create a new MCP Device with MRN {}", mcpDevice.getMrn());

        // Sanity Check
        Optional.of(mcpDevice)
                .filter(d -> StringUtils.isNotBlank(d.getName()))
                .filter(d -> StringUtils.isNotBlank(d.getMrn()))
                .orElseThrow(() -> new SavingFailedException("Cannot create new devices in the MCP without a name or an MRN"));

        // Make sure the MCP device MRN has the right prefix
        if(!mcpDevice.getMrn().startsWith(this.mcpDevicePrefix)) {
            mcpDevice.setMrn(String.format("%s:%s:%s", this.mcpDevicePrefix, this.organisation, mcpDevice.getMrn()));
        }

        // Convert the new MCP Device object to a string entity
        String json = this.objectMapper.writeValueAsString(mcpDevice);
        StringEntity entity = new StringEntity(json);
        entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));

        //Building the CloseableHttpClient
        CloseableHttpClient httpClient = this.clientBuilder.build();
        HttpPost httpPost = new HttpPost(this.constructMcpDeviceEndpointUrl("device"));
        httpPost.setEntity(entity);

        //Executing the request
        HttpResponse httpResponse = httpClient.execute(httpPost);

        // Construct and return the MCP device object through JSON
        return Optional.of(httpResponse)
                .filter(r -> r.getStatusLine().getStatusCode() == HttpStatus.OK.value())
                .map(HttpResponse::getEntity)
                .map(e -> {
                    try {
                        return this.objectMapper.readValue(e.getContent(), McpDeviceDto.class);
                    } catch (IOException ex) {
                        throw new SavingFailedException("Unable to parse MCP response");
                    }
                })
                .orElseThrow(() -> new SavingFailedException(this.parseMCPStatusLineError(httpResponse.getStatusLine())));
    }

    /**
     * Updates the MRN device identified by the provided MRN in the MCP MIR
     * and returns the updated object if successful. Otherwise, a
     * SavingFailedException will be thrown.
     *
     * @param mrn       The MRN of the MCP device to be updated
     * @param mcpDevice The MCP device to be updated
     * @return the updated version of the MCP device object
     * @throws IOException if the HTTP call failed to execute
     */
    public McpDeviceDto updateMcpDevice(@NotNull String mrn, @NotNull McpDeviceDto mcpDevice) throws IOException {
        this.log.debug("Request to update a new MCP Device with MRN {}", mcpDevice.getMrn());

        // Sanity Check
        Optional.of(mcpDevice)
                .filter(d -> StringUtils.isNotBlank(d.getName()))
                .filter(d -> StringUtils.isNotBlank(d.getMrn()))
                .orElseThrow(() -> new SavingFailedException("Cannot update devices in the MCP without a name or an MRN"));

        // Make sure the MCP device MRN has the right prefix
        if(!mrn.startsWith(this.mcpDevicePrefix)) {
            mrn = String.format("%s:%s:%s", this.mcpDevicePrefix, this.organisation, mrn);
        }

        // Convert the new MCP Device object to a string entity
        String json = this.objectMapper.writeValueAsString(mcpDevice);
        StringEntity entity = new StringEntity(json);
        entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));

        //Building the CloseableHttpClient
        CloseableHttpClient httpClient = this.clientBuilder.build();
        HttpPut httpPut = new HttpPut(this.constructMcpDeviceEndpointUrl("device") + mrn);
        httpPut.setEntity(entity);

        //Executing the request
        HttpResponse httpResponse = httpClient.execute(httpPut);

        // Construct and return the MCP device object through JSON
        return Optional.of(httpResponse)
                .filter(r -> r.getStatusLine().getStatusCode() == HttpStatus.OK.value())
                .map(HttpResponse::getEntity)
                .map(e -> {
                    try {
                        return this.getMcpDevice(mcpDevice.getMrn());
                    } catch (IOException ex) {
                        throw new SavingFailedException("Unable to parse MCP response");
                    }
                })
                .orElseThrow(() -> new SavingFailedException(this.parseMCPStatusLineError(httpResponse.getStatusLine())));
    }

    /**
     * Delete the MRN device identified by the provided MRN from the MCP
     * MIR, if that exists, otherwise a DeletingFailedException will be thrown.
     *
     * @param mrn       The MRN of the MCP device to be deleted
     * @return whether the operation was successful or not
     * @throws IOException if the HTTP call failed to execute
     */
    public boolean deleteMcpDevice(@NotNull String mrn) throws IOException {
        this.log.debug("Request to delete MCP Device with MRN {}", mrn);

        // Make sure the MCP device MRN has the right prefix
        if(!mrn.startsWith(this.mcpDevicePrefix)) {
            mrn = String.format("%s:%s:%s", this.mcpDevicePrefix, this.organisation, mrn);
        }

        //Building the CloseableHttpClient
        CloseableHttpClient httpClient = this.clientBuilder.build();
        HttpDelete httpDelete = new HttpDelete(this.constructMcpDeviceEndpointUrl("device") + mrn);

        //Executing the request
        HttpResponse httpResponse = httpClient.execute(httpDelete);

        // Construct and return the MCP device object through JSON
        return Optional.of(httpResponse)
                .filter(r -> r.getStatusLine().getStatusCode() == HttpStatus.OK.value())
                .map(r -> Boolean.TRUE)
                .orElseThrow(() -> new DeletingFailedException(this.parseMCPStatusLineError(httpResponse.getStatusLine())));
    }

    /**
     * Requests the MCP MIR to issue a new X509 certificate based on the
     * provided certificate signing operation. This certificate will be attached
     * to the specified MCP device, based on the provided MRN.
     *
     * @param mrn           The MCP device MRN to attach the new certificate tp
     * @param csr           The certificate signing request to issue the certificate from
     * @return the signed X.509 certificate
     * @throws IOException if the PEM generation of the csr or the HTTP request fail
     */
    public X509Certificate issueMcpDeviceCertificate(String mrn, PKCS10CertificationRequest csr) throws IOException {
        this.log.debug("Request to issue a new certificate for the  MCP Device with MRN {}", mrn);

        // Make sure the MCP device MRN has the right prefix
        if(!mrn.startsWith(this.mcpDevicePrefix)) {
            mrn = String.format("%s:%s:%s", this.mcpDevicePrefix, this.organisation, mrn);
        }

        // Convert the new MCP Device object to a string entity
        StringEntity entity = new StringEntity(X509Utils.formatCSR(csr));
        entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "text/plain"));

        //Building the CloseableHttpClient
        CloseableHttpClient httpClient = this.clientBuilder.build();
        HttpPost httpPost = new HttpPost(this.constructMcpDeviceEndpointUrl("device") + mrn + "/certificate/issue-new/csr");
        httpPost.setEntity(entity);

        //Executing the request
        HttpResponse httpResponse = httpClient.execute(httpPost);

        // Construct and return the MCP device object through JSON
        return Optional.of(httpResponse)
                .filter(r -> r.getStatusLine().getStatusCode() == HttpStatus.OK.value())
                .map(HttpResponse::getEntity)
                .map(e -> {
                    try {
                        return (X509Certificate) CertificateFactory.getInstance("X.509")
                                .generateCertificate(e.getContent());
                    } catch (CertificateException | IOException ex) {
                        throw new  InvalidRequestException(ex.getMessage());
                    }
                })
                .orElseThrow(() -> new InvalidRequestException(this.parseMCPStatusLineError(httpResponse.getStatusLine())));
    }

    /**
     * Requests the MCP MIR to issue revoke an existing X509 certificate based
     * on the provided MCP device MRN and the certificate ID.
     *
     * @param mrn       The MRN of the MCP device to revoke the certificate for
     * @param certId    The ID of the certificate to be revoked
     * @throws IOException if the HTTP request fails
     */
    public void revokeMcpDeviceCertificate(String mrn, BigInteger certId) throws IOException {
        this.log.debug("Request to revoke a certificate for the  MCP Device with MRN {}", mrn);

        // Make sure the MCP device MRN has the right prefix
        if(!mrn.startsWith(this.mcpDevicePrefix)) {
            mrn = String.format("%s:%s:%s", this.mcpDevicePrefix, this.organisation, mrn);
        }

        //Building the CloseableHttpClient
        CloseableHttpClient httpClient = this.clientBuilder.build();
        HttpPost httpPost = new HttpPost(this.constructMcpDeviceEndpointUrl("device") + mrn + "/certificate/" + certId + "/revoke");

        //Executing the request
        HttpResponse httpResponse = httpClient.execute(httpPost);

        // Construct and return the MCP device object through JSON
        Optional.of(httpResponse)
                .filter(r -> r.getStatusLine().getStatusCode() == HttpStatus.OK.value())
                .orElseThrow(() -> new InvalidRequestException(this.parseMCPStatusLineError(httpResponse.getStatusLine())));
    }

    /**
     * Provided a keystore URI location and the corresponding encryption
     * password, this function will load and the return the keystore in a
     * Java Security format.
     *
     * @param kin    The keystore input stream
     * @param ksp   The keystore encryption password
     * @return The loaded keystore
     * @throws KeyStoreException if no Provider supports a KeyStoreSpi implementation for the specified type
     * @throws IOException for any file loading operations gone wrong
     */
    protected KeyStore loadKeyMaterial(InputStream kin, char[] ksp) throws KeyStoreException, IOException {
        final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try {
            keyStore.load(kin, ksp);
        } catch(NoSuchAlgorithmException | CertificateException ex) {
            this.log.error(ex.getMessage());
        } finally {
            kin.close();
        }
        return keyStore;
    }

    /**
     * A helper function to construct the appropriate MCP endpoint URL, based
     * on the currently loaded host, registered organisation and endpoint to
     * be reached.
     *
     * @param endpoint  The MCP endpoint to be reached
     * @return the complete MCP endpoint URL
     */
    protected String constructMcpDeviceEndpointUrl(String endpoint) {
        return String.format("https://%s/x509/api/org/%s:%s/%s/", this.host, this.mcpOrgPrefix, this.organisation, endpoint);
    }

    /**
     * Tries to identify the error returns by the MCP MIR server and if non
     * found, a standard error message is returned.
     *
     * @param statusLine    The MCP MIR returned status line
     * @return The parsed error message
     */
    protected String parseMCPStatusLineError(StatusLine statusLine) {
        return Optional.of(statusLine)
                .map(StatusLine::getReasonPhrase)
                .filter(StringUtils::isNotBlank)
                .orElse("Unknown error returned by the MCP MIR.");
    }

}
