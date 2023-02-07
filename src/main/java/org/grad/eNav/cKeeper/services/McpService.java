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
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.grad.eNav.cKeeper.exceptions.*;
import org.grad.eNav.cKeeper.models.domain.Pair;
import org.grad.eNav.cKeeper.models.domain.mcp.McpEntityType;
import org.grad.eNav.cKeeper.models.dtos.mcp.McpCertitifateDto;
import org.grad.eNav.cKeeper.models.dtos.mcp.McpEntityBase;
import org.grad.eNav.cKeeper.models.dtos.mcp.McpServiceDto;
import org.grad.eNav.cKeeper.utils.X509Utils;
import org.grad.secom.core.utils.KeyStoreUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
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
     * The X.509 Trust-Store.
     */
    @Value("${gla.rad.ckeeper.mcp.trustStore:mcp/truststore.p12}")
    String trustStore;

    /**
     * The X.509 Trust-Store Password.
     */
    @Value("${gla.rad.ckeeper.mcp.trustStorePassword:password}")
    String trustStorePassword;

    /**
     * The X.509 Trust-Store Type.
     */
    @Value("${gla.rad.ckeeper.mcp.trustStoreType:PKCS12}")
    String trustStoreType;

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

    // Class Variables
    protected WebClient mcpMirClient;
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
        // Initialise the certificate factory
        this.certificateFactory = CertificateFactory.getInstance("X.509");

        // Initialise the HTTP connection configuration
        HttpClient httpConnector = HttpClient
                .create()
                .followRedirect(true);

        // Start Setting up the SSL context builder.
        SslContextBuilder sslContextBuilder = SslContextBuilder
                .forClient();

        // If we have a keystore and a valid password
        if (Strings.isNotBlank(keyStore) && Strings.isNotBlank(keyStorePassword)) {
            sslContextBuilder.keyManager(KeyStoreUtils.getKeyManagerFactory(
                    keyStore, keyStorePassword, keyStoreType, null));
        }

        // If we have a truststore and a valid password
        if (Strings.isNotBlank(this.trustStore) && Strings.isNotBlank(this.trustStorePassword)) {
            sslContextBuilder.trustManager(KeyStoreUtils.getTrustManagerFactory(
                    this.trustStore, this.trustStorePassword, this.trustStoreType, null));
        }
        // Otherwise, check is an insecure policy it to be applied
        else {
            sslContextBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE);
        }

        // Add the SSL context to the HTTP connector
        final SslContext sslContext = sslContextBuilder.build();
        httpConnector = httpConnector.secure(spec -> spec.sslContext(sslContext)
                .handshakeTimeout(Duration.of(2, ChronoUnit.SECONDS)));

        // And create the SECOM web client
        this.mcpMirClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpConnector))
                .baseUrl(this.mcpConfigService.constructMcpBaseUrl())
                //.filter(setJWT())
                .build();
    }

    /**
     * Retrieves the MCP entity object identified by the provided MRN from the
     * MCP MIR if successful. Otherwise, a DataNotFoundException will be thrown.
     *
     * @param mrn       The MRN of the MCP entity to be retrieved
     * @return the retrieved MCP entity object
     * @throws McpConnectivityException if the connection to the MCP is not active
     */
    public <T extends McpEntityBase> T getMcpEntity(@NotNull String mrn,
                                                    String version,
                                                    @NotNull Class<T> entityClass) throws McpConnectivityException {
        // Figure our the type of entity we are working on
        final McpEntityType mcpEntityType = McpEntityType.fromEntityClass(entityClass);
        log.debug("Request to get MCP {} with MRN {}", mcpEntityType.getValue(), mrn);

        // Make sure that the service is up first
        this.checkMcpMirConnectivity();

        // Make sure the MCP device MRN has the right prefix
        final String fullMrn = this.mcpConfigService.constructMcpEntityMrn(mcpEntityType, mrn);

        // And get the MCP entity
        try {
            return this.mcpMirClient.get()
                    .uri(mcpEntityType.getValue() + "/" +
                            fullMrn + "/" +
                            Optional.of(mcpEntityType)
                                    .filter(McpEntityType.SERVICE::equals)
                                    .map(t -> String.format("%s", version))
                                    .orElse(""))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(entityClass)
                    .blockOptional()
                    .orElseThrow(() -> new DataNotFoundException(String.format("Failed to retrieve the requested MRN entity with MRN: %s", fullMrn)));
        } catch (WebClientResponseException ex) {
            throw new DataNotFoundException((ex.getMessage()));
        }
    }

    /**
     * Creates a new MCP entity object into the MCP MIR and returns the created
     * object if successful. Otherwise, a SavingFailedException will be thrown.
     *
     * @param mcpEntity     The MCP entity object to be created
     * @return the created MCP entity object
     * @throws McpConnectivityException if the connection to the MCP is not active
     */
    public <T extends McpEntityBase> T createMcpEntity(@NotNull T mcpEntity) throws McpConnectivityException {
        // Figure our the type of entity we are working on
        McpEntityType mcpEntityType = McpEntityType.fromEntityClass(mcpEntity.getClass());
        log.debug("Request to create a new MCP {} with MRN {}", mcpEntityType.getValue(), mcpEntity.getMrn());

        // Make sure that the service is up first
        this.checkMcpMirConnectivity();

        // Sanity Check
        Optional.of(mcpEntity)
                .filter(d -> StringUtils.isNotBlank(d.getMrn()))
                .orElseThrow(() -> new SavingFailedException("Cannot create new devices in the MCP without an MRN"));

        // Make sure the MCP device MRN has the right prefix
        final String fullMrn = this.mcpConfigService.constructMcpEntityMrn(mcpEntityType, mcpEntity.getMrn());
        mcpEntity.setMrn(fullMrn);

        // And create the MCP entity
        try {
            return this.mcpMirClient.post()
                    .uri(mcpEntityType.getValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(mcpEntity))
                    .retrieve()
                    .bodyToMono(mcpEntityType.getEntityClass())
                    .blockOptional()
                    .map(o -> (T) o)
                    .orElseThrow(() -> new SavingFailedException(String.format("Failed to create the provided MCP entity with MRN: %s", mcpEntity.getMrn())));
        } catch (WebClientResponseException ex) {
            throw new SavingFailedException(ex.getMessage());
        }
    }

    /**
     * Updates the MRN entity identified by the provided MRN in the MCP MIR
     * and returns the updated object if successful. Otherwise, a
     * SavingFailedException will be thrown.
     *
     * @param mrn       The MRN of the MCP entity to be updated
     * @param mcpEntity The MCP entity to be updated
     * @return the updated version of the MCP entity object
     * @throws McpConnectivityException if the connection to the MCP is not active
     */
    public <T extends McpEntityBase> T updateMcpEntity(@NotNull String mrn,
                                                       @NotNull T mcpEntity) throws McpConnectivityException {
        // Figure our the type of entity we are working on
        McpEntityType mcpEntityType = McpEntityType.fromEntityClass(mcpEntity.getClass());
        log.debug("Request to update a existing MCP {} with MRN {}", mcpEntityType.getValue(), mcpEntity.getMrn());

        // Make sure that the service is up first
        this.checkMcpMirConnectivity();

        // Sanity Check
        Optional.of(mcpEntity)
                .filter(d -> StringUtils.isNotBlank(d.getMrn()))
                .orElseThrow(() -> new SavingFailedException("Cannot update devices in the MCP without an MRN"));

        // Make sure the MCP device MRN has the right prefix
        final String fullMrn = this.mcpConfigService.constructMcpEntityMrn(mcpEntityType, mrn);

        // And update the MCP entity
        try {
            this.mcpMirClient.put()
                    .uri(mcpEntityType.getValue() + "/" +
                            fullMrn + "/" +
                            Optional.of(mcpEntityType)
                                    .filter(McpEntityType.SERVICE::equals)
                                    .map(t -> String.format("%s", ((McpServiceDto)mcpEntity).getInstanceVersion()))
                                    .orElse(""))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(mcpEntity))
                    .retrieve()
                    .toBodilessEntity()
                    .filter(response -> response.getStatusCode().is2xxSuccessful())
                    .blockOptional()
                    .orElseThrow(() -> new SavingFailedException(String.format("Failed to update the provided MCP entity with MRN: %s", mcpEntity.getMrn())));
        } catch (WebClientResponseException ex) {
            throw new SavingFailedException(ex.getMessage());
        }

        // Once updated, we can retrieve the full entity again
        return (T) this.getMcpEntity(
                mcpEntity.getMrn(),
                Optional.of(mcpEntity)
                        .filter(McpServiceDto.class::isInstance)
                        .map(McpServiceDto.class::cast)
                        .map(McpServiceDto::getInstanceVersion)
                        .orElse(null),
                mcpEntity.getClass()
        );
    }

    /**
     * Delete the MRN entity identified by the provided MRN from the MCP
     * MIR, if that exists, otherwise a DeletingFailedException will be thrown.
     *
     * @param mrn       The MRN of the MCP entity to be deleted
     * @return whether the operation was successful or not
     * @throws McpConnectivityException if the connection to the MCP is not active
     */
    public <T extends McpEntityBase> boolean deleteMcpEntity(@NotNull String mrn,
                                                             String version,
                                                             @NotNull Class<T> entityClass) throws McpConnectivityException {
        // Figure our the type of entity we are working on
        McpEntityType mcpEntityType = McpEntityType.fromEntityClass(entityClass);
        log.debug("Request to delete MCP {} with MRN {}", mcpEntityType.getValue(), mrn);

        // Make sure that the service is up first
        this.checkMcpMirConnectivity();

        // Make sure the MCP device MRN has the right prefix
        final String fullMrn = this.mcpConfigService.constructMcpEntityMrn(mcpEntityType, mrn);

        // And delete the MCP entity
        try {
            return this.mcpMirClient.delete()
                    .uri(mcpEntityType.getValue() + "/" +
                            fullMrn + "/" +
                            Optional.of(mcpEntityType)
                                    .filter(McpEntityType.SERVICE::equals)
                                    .map(t -> String.format("%s", version))
                                    .orElse(""))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toBodilessEntity()
                    .filter(response -> response.getStatusCode().is2xxSuccessful())
                    .blockOptional()
                    .isPresent();
        } catch (WebClientResponseException ex) {
            throw new DeletingFailedException((ex.getMessage()));
        }
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
     * @throws McpConnectivityException if the connection to the MCP is not active
     */
    public Map<String, X509Certificate> getMcpEntityCertificates(@NotNull McpEntityType mcpEntityType,
                                                                 @NotNull String mrn,
                                                                 String version) throws McpConnectivityException {
        log.debug("Request to retrieve an existing certificate for the MCP {} with MRN {}", mcpEntityType.getValue(), mrn);

        // Get the MCP Entity certificates directly from the MCP MIR
        return this.getMcpEntity(mrn, version, mcpEntityType.getEntityClass())
                .getCertificates()
                .stream()
                .filter(not(McpCertitifateDto::isRevoked))
                .map(McpCertitifateDto::getCertificate)
                .map(s -> s.replace("\\n","\n"))
                .map(pem -> {
                    try {
                        return (X509Certificate) this.certificateFactory.generateCertificate(new ByteArrayInputStream(pem.getBytes()));
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
     * @throws McpConnectivityException if the connection to the MCP is not active
     * @throws IOException if the PEM generation of the CSR fails
     */
    public Pair<String, X509Certificate> issueMcpEntityCertificate(@NotNull McpEntityType mcpEntityType,
                                                                   @NotNull String mrn,
                                                                   String version,
                                                                   @NotNull PKCS10CertificationRequest csr) throws McpConnectivityException, IOException {
        log.debug("Request to issue a new certificate for the MCP {} with MRN {}", mcpEntityType.getValue(), mrn);

        // Make sure that the service is up first
        this.checkMcpMirConnectivity();

        // Make sure the MCP device MRN has the right prefix
        final String fullMrn = this.mcpConfigService.constructMcpEntityMrn(mcpEntityType, mrn);
        final String formattedCsr = X509Utils.formatCSR(csr);

        // And issue the MCP entity certificate
        final ResponseEntity<String> responseEntity;
        try {
            responseEntity = this.mcpMirClient.post()
                    .uri(mcpEntityType.getValue() + "/"
                            + fullMrn
                            + Optional.of(mcpEntityType)
                                .filter(McpEntityType.SERVICE::equals)
                                .map(t -> String.format("/%s", version))
                                .orElse("") + "/"
                            + "certificate/issue-new/csr")
                    .contentType(MediaType.TEXT_PLAIN)
                    .accept(MediaType.ALL)
                    .body(BodyInserters.fromValue(formattedCsr))
                    .retrieve()
                    .toEntity(String.class)
                    .filter(response -> response.getStatusCode().is2xxSuccessful())
                    .blockOptional()
                    .orElseThrow(() -> new InvalidRequestException(String.format("Failed to issue a new certificate for entity with MRN: %s", fullMrn)));
        } catch (WebClientResponseException ex) {
            throw new InvalidRequestException((ex.getMessage()));
        }

        // Now parse the response to return the X.509 certificate object
        final String mcpMirId = Optional.of(responseEntity)
                .map(ResponseEntity::getHeaders)
                .map(headers -> headers.getFirst("Location"))
                .map(location -> location.split("/"))
                .map(path -> path[path.length-1])
                .orElseThrow(() -> new InvalidRequestException(String.format("Certificate without an ID issued for entity with MRN: %s", fullMrn)));
        final String certificate = Optional.of(responseEntity)
                .map(ResponseEntity::getBody)
                .map(s -> s.replace("\\n","\n"))
                .orElseThrow(() -> new InvalidRequestException(String.format("Empty certificate issued for entity with MRN: %s", fullMrn)));
        try {
            return new Pair<>(mcpMirId, (X509Certificate) this.certificateFactory.generateCertificate(new ByteArrayInputStream(certificate.getBytes())));
        } catch (CertificateException ex) {
            throw new InvalidRequestException(ex.getMessage());
        }
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
    public void revokeMcpEntityCertificate(@NotNull McpEntityType mcpEntityType,
                                           @NotNull String mrn,
                                           String version,
                                           @NotNull String mcpMirId) throws IOException, McpConnectivityException {
        log.debug("Request to revoke a certificate for the MCP {} with MRN {}", mcpEntityType.getValue(), mrn);

        // Make sure that the service is up first
        this.checkMcpMirConnectivity();

        // Make sure the MCP device MRN has the right prefix
        final String fullMrn = this.mcpConfigService.constructMcpEntityMrn(mcpEntityType, mrn);

        try {
            this.mcpMirClient.post()
                    .uri(mcpEntityType.getValue() + "/"
                            + fullMrn
                            + Optional.of(mcpEntityType)
                            .filter(McpEntityType.SERVICE::equals)
                            .map(t -> String.format("/%s", version))
                            .orElse("") + "/"
                            + "certificate" + "/"
                            + mcpMirId + "/"
                            + "revoke")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.ALL)
                    .body(BodyInserters.fromValue(new McpRevocationRequest("unspecified", String.format("%d", System.currentTimeMillis()))))
                    .retrieve()
                    .toBodilessEntity()
                    .filter(response -> response.getStatusCode().is2xxSuccessful())
                    .blockOptional()
                    .orElseThrow(() -> new InvalidRequestException(String.format("Failed to revoke a new certificate for entity with MRN: %s", fullMrn)));
        } catch (WebClientResponseException ex) {
            throw new InvalidRequestException((ex.getMessage()));
        }
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
            assert this.mcpMirClient.options()
                    .retrieve()
                    .toBodilessEntity()
                    .filter(response -> response.getStatusCode().is2xxSuccessful())
                    .blockOptional()
                    .isPresent();
        } catch (WebClientResponseException | AssertionError ex) {
            log.trace(ex.getMessage(), ex);
            throw new McpConnectivityException("MCP Identity Registry could not be contacted... please make sure you have connected and try again later!");
        }
    }

    /**
     * A private class that defines the MCP MIR certificate revocation request
     * objects. This includes the reason for revoking the certificate and the
     * time this operation took place.
     */
    @Data
    @AllArgsConstructor
    private class McpRevocationRequest {
        private String revokationReason;
        private String revokedAt;
    }
}
