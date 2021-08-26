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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

/**
 * Utility class for X509 Certificate generation.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Slf4j
public class X509Utils {

    /**
     * Generate a public/private elliptic cryptography key-pair. The encryption
     * algorithm is always ECDSA by the curve can be provided as a parameter,
     * otherwise the default "secp256r1" mode will be chosen.
     *
     * @param curve         The curve to be used for generating the pair - leave null for default
     * @return the generates key-pair
     * @throws NoSuchAlgorithmException if the provided algorithm does not exist
     * @throws InvalidAlgorithmParameterException if the provided algorithm parameters are not valid
     */
    public static KeyPair generateKeyPair(String curve) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        final ECGenParameterSpec spec = new ECGenParameterSpec(Optional.ofNullable(curve).orElse("secp256r1"));
        keyPairGenerator.initialize(spec, new SecureRandom());
        return keyPairGenerator.generateKeyPair();
    }

    /**
     * Generates a new X.509 Certificate base don the provided arguments:
     * <ul>
     *     <li>Key Pair</li>
     *     <li>X500 Name</li>
     *     <li>Start Date</li>
     *     <li>End Date</li>
     *     <li>Optional Algorithm Name</li>
     * </ul>
     * The generated certificate is self-signed and should be used carefully.
     *
     * @param keyPair       The key-pair to wrap into the certificate
     * @param dirName       The X500 name of the certificate
     * @param startDate     The validity start date of the certificate
     * @param endDate       The validity end date of the certificate
     * @param algorithm     The optional algorithm name to be used - leave NULL for default
     * @return A wrapped certificate with constant name
     * @throws CertificateException if the certificate generation fails
     * @throws OperatorCreationException if the content signing operation fails
     */
    public static X509Certificate generateX509Certificate(KeyPair keyPair, String dirName, Date startDate, Date endDate, String algorithm) throws CertificateException, OperatorCreationException {
        // Sanity Checks
        assert Objects.nonNull(keyPair);
        assert StringUtils.isNotBlank(dirName);
        assert Objects.nonNull(startDate);
        assert Objects.nonNull(endDate);

        // Create the Issuer/Subject Name
        X500Name name = new X500Name(dirName);

        // Setup the X.509 Certificate Builder
        final X509v3CertificateBuilder builder = new X509v3CertificateBuilder(name,
                new BigInteger(10, new SecureRandom()), //Choose something better for real use
                startDate,
                endDate,
                name,
                SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded())
        );

        // Setup the Content Signer
        ContentSigner signer = new JcaContentSignerBuilder(Optional.ofNullable(algorithm).orElse("SHA256withCVC-ECDSA"))
                .setProvider(new BouncyCastleProvider())
                .build(keyPair.getPrivate());

        // And sign the self-signed certificate
        final X509CertificateHolder holder = builder.build(signer);

        // Return the certificate using Bouncy Castle
        return new JcaX509CertificateConverter()
                .setProvider(new BouncyCastleProvider())
                .getCertificate(holder);
    }

    /**
     * Generates a certificate signing request (CSR) that can be then sent to
     * a certificate provider to be signed and returned as an actual X.509
     * certificate. In our case this is primarily the MCP MIR.
     *
     * @param keyPair       The key-pair to wrap into the certificate
     * @param dirName       The X500 name of the certificate
     * @param algorithm     The optional algorithm name to be used - leave NULL for default
     * @return The generated certificate signing request
     * @throws OperatorCreationException if the content signing operation fails
     */
    public static PKCS10CertificationRequest generateX509CSR(KeyPair keyPair, String dirName, String algorithm) throws OperatorCreationException {
        // Sanity Checks
        assert Objects.nonNull(keyPair);
        assert StringUtils.isNotBlank(dirName);

        // Create the Issuer/Subject Name
        final X500Principal principal = new X500Principal(dirName);

        // Setup the Certificate Request Builder
        final PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(
                principal, keyPair.getPublic());
        
        // Setup the Content Signer
        final ContentSigner signer = new JcaContentSignerBuilder(Optional.ofNullable(algorithm).orElse("SHA256withCVC-ECDSA")).
                setProvider(new BouncyCastleProvider())
                .build(keyPair.getPrivate());

        // Return the certificate using Bouncy Castle
        return p10Builder.build(signer);
    }

    /**
     * Returns the provided certificate in a PEM string format using the
     * Bouncy Castle PEMWriter functionality.
     *
     * @param certificate   The certificate to be formatted
     * @return the PEM formatted string of the certificate
     * @throws IOException if the string export operation fails
     */
    public static String formatCertificate(final X509Certificate certificate) throws IOException {
        final StringWriter certWriter = new StringWriter();
        final JcaPEMWriter pemCertWriter = new JcaPEMWriter(certWriter);
        pemCertWriter.writeObject(certificate);
        pemCertWriter.flush();
        return certWriter.toString();
    }

    /**
     * Returns the provided certificate signing request in a PEM string format
     * using the Bouncy Castle PEMWriter functionality.
     *
     * @param csr           The certificate signing request to be formatted
     * @return the PEM formatted string of the certificate
     * @throws IOException if the string export operation fails
     */
    public static String formatCSR(final PKCS10CertificationRequest csr) throws IOException {
        final StringWriter certWriter = new StringWriter();
        final JcaPEMWriter pemCertWriter = new JcaPEMWriter(certWriter);
        pemCertWriter.writeObject(csr);
        pemCertWriter.flush();
        return certWriter.toString();
    }

    /**
     * Returns the provided key-pair public key in a PEM string format using the
     * Bouncy Castle PEMWriter functionality.
     *
     * @param keyPair   The key-pair to be formatted
     * @return the PEM formatted string of the key-pair public key
     * @throws IOException if the string export operation fails
     */
    public static String formatPublicKey(final KeyPair keyPair) throws IOException {
        final StringWriter keyWriter = new StringWriter();
        final JcaPEMWriter pemKeyWriter = new JcaPEMWriter(keyWriter);
        pemKeyWriter.writeObject(keyPair.getPublic());
        pemKeyWriter.flush();
        return keyWriter.toString();
    }

    /**
     * Returns the provided key-pair private key in a PEM string format using the
     * Bouncy Castle PEMWriter functionality.
     *
     * @param keyPair   The key-pair to be formatted
     * @return the PEM formatted string of the key-pair private key
     * @throws IOException if the string export operation fails
     */
    public static String formatPrivateKey(final KeyPair keyPair) throws IOException {
        final StringWriter keyWriter = new StringWriter();
        final JcaPEMWriter pemKeyWriter = new JcaPEMWriter(keyWriter);
        /*
             Generating the PEM directly uses the SEC1 format which cannot be correctly read.
             Therefore, we better do this in PKCS8  format.
             For more info: https://stackoverflow.com/questions/61589895/unable-to-read-bouncycastle-generated-privatekey-in-java
         */
        //pemKeyWriter.writeObject(keyPair.getPrivate(), null);
        pemKeyWriter.writeObject(new JcaPKCS8Generator(keyPair.getPrivate(), null));
        pemKeyWriter.flush();
        return keyWriter.toString();
    }

    /**
     * Retrieves the public key object as generated from the Bouncy Castle
     * library from it's PEM string representation.
     *
     * @param publicKeyPem     The public key PEM representation
     * @return The public key object
     * @throws NoSuchAlgorithmException if the key factory algorithm doesn't exist
     * @throws InvalidKeySpecException if the key provided is invalid
     * @throws IOException for exceptions while accessing the public key
     */
    public static PublicKey publicKeyFromPem(String publicKeyPem) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        final KeyFactory factory = KeyFactory.getInstance("EC");
        final StringReader stringReader = new StringReader(publicKeyPem);
        final PemReader pemReader = new PemReader(stringReader);
        final PemObject pemObject = pemReader.readPemObject();
        final X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(pemObject.getContent());
        return factory.generatePublic(publicKeySpec);
    }

    /**
     * Retrieves the private key object as generated from the Bouncy Castle
     * library from it's PEM string representation. The encryption
     *  algorithm is always ECDSA by the curve can be provided as a parameter,
     *  otherwise the default "secp256r1" mode will be chosen.
     *
     * @param privateKeyPem     The private key PEM representation
     * @param curve             The curve to be used for reading the private key - leave blank for default
     * @return The private key object
     * @throws NoSuchAlgorithmException if the key factory algorithm doesn't exist
     * @throws InvalidKeySpecException if the key provided is invalid
     * @throws IOException for exceptions while accessing the public key
     */
    public static PrivateKey privateKeyFromPem(String privateKeyPem, String curve) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        final KeyFactory factory = KeyFactory.getInstance("EC");
        final StringReader stringReader = new StringReader(privateKeyPem);
        final PemReader pemReader = new PemReader(stringReader);
        final PemObject pemObject = pemReader.readPemObject();
        // The ECPrivateKeySpec loading of the key doesn't return the correct key for some reason!
        /*
            The ECPrivateKeySpec loading of the key doesn't return the correct key for some reason!
            From more info: https://stackoverflow.com/questions/61589895/unable-to-read-bouncycastle-generated-privatekey-in-java
         */
        //ECParameterSpec spec = ECNamedCurveTable.getParameterSpec(Optional.ofNullable(curve).orElse("secp256r1"));
        //ECPrivateKeySpec ecPrivateKeySpec = new ECPrivateKeySpec(new BigInteger(1, pemObject.getContent()), spec);
        final PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(pemObject.getContent());
        return factory.generatePrivate(pkcs8EncodedKeySpec);
    }

}
