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

package org.grad.eNav.cKeeper.models.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.*;
import org.hibernate.annotations.Cache;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.math.BigInteger;
import java.util.Objects;

/**
 * The type Certificate.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Entity
@Table(name = "certificates")
@Cacheable
public class Certificate {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private BigInteger id;

    @JsonIgnore
    @ManyToOne()
    @JoinColumn(name = "mrnEntityId", nullable = false)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private MRNEntity mrnEntity;

    @NotNull
    @Type(type="text")
    @Column(name = "certificate")
    private String certificate;

    @NotNull
    @Type(type="text")
    @Column(name = "publicKey")
    private String publicKey;

    @NotNull
    @Type(type="text")
    @Column(name = "privateKey")
    private String privateKey;

    @Column(name = "revoked")
    private Boolean revoked;

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
     * Gets mrn entity.
     *
     * @return the mrn entity
     */
    public MRNEntity getMrnEntity() {
        return mrnEntity;
    }

    /**
     * Sets mrn entity.
     *
     * @param mrnEntity the mrn entity
     */
    public void setMrnEntity(MRNEntity mrnEntity) {
        this.mrnEntity = mrnEntity;
    }

    /**
     * Gets certificate.
     *
     * @return the certificate
     */
    public String getCertificate() {
        return certificate;
    }

    /**
     * Sets certificate.
     *
     * @param certificate the certificate
     */
    public void setCertificate(String certificate) {
        this.certificate = certificate;
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
     * Gets private key.
     *
     * @return the private key
     */
    public String getPrivateKey() {
        return privateKey;
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
     * Sets private key.
     *
     * @param privateKey the private key
     */
    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Certificate)) return false;
        Certificate that = (Certificate) o;
        return id.equals(that.id) && mrnEntity.equals(that.mrnEntity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, mrnEntity);
    }
}
