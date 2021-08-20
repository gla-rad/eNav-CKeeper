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

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.stream.Collectors;

/**
 * The MRN Entity Service Class
 *
 * Service Implementation for the MCP Certificate Operations.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Service
@Slf4j
public class MCPService {

    // Service Variables
    private String host = "api-x509.maritimeconnectivity.net";
    private String keyStorePass = "I5-jxFaKysB&^-dZhrS&7BF5qr";

    // Apache HTTP Client SSL Context
    private SSLContext sslContext;
    private SSLConnectionSocketFactory sslConSocFactory;
    private HttpClientBuilder clientbuilder;

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
    public void init() throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, UnrecoverableKeyException {
        //Creating SSLContextBuilder object
        SSLContextBuilder SSLBuilder = SSLContexts.custom();

        //Loading the Keystore file
        ClassPathResource trustStoreResource = new ClassPathResource("mcp/keystore.p12");
        SSLBuilder = SSLBuilder.loadKeyMaterial(this.loadKeyMaterial(trustStoreResource.getURI(), keyStorePass.toCharArray()), keyStorePass.toCharArray());

        //Building the SSLContext
        this.sslContext = SSLBuilder.build();

        // Creating SSLConnectionSocketFactory object
        // To allow all hosts, create SSLConnectionSocketFactory object by
        // passing a SSLContext object and a NoopHostnameVerifier object.
        this.sslConSocFactory = new SSLConnectionSocketFactory(this.sslContext, new NoopHostnameVerifier());

        //Creating HttpClientBuilder
        HttpClientBuilder clientbuilder = HttpClients.custom();
        this.clientbuilder = clientbuilder.setSSLSocketFactory(this.sslConSocFactory);
    }

    public void registerEntity() throws IOException {
        System.out.println("Registering");

        //Building the CloseableHttpClient
        CloseableHttpClient httpclient = this.clientbuilder.build();
        HttpGet httpget = new HttpGet("https://" + host + "/x509/api/org/urn:mrn:mcp:org:mcc:grad/services");

        //Executing the request
        HttpResponse httpresponse = httpclient.execute(httpget);

        // Print the result
        String text = new BufferedReader(new InputStreamReader(httpresponse.getEntity().getContent(), StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));
        System.out.println(text);
    }

    protected KeyStore loadKeyMaterial(URI uri, char[] ksp) throws KeyStoreException, IOException {
        final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        final FileInputStream inputStream = new FileInputStream(Paths.get(uri).toFile());
        try {/*w w  w  . ja va 2 s  . c  o m*/
            keyStore.load(inputStream, ksp);
        } catch(NoSuchAlgorithmException | CertificateException ex) {
            this.log.error(ex.getMessage());
        } finally {
            inputStream.close();
        }
        return keyStore;
    }

}
