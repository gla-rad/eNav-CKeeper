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

package org.grad.eNav.cKeeper.models.dtos;

import org.grad.eNav.cKeeper.models.domain.Certificate;

import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;

/**
 * The type Certificate DTO Class.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class CertificateDto {

    // Class Variables
    private BigInteger id;
    private BigInteger mrnEntityId;
    private String publicKey;
    private Boolean revoked;

    /**
     * Instantiates a new Certificate dto.
     */
    public CertificateDto() {

    }

    /**
     * Instantiates a new Certificate dto.
     *
     * @param certificate The certificate domain object to construct the DTO from
     */
    public CertificateDto(Certificate certificate) {
        this.id = certificate.getId();
        this.mrnEntityId = certificate.getMrnEntity().getId();
        this.publicKey = certificate.getPublicKey();
        this.revoked = Optional.of(certificate)
                .map(Certificate::isRevoked)
                .orElse(Boolean.FALSE);
    }

    /**
     * Gets id.
     *
     * @return the id
     */
    public BigInteger getId() {
        return id;
    }

    /**
     * Sets id.
     *
     * @param id the id
     */
    public void setId(BigInteger id) {
        this.id = id;
    }

    /**
     * Gets mrn entity id.
     *
     * @return the mrn entity id
     */
    public BigInteger getMrnEntityId() {
        return mrnEntityId;
    }

    /**
     * Sets mrn entity id.
     *
     * @param mrnEntityId the mrn entity id
     */
    public void setMrnEntityId(BigInteger mrnEntityId) {
        this.mrnEntityId = mrnEntityId;
    }

    /**
     * Gets public key.
     *
     * @return the public key
     */
    public String getPublicKey() {
        return publicKey;
    }

    /**
     * Sets public key.
     *
     * @param publicKey the public key
     */
    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }


    /**
     * Is revoked boolean.
     *
     * @return the boolean
     */
    public Boolean isRevoked() {
        return revoked;
    }

    /**
     * Sets revoked.
     *
     * @param revoked the revoked
     */
    public void setRevoked(Boolean revoked) {
        this.revoked = revoked;
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
        if (!(o instanceof CertificateDto)) return false;
        CertificateDto that = (CertificateDto) o;
        return id.equals(that.id) && mrnEntityId.equals(that.mrnEntityId);
    }

    /**
     * Overrides the hashcode generation of the object.
     *
     * @return the generated hashcode
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, mrnEntityId);
    }

}
