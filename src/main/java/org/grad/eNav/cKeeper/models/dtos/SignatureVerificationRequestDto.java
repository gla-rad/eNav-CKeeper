package org.grad.eNav.cKeeper.models.dtos;

import java.util.Arrays;

/**
 * The type Signature verification request.
 */
public class SignatureVerificationRequestDto {

    // Class Variables
    private byte[] content;
    private byte[] signature;

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
    public byte[] getContent() {
        return content;
    }

    /**
     * Sets content.
     *
     * @param content the content
     */
    public void setContent(byte[] content) {
        this.content = content;
    }

    /**
     * Get signature byte [ ].
     *
     * @return the byte [ ]
     */
    public byte[] getSignature() {
        return signature;
    }

    /**
     * Sets signature.
     *
     * @param signature the signature
     */
    public void setSignature(byte[] signature) {
        this.signature = signature;
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
        if (o == null || getClass() != o.getClass()) return false;
        SignatureVerificationRequestDto that = (SignatureVerificationRequestDto) o;
        return Arrays.equals(content, that.content) && Arrays.equals(signature, that.signature);
    }

    /**
     * Overrides the hashcode generation of the object.
     *
     * @return the generated hashcode
     */
    @Override
    public int hashCode() {
        int result = Arrays.hashCode(content);
        result = 31 * result + Arrays.hashCode(signature);
        return result;
    }
}
