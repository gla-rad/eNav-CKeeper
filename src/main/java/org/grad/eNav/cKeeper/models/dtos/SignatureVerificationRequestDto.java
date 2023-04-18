package org.grad.eNav.cKeeper.models.dtos;

import java.util.Objects;

/**
 * The type Signature verification request.
 *
 * Note that the class variables are expected to b4e Base64 encoded.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class SignatureVerificationRequestDto {

    // Class Variables
    private String content;
    private String signature;
    private String algorithm;

    /**
     * Instantiates a new Signature verification request.
     */
    public SignatureVerificationRequestDto() {
    }

    /**
     * Get content byte [ ].
     *
     * @return the byte [ ]
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets content.
     *
     * @param content the content
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Get signature byte [ ].
     *
     * @return the byte [ ]
     */
    public String getSignature() {
        return signature;
    }

    /**
     * Sets signature.
     *
     * @param signature the signature
     */
    public void setSignature(String signature) {
        this.signature = signature;
    }

    /**
     * Gets algorithm.
     *
     * @return the algorithm
     */
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * Sets algorithm.
     *
     * @param algorithm the algorithm
     */
    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    /**
     * Overrides the equality operator of the class.
     *
     * @param o the object to check the equality
     * @return whether the two objects are equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SignatureVerificationRequestDto that)) return false;
        return Objects.equals(content, that.content) && Objects.equals(signature, that.signature) && Objects.equals(algorithm, that.algorithm);
    }

    /**
     * Overrides the hashcode generation of the object.
     *
     * @return the generated hashcode
     */
    @Override
    public int hashCode() {
        return Objects.hash(content, signature, algorithm);
    }
}
