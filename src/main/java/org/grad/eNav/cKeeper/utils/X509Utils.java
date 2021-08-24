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
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
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
     * @return the generates key-pair
     * @throws NoSuchAlgorithmException if the provided algorithm does not exist
     * @throws InvalidAlgorithmParameterException if the provided algorithm parameters are not valid
     */
    public static KeyPair generateKeyPair(String curve) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(new ECGenParameterSpec(Optional.ofNullable(curve).orElse("secp256r1")), new SecureRandom());
        return keyPairGenerator.generateKeyPair();
    }

    /**
     * It's annoying to have to wrap KeyPairs with Certificates, but this is
     * "easier" for you to know who the key belongs to.
     *
     * @param keyPair A KeyPair to wrap
     * @return A wrapped certificate with constant name
     * @throws CertificateException
     * @throws OperatorCreationException
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
        ContentSigner signer = new JcaContentSignerBuilder(Optional.ofNullable(algorithm).orElse("SHA256withCVC-ECDSA")).
                setProvider(new BouncyCastleProvider())
                .build(keyPair.getPrivate());

        // And sign the self-signed certificate
        final X509CertificateHolder holder = builder.build(signer);

        // Return the certificate using Bouncy Castle
        return new JcaX509CertificateConverter()
                .setProvider(new BouncyCastleProvider())
                .getCertificate(holder);
    }

    /**
     * Returns the provided certificate in a PEM string format using the
     * Bouncy Castle PEMWriter functionality.
     *
     * @param certificate   The certificate to be formatted
     * @return the PEM formatted string of the certificate
     * @throws IOException if the string export operation fails
     */
    public static String formatCrtFileContents(final X509Certificate certificate) throws IOException {
        final StringWriter certWriter = new StringWriter();
        final JcaPEMWriter pemCertWriter = new JcaPEMWriter(certWriter);
        pemCertWriter.writeObject(certificate);
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
        pemKeyWriter.writeObject(keyPair.getPrivate());
        pemKeyWriter.flush();
        return keyWriter.toString();
    }

}
