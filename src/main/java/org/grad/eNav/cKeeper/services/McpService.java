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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.grad.eNav.cKeeper.exceptions.*;
import org.grad.eNav.cKeeper.models.domain.mcp.McpEntityType;
import org.grad.eNav.cKeeper.models.domain.Pair;
import org.grad.eNav.cKeeper.models.dtos.mcp.McpCertitifateDto;
import org.grad.eNav.cKeeper.models.dtos.mcp.McpEntityBase;
import org.grad.eNav.cKeeper.models.dtos.mcp.McpServiceDto;
import org.grad.eNav.cKeeper.utils.X509Utils;
import org.grad.secom.core.utils.KeyStoreUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLContext;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

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
     * The MCP Keystore File Location.
     */
    @Value("${gla.rad.ckeeper.mcp.keyStore:mcp/keystore.p12}")
    String keyStore;

    /**
     * The MCP Keystore File Password.
     */
    @Value("${gla.rad.ckeeper.mcp.keyStorePassword:password}")
    String keyStorePassword;

    /**
     * The MCP Keystore File Type.
     */
    @Value("${gla.rad.ckeeper.mcp.keyStoreType:PKCS12}")
    String keyStoreType;

    /**
     * The JSON Object Mapper
     */
    @Autowired
    ObjectMapper objectMapper;

    /**
     * The MCP Base Service.
     */
    @Autowired
    McpConfigService mcpConfigService;

    // Apache HTTP Client SSL Context
    protected SSLContext sslContext;
    protected HttpClientBuilder clientBuilder;
    protected RestTemplate restTemplate;

    // Certificate Factory
    protected CertificateFactory certificateFactory;

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
    public void init() throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, UnrecoverableKeyException, CertificateException {
        //Loading the Keystore file
        SSLContextBuilder SSLBuilder = SSLContexts.custom();
        SSLBuilder = SSLBuilder.loadKeyMaterial(KeyStoreUtils.getKeyStore(this.keyStore, this.keyStorePassword, this.keyStoreType), this.keyStorePassword.toCharArray());

        //Building the SSLContext
        this.sslContext = SSLBuilder.build();

        // Creating SSLConnectionSocketFactory object
        // To allow all hosts, create SSLConnectionSocketFactory object by
        // passing a SSLContext object and a NoopHostnameVerifier object.
        SSLConnectionSocketFactory sslConSocFactory = new SSLConnectionSocketFactory(this.sslContext, new NoopHostnameVerifier());

        //Creating HttpClientBuilder
        this.clientBuilder = HttpClients.custom();
        this.clientBuilder = clientBuilder.setSSLSocketFactory(sslConSocFactory);
        this.restTemplate = new RestTemplate();

        // Initialise the certificate factory
        this.certificateFactory = CertificateFactory.getInstance("X.509");
    }

    /**
     * Retrieves the MCP entity object identified by the provided MRN from the
     * MCP MIR if successful. Otherwise, a DataNotFoundException will be thrown.
     *
     * @param mrn       The MRN of the MCP entity to be retrieved
     * @return the retrieved MCP entity object
     * @throws IOException if the HTTP call failed to execute
     */
    public <T extends McpEntityBase> T getMcpEntity(@NotNull String mrn,
                                                    String version,
                                                    @NotNull Class<T> entityClass) throws IOException, McpConnectivityException {
        // Figure our the type of entity we are working on
        McpEntityType mcpEntityType = McpEntityType.fromEntityClass(entityClass);
        this.log.debug("Request to get MCP {} with MRN {}", mcpEntityType.getValue(), mrn);

        // Make sure that the service is up first
        this.checkMcpMirConnectivity();

        // Make sure the MCP device MRN has the right prefix
        mrn = this.mcpConfigService.constructMcpEntityMrn(mcpEntityType, mrn);

        //Building the CloseableHttpClient
        CloseableHttpClient httpClient = this.clientBuilder.build();
        HttpGet httpGet = new HttpGet(this.mcpConfigService.constructMcpEndpointUrl(mcpEntityType.getValue())
                + mrn
                + Optional.of(mcpEntityType)
                        .filter(McpEntityType.SERVICE::equals)
                        .map(t -> String.format("/%s", version))
                        .orElse(""));

        //Executing the request
        HttpResponse httpResponse = httpClient.execute(httpGet);

        // Construct and return the MCP device object through JSON
        return Optional.of(httpResponse)
                .filter(r -> r.getStatusLine().getStatusCode() == HttpStatus.OK.value())
                .map(HttpResponse::getEntity)
                .map(e -> {
                    try {
                        return (T) this.objectMapper.readValue(e.getContent(), mcpEntityType.getEntityClass());
                    } catch (IOException ex) {
                        throw new DataNotFoundException("Unable to parse MCP response");
                    }
                })
                .orElseThrow(() -> new DataNotFoundException(this.parseMCPStatusLineError(httpResponse.getStatusLine())));
    }

    /**
     * Creates a new MCP entity object into the MCP MIR and returns the created
     * object if successful. Otherwise, a SavingFailedException will be thrown.
     *
     * @param mcpEntity     The MCP entity object to be created
     * @return the created MCP entity object
     * @throws IOException if the HTTP call failed to execute
     */
    public <T extends McpEntityBase> T createMcpEntity(@NotNull T mcpEntity) throws IOException, McpConnectivityException {
        // Figure our the type of entity we are working on
        McpEntityType mcpEntityType = McpEntityType.fromEntityClass(mcpEntity.getClass());
        this.log.debug("Request to create a new MCP {} with MRN {}", mcpEntityType.getValue(), mcpEntity.getMrn());

        // Make sure that the service is up first
        this.checkMcpMirConnectivity();

        // Sanity Check
        Optional.of(mcpEntity)
                .filter(d -> StringUtils.isNotBlank(d.getMrn()))
                .orElseThrow(() -> new SavingFailedException("Cannot create new devices in the MCP without an MRN"));

        // Make sure the MCP device MRN has the right prefix
        mcpEntity.setMrn(this.mcpConfigService.constructMcpEntityMrn(mcpEntityType, mcpEntity.getMrn()));

        // Convert the new MCP Device object to a string entity
        String json = this.objectMapper.writeValueAsString(mcpEntity);
        StringEntity entity = new StringEntity(json);
        entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));

        //Building the CloseableHttpClient
        CloseableHttpClient httpClient = this.clientBuilder.build();
        HttpPost httpPost = new HttpPost(this.mcpConfigService.constructMcpEndpointUrl(mcpEntityType.getValue()));
        httpPost.setEntity(entity);

        //Executing the request
        HttpResponse httpResponse = httpClient.execute(httpPost);

        // Construct and return the MCP device object through JSON
        return Optional.of(httpResponse)
                .filter(r -> r.getStatusLine().getStatusCode() == HttpStatus.CREATED.value())
                .map(HttpResponse::getEntity)
                .map(e -> {
                    try {
                        return (T) this.objectMapper.readValue(e.getContent(), mcpEntity.getClass());
                    } catch (IOException ex) {
                        throw new SavingFailedException("Unable to parse MCP response");
                    }
                })
                .orElseThrow(() -> new SavingFailedException(this.parseMCPStatusLineError(httpResponse.getStatusLine())));
    }

    /**
     * Updates the MRN entity identified by the provided MRN in the MCP MIR
     * and returns the updated object if successful. Otherwise, a
     * SavingFailedException will be thrown.
     *
     * @param mrn       The MRN of the MCP entity to be updated
     * @param mcpEntity The MCP entity to be updated
     * @return the updated version of the MCP entity object
     * @throws IOException if the HTTP call failed to execute
     */
    public <T extends McpEntityBase> T updateMcpEntity(@NotNull String mrn,
                                                       @NotNull T mcpEntity) throws IOException, McpConnectivityException {
        // Figure our the type of entity we are working on
        McpEntityType mcpEntityType = McpEntityType.fromEntityClass(mcpEntity.getClass());
        this.log.debug("Request to update a existing MCP {} with MRN {}", mcpEntityType.getValue(), mcpEntity.getMrn());

        // Make sure that the service is up first
        this.checkMcpMirConnectivity();

        // Sanity Check
        Optional.of(mcpEntity)
                .filter(d -> StringUtils.isNotBlank(d.getMrn()))
                .orElseThrow(() -> new SavingFailedException("Cannot update devices in the MCP without an MRN"));

        // Make sure the MCP device MRN has the right prefix
        mrn = this.mcpConfigService.constructMcpEntityMrn(mcpEntityType, mrn);

        // Convert the new MCP Device object to a string entity
        String json = this.objectMapper.writeValueAsString(mcpEntity);
        StringEntity entity = new StringEntity(json);
        entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));

        //Building the CloseableHttpClient
        CloseableHttpClient httpClient = this.clientBuilder.build();
        HttpPut httpPut = new HttpPut(this.mcpConfigService.constructMcpEndpointUrl(mcpEntityType.getValue())
                + mrn
                + Optional.of(mcpEntityType)
                    .filter(McpEntityType.SERVICE::equals)
                    .map(t -> String.format("/%s", ((McpServiceDto)mcpEntity).getInstanceVersion()))
                    .orElse(""));
        httpPut.setEntity(entity);

        //Executing the request
        HttpResponse httpResponse = httpClient.execute(httpPut);

        // Construct and return the MCP device object through JSON
        return Optional.of(httpResponse)
                .filter(r -> r.getStatusLine().getStatusCode() == HttpStatus.OK.value())
                .map(HttpResponse::getEntity)
                .map(e -> {
                    try {
                        return (T) this.getMcpEntity(
                                mcpEntity.getMrn(),
                                Optional.of(mcpEntity)
                                        .filter(McpServiceDto.class::isInstance)
                                        .map(McpServiceDto.class::cast)
                                        .map(McpServiceDto::getInstanceVersion)
                                        .orElse(null),
                                mcpEntity.getClass());
                    } catch (McpConnectivityException ex) {
                        throw new SavingFailedException(ex.getMessage());
                    } catch (IOException ex) {
                        throw new SavingFailedException("Unable to parse MCP response");
                    }
                })
                .orElseThrow(() -> new SavingFailedException(this.parseMCPStatusLineError(httpResponse.getStatusLine())));
    }

    /**
     * Delete the MRN entity identified by the provided MRN from the MCP
     * MIR, if that exists, otherwise a DeletingFailedException will be thrown.
     *
     * @param mrn       The MRN of the MCP entity to be deleted
     * @return whether the operation was successful or not
     * @throws IOException if the HTTP call failed to execute
     */
    public <T extends McpEntityBase> boolean deleteMcpEntity(@NotNull String mrn,
                                                             String version,
                                                             @NotNull Class<T> entityClass) throws IOException, McpConnectivityException {
        // Figure our the type of entity we are working on
        McpEntityType mcpEntityType = McpEntityType.fromEntityClass(entityClass);
        this.log.debug("Request to delete MCP {} with MRN {}", mcpEntityType.getValue(), mrn);

        // Make sure that the service is up first
        this.checkMcpMirConnectivity();

        // Make sure the MCP device MRN has the right prefix
        mrn = this.mcpConfigService.constructMcpEntityMrn(mcpEntityType, mrn);

        //Building the CloseableHttpClient
        CloseableHttpClient httpClient = this.clientBuilder.build();
        HttpDelete httpDelete = new HttpDelete(this.mcpConfigService.constructMcpEndpointUrl(mcpEntityType.getValue())
                + mrn
                + Optional.of(mcpEntityType)
                    .filter(McpEntityType.SERVICE::equals)
                    .map(t -> String.format("/%s", version))
                    .orElse(""));

        //Executing the request
        HttpResponse httpResponse = httpClient.execute(httpDelete);

        // Construct and return the MCP device object through JSON
        return Optional.of(httpResponse)
                .filter(r -> r.getStatusLine().getStatusCode() == HttpStatus.OK.value())
                .map(r -> Boolean.TRUE)
                .orElseThrow(() -> new DeletingFailedException(this.parseMCPStatusLineError(httpResponse.getStatusLine())));
    }

    /**
     * This method can be used to retrieve all the certificates available for
     * a specific MCP entity registered in the MCP Identity Registry. This way
     * we can keep the local MCP certificate up to date with the MIR database.
     *
     * @param mcpEntityType The MCP entity type
     * @param mrn           The MCP entity MRN to retrieve the certificates for
     * @param version       The version (if applicable) of the MCP entity
     * @return the list of available certificates
     * @throws IOException if the response parsing operation fails
     */
    public Map<String, X509Certificate> getMcpEntityCertificates(McpEntityType mcpEntityType, String mrn, String version) throws IOException, McpConnectivityException {
        this.log.debug("Request to retrieve an existing certificate for the MCP {} with MRN {}", mcpEntityType.getValue(), mrn);

        // Make sure that the service is up first
        this.checkMcpMirConnectivity();

        // Make sure the MCP device MRN has the right prefix
        mrn = this.mcpConfigService.constructMcpEntityMrn(mcpEntityType, mrn);

        //Building the CloseableHttpClient
        CloseableHttpClient httpClient = this.clientBuilder.build();
        HttpGet httpGet = new HttpGet(this.mcpConfigService.constructMcpEndpointUrl(mcpEntityType.getValue())
                + mrn
                + Optional.of(mcpEntityType)
                        .filter(McpEntityType.SERVICE::equals)
                        .map(t -> String.format("/%s", version))
                        .orElse(""));

        //Executing the request
        HttpResponse httpResponse = httpClient.execute(httpGet);

        // Construct and return the MCP certificate objects through JSON
        final JsonNode jsonCertificates = Optional.of(httpResponse)
                .filter(r -> r.getStatusLine().getStatusCode() == HttpStatus.OK.value())
                .map(HttpResponse::getEntity)
                .map(entity -> {
                    try {
                        return objectMapper.readTree(EntityUtils.toString(entity))
                                .get("certificates");
                    } catch (IOException ex) {
                        throw new InvalidRequestException(ex.getMessage());
                    }
                })
                .orElseThrow(() -> new InvalidRequestException(this.parseMCPStatusLineError(httpResponse.getStatusLine())));

        // Now map JSON into MCP Certificate Objects for easier handling
        return Arrays.asList(this.objectMapper.readerForArrayOf(McpCertitifateDto.class).readValue(jsonCertificates))
                .stream()
                .filter(McpCertitifateDto.class::isInstance)
                .map(McpCertitifateDto.class::cast)
                .filter(not(McpCertitifateDto::isRevoked))
                .map(McpCertitifateDto::getCertificate)
                .map(s -> s.replace("\\n","\n"))
                .map(IOUtils::toInputStream)
                .map(c -> {
                    try {
                        return (X509Certificate) this.certificateFactory.generateCertificate(c);
                    } catch (CertificateException ex) {
                        // Don't include invalid certificates
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(c -> c.getSerialNumber().toString(), Function.identity()));
    }

    /**
     * Requests the MCP MIR to issue a new X509 certificate based on the
     * provided certificate signing operation. This certificate will be attached
     * to the specified MCP entity, based on the provided entity type, the MRN
     * and the entity version.
     *
     * @param mcpEntityType The MCP entity type
     * @param mrn           he MCP device MRN to attach the new certificate to
     * @param version       The version (if applicable) of the MCP entity
     * @param csr           The certificate signing request to issue the certificate from
     * @return the signed X.509 certificate
     * @throws IOException if the PEM generation of the csr or the HTTP request fail
     */
    public Pair<String, X509Certificate> issueMcpEntityCertificate(McpEntityType mcpEntityType, String mrn, String version, PKCS10CertificationRequest csr) throws IOException, McpConnectivityException {
        this.log.debug("Request to issue a new certificate for the MCP {} with MRN {}", mcpEntityType.getValue(), mrn);

        // Make sure that the service is up first
        this.checkMcpMirConnectivity();

        // Make sure the MCP device MRN has the right prefix
        mrn = this.mcpConfigService.constructMcpEntityMrn(mcpEntityType, mrn);

        // Convert the new MCP Device object to a string entity
        StringEntity entity = new StringEntity(X509Utils.formatCSR(csr));
        entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "text/plain"));

        //Building the CloseableHttpClient
        CloseableHttpClient httpClient = this.clientBuilder.build();
        HttpPost httpPost = new HttpPost(this.mcpConfigService.constructMcpEndpointUrl(mcpEntityType.getValue())
                + mrn
                + Optional.of(mcpEntityType)
                    .filter(McpEntityType.SERVICE::equals)
                    .map(t -> String.format("/%s", version))
                    .orElse("")
                + "/certificate/issue-new/csr");
        httpPost.setEntity(entity);

        //Executing the request
        HttpResponse httpResponse = httpClient.execute(httpPost);

        // Construct and return the MCP device object through JSON
        return Optional.of(httpResponse)
                .filter(r -> r.getStatusLine().getStatusCode() == HttpStatus.CREATED.value())
                .filter(r -> Objects.nonNull(r.getFirstHeader("Location")))
                .map(response -> {
                    // Get the MCP MIR ID of the generated certificate from the location header
                    String location = response.getFirstHeader("Location").getValue();
                    String[] locationPath = location.split("/");
                    String mcpMirId = locationPath[locationPath.length-1];

                    // Build the output with the ID and the certificate
                    try {
                        return new Pair<>(
                                mcpMirId,
                                (X509Certificate) this.certificateFactory.generateCertificate(response.getEntity().getContent())
                        );
                    } catch (CertificateException | IOException ex) {
                        throw new InvalidRequestException(ex.getMessage());
                    }
                })
                .orElseThrow(() -> new InvalidRequestException(this.parseMCPStatusLineError(httpResponse.getStatusLine())));
    }

    /**
     * Requests the MCP MIR to issue revoke an existing X509 certificate based
     * on the provided MCP entity type, the MRN, the entiry version and the
     * certificate ID.
     *
     * @param mcpEntityType The MCP entity type
     * @param mrn           The MRN of the MCP device to revoke the certificate for
     * @param version       The version (if applicable) of the MCP entity
     * @param mcpMirId  The MCP MIR ID of the certificate to be revoked
     * @throws IOException if the HTTP request fails
     */
    public void revokeMcpEntityCertificate(McpEntityType mcpEntityType, String mrn, String version, String mcpMirId) throws IOException, McpConnectivityException {
        this.log.debug("Request to revoke a certificate for the MCP {} with MRN {}", mcpEntityType.getValue(), mrn);

        // Make sure that the service is up first
        this.checkMcpMirConnectivity();

        // Make sure the MCP device MRN has the right prefix
        mrn = this.mcpConfigService.constructMcpEntityMrn(mcpEntityType, mrn);

        //Building the CloseableHttpClient
        CloseableHttpClient httpClient = this.clientBuilder.build();
        HttpPost httpPost = new HttpPost(this.mcpConfigService.constructMcpEndpointUrl(mcpEntityType.getValue())
                + mrn
                + Optional.of(mcpEntityType)
                    .filter(McpEntityType.SERVICE::equals)
                    .map(t -> String.format("/%s", version))
                    .orElse("")
                + "/certificate/" + mcpMirId + "/revoke");

        // Add the request body
        ObjectNode jsonNode = this.objectMapper.createObjectNode();
        jsonNode.put("revokationReason", "unspecified");
        jsonNode.put("revokedAt", String.format("%d", System.currentTimeMillis()));
        httpPost.setEntity(new StringEntity(jsonNode.toString(), ContentType.APPLICATION_JSON));

        //Executing the request
        HttpResponse httpResponse = httpClient.execute(httpPost);

        // Construct and return the MCP device object through JSON
        Optional.of(httpResponse)
                .filter(r -> r.getStatusLine().getStatusCode() == HttpStatus.OK.value())
                .orElseThrow(() -> new InvalidRequestException(this.parseMCPStatusLineError(httpResponse.getStatusLine())));
    }

    /**
     * An attempt to verify that the connection with the MCP Identity Registry
     * is up and active. We just check the address of an empty device which
     * should basically give as the organisation endpoint. Don't worry about
     * the response or authorisation, we just need to make sure the service is
     * up, and we can contact it.
     *
     * @return Whether the connection to the MCP Identity Registry is possible
     */
    public void checkMcpMirConnectivity() throws McpConnectivityException {
        try {
            HttpHeaders httpHeaders = this.restTemplate.headForHeaders(this.mcpConfigService.constructMcpEndpointUrl(""));
            assert httpHeaders != null && !httpHeaders.isEmpty();
        } catch (RestClientException | AssertionError ex) {
            this.log.trace(ex.getMessage(), ex);
            throw new McpConnectivityException("MCP Identity Registry could not be contacted... please make sure you have connected and try again later!");
        }
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
