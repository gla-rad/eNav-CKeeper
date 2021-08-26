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

package org.grad.eNav.cKeeper.utils;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCSException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

class X509UtilsTest {

    // Test Variables
    private byte[] challenge = new byte[10000];

    /**
     * Add the Bouncy Castle as a security provider for the unit tests.
     */
    @BeforeAll
    static void addSecurityProvider() {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setup() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        // Create a challenge for validating public/private key pairs
        ThreadLocalRandom.current().nextBytes(this.challenge);
    }

    /**
     * Test that we can generate an elliptic curve key-pair with the default
     * encryption curve and that the generated public/private keys are valid.
     *
     * @throws NoSuchAlgorithmException if the provided encryption algorithm is not found
     * @throws InvalidAlgorithmParameterException if the provided encryption parameters ar not valid
     * @throws InvalidKeyException if the provided key is not valid
     * @throws SignatureException if the provided public key does not match the signed content
     */
    @Test
    void testGenerateKeyPairDefault() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, SignatureException {
        KeyPair keyPair = X509Utils.generateKeyPair(null);

        // Basic Assertions
        assertNotNull(keyPair);
        assertNotNull(keyPair.getPublic());
        assertEquals("EC", keyPair.getPublic().getAlgorithm());
        assertEquals("X.509", keyPair.getPublic().getFormat());
        assertNotNull(keyPair.getPrivate());
        assertEquals("EC", keyPair.getPrivate().getAlgorithm());
        assertEquals("PKCS#8", keyPair.getPrivate().getFormat());

        // Sign using the private key
        Signature sig = Signature.getInstance("SHA256withECDSA");
        sig.initSign(keyPair.getPrivate());
        sig.update(challenge);

        // Verify signature using the public key
        sig.initVerify(keyPair.getPublic());
    }

    /**
     * Test that we can generate an elliptic curve key-pair with a custom
     * encryption curve and that the generated public/private keys are valid.
     *
     * @throws NoSuchAlgorithmException if the provided encryption algorithm is not found
     * @throws InvalidAlgorithmParameterException if the provided encryption parameters are not valid
     * @throws InvalidKeyException if the provided key is not valid
     * @throws SignatureException if the provided public key does not match the signed content
     */
    @Test
    void testGenerateKeyPair() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        KeyPair keyPair = X509Utils.generateKeyPair("secp192r1");

        // Basic Assertions
        assertNotNull(keyPair);
        assertNotNull(keyPair.getPublic());
        assertEquals("EC", keyPair.getPublic().getAlgorithm());
        assertEquals("X.509", keyPair.getPublic().getFormat());
        assertNotNull(keyPair.getPrivate());
        assertEquals("EC", keyPair.getPrivate().getAlgorithm());
        assertEquals("PKCS#8", keyPair.getPrivate().getFormat());

        // Sign using the private key
        Signature sig = Signature.getInstance("SHA256withECDSA");
        sig.initSign(keyPair.getPrivate());
        sig.update(challenge);

        // Verify signature using the public key
        sig.initVerify(keyPair.getPublic());
    }

    /**
     * Test that we can correctly generate an X.509 certificate based on the
     * default arguments, in this case by using the default encryption
     * and hashing algorithms.
     *
     * @throws NoSuchAlgorithmException if the provided encryption algorithm is not found
     * @throws InvalidAlgorithmParameterException if the provided encryption parameters are not valid
     * @throws CertificateException if the certificate is not valid
     * @throws OperatorCreationException if the certificate generation process fails
     */
    @Test
    void testGenerateX509CertificateDefault() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, CertificateException, OperatorCreationException  {
        // Yesterday
        Date validityBeginDate = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
        // Tomorrow
        Date validityEndDate = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000);

        // Create a keypair
        KeyPair keyPair = X509Utils.generateKeyPair(null);

        // Create the certificate
        X509Certificate certificate = X509Utils.generateX509Certificate(keyPair, "CN=Test", validityBeginDate, validityEndDate, null);

        // Basic Assertions
        assertNotNull(certificate);
        assertEquals("SHA256WITHCVC-ECDSA", certificate.getSigAlgName());
        assertEquals("CN=Test", certificate.getIssuerDN().getName());
        assertTrue(certificate.getNotBefore().compareTo(validityBeginDate) <= 0);
        assertTrue(certificate.getNotAfter().compareTo(validityEndDate) <= 0);
        certificate.checkValidity();
    }

    /**
     * Test that we can correctly generate an X.509 certificate based on the
     * provided arguments, in this case by using the SHA256WITHECDSA encryption
     * and hashing algorithms.
     *
     * @throws NoSuchAlgorithmException if the provided encryption algorithm is not found
     * @throws InvalidAlgorithmParameterException if the provided encryption parameters are not valid
     * @throws CertificateException if the certificate is not valid
     * @throws OperatorCreationException if the certificate generation process fails
     */
    @Test
    void testGenerateX509Certificate() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, CertificateException, OperatorCreationException  {
        // Yesterday
        Date validityBeginDate = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
        // Tomorrow
        Date validityEndDate = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000);

        // Create a keypair
        KeyPair keyPair = X509Utils.generateKeyPair(null);

        // Create the certificate
        X509Certificate certificate = X509Utils.generateX509Certificate(keyPair, "CN=Test", validityBeginDate, validityEndDate, "SHA256WITHECDSA");

        // Basic Assertions
        assertNotNull(certificate);
        assertEquals("SHA256WITHECDSA", certificate.getSigAlgName());
        assertEquals("CN=Test", certificate.getIssuerDN().getName());
        assertTrue(certificate.getNotBefore().compareTo(validityBeginDate) <= 0);
        assertTrue(certificate.getNotAfter().compareTo(validityEndDate) <= 0);
        certificate.checkValidity();
    }

    /**
     * Test that we can correctly generate an X.509 certificate signing request
     * based on the default arguments, in this case by using the default
     * encryption and hashing algorithms.
     *
     * @throws InvalidAlgorithmParameterException if the provided encryption parameters are not valid
     * @throws NoSuchAlgorithmException if the provided encryption algorithm is not found
     * @throws OperatorCreationException if the certificate generation process fails
     * @throws PKCSException if the provided signature does not match the CSR
     */
    @Test
    void testGenerateX509CSRDefault() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, OperatorCreationException, PKCSException {
        // Create a keypair
        KeyPair keyPair = X509Utils.generateKeyPair(null);

        // Create the certificate
        PKCS10CertificationRequest csr = X509Utils.generateX509CSR(keyPair, "CN=Test", null);

        // Basic Assertions
        assertNotNull(csr);
        assertEquals("CN=Test", csr.getSubject().toString());

        // Assert the CSR signature validity
        assertTrue(csr.isSignatureValid(new JcaContentVerifierProviderBuilder()
                .build(keyPair.getPublic())));
    }

    /**
     * Test that we can correctly generate an X.509 certificate signing request
     * based on the provided arguments, in this case by using the
     * SHA256WITHECDSA encryption and hashing algorithms.
     *
     * @throws InvalidAlgorithmParameterException if the provided encryption parameters are not valid
     * @throws NoSuchAlgorithmException if the provided encryption algorithm is not found
     * @throws OperatorCreationException if the certificate generation process fails
     * @throws PKCSException if the provided signature does not match the CSR
     */
    @Test
    void testGenerateX509CSR() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, OperatorCreationException, PKCSException {
        // Create a keypair
        KeyPair keyPair = X509Utils.generateKeyPair(null);

        // Create the certificate
        PKCS10CertificationRequest csr = X509Utils.generateX509CSR(keyPair, "CN=Test", "SHA256WITHECDSA");

        // Basic Assertions
        assertNotNull(csr);
        assertEquals("CN=Test", csr.getSubject().toString());

        // Assert the CSR signature validity
        assertTrue(csr.isSignatureValid(new JcaContentVerifierProviderBuilder()
                .build(keyPair.getPublic())));
    }

    /**
     * Test that we can correctly format the generated X.509 certificate from an
     * elliptic curve key pair.
     *
     * @throws NoSuchAlgorithmException if the provided encryption algorithm is not found
     * @throws InvalidAlgorithmParameterException if the provided encryption parameters are not valid
     * @throws CertificateException if the certificate is not valid
     * @throws OperatorCreationException if the certificate generation process fails
     * @throws IOException for error during the PEM exporting operation
     */
    @Test
    void testFormatCrtFileContents() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, CertificateException, OperatorCreationException, IOException {
        // Yesterday
        Date validityBeginDate = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
        // Tomorrow
        Date validityEndDate = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000);

        // Create a keypair
        KeyPair keyPair = X509Utils.generateKeyPair(null);

        // Create the certificate
        X509Certificate certificate = X509Utils.generateX509Certificate(keyPair, "CN=Test", validityBeginDate, validityEndDate, null);

        // Generate the PEM formatted string
        String privateKeyPem = X509Utils.formatCertificate(certificate);

        // Basic Assertions
        assertNotNull(privateKeyPem);
        assertTrue(privateKeyPem.startsWith("-----BEGIN CERTIFICATE-----"));
        assertTrue(privateKeyPem.endsWith("-----END CERTIFICATE-----\n"));
        assertTrue(privateKeyPem.length() > 100);
    }

    /**
     * Test that we can correctly format the generated public key from an
     * elliptic curve key pair.
     *
     * @throws NoSuchAlgorithmException if the provided encryption algorithm is not found
     * @throws InvalidAlgorithmParameterException if the provided encryption parameters are not valid
     * @throws IOException for error during the PEM exporting operation
     */
    @Test
    void testFormatPublicKey() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, IOException {
        // Create a keypair
        KeyPair keyPair = X509Utils.generateKeyPair(null);

        // Generate the PEM formatted string
        String publicKeyPem = X509Utils.formatPublicKey(keyPair);

        // Basic Assertions
        assertNotNull(publicKeyPem);
        assertTrue(publicKeyPem.startsWith("-----BEGIN PUBLIC KEY-----"));
        assertTrue(publicKeyPem.endsWith("-----END PUBLIC KEY-----\n"));
        assertTrue(publicKeyPem.length() > 100);
    }

    /**
     * Test that we can correctly format the generated private key from an
     * elliptic curve key pair.
     *
     * @throws NoSuchAlgorithmException if the provided encryption algorithm is not found
     * @throws InvalidAlgorithmParameterException if the provided encryption parameters are not valid
     * @throws IOException for error during the PEM exporting operation
     */
    @Test
    void testFormatPrivateKey() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, IOException {
        // Create a keypair
        KeyPair keyPair = X509Utils.generateKeyPair(null);

        // Generate the PEM formatted string
        String privateKeyPem = X509Utils.formatPrivateKey(keyPair);

        // Basic Assertions
        assertNotNull(privateKeyPem);
        assertTrue(privateKeyPem.startsWith("-----BEGIN PRIVATE KEY-----"));
        assertTrue(privateKeyPem.endsWith("-----END PRIVATE KEY-----\n"));
        assertTrue(privateKeyPem.length() > 100);
    }

    /**
     * Test that we can reconstruct the public key object from it's PEM
     * representation.
     */
    @Test
    void testPublicKeyFromPem() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        // Create a keypair
        KeyPair keyPair = X509Utils.generateKeyPair(null);

        // Generate the PEM formatted string
        String publicKeyPem = X509Utils.formatPublicKey(keyPair);
        PublicKey publicKey = X509Utils.publicKeyFromPem(publicKeyPem);

        assertNotNull(publicKey);
    }

    /**
     * Test that we can reconstruct the private key object from it's PEM
     * representation.
     */
    @Test
    void testPrivateKeyFromPem() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        // Create a keypair
        KeyPair keyPair = X509Utils.generateKeyPair(null);

        // Generate the PEM formatted string
        String privateKeyPem = X509Utils.formatPrivateKey(keyPair);
        PrivateKey privateKey = X509Utils.privateKeyFromPem(privateKeyPem, null);

        assertNotNull(privateKey);
    }

}