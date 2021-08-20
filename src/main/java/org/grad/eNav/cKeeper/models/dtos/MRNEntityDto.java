/*
 * Copyright (c) 2021 GLA Research and Development Directorate
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.grad.eNav.cKeeper.models.dtos;

import org.grad.eNav.cKeeper.models.domain.MRNEntity;

import javax.validation.constraints.NotNull;
import java.math.BigInteger;
import java.util.Objects;

/**
 * The type Mrn entity dto.
 */
public class MRNEntityDto {

    // Class Variables
    private BigInteger id;
    @NotNull
    private String name;
    @NotNull
    private String mrn;
    private String certificate;
    private String publicKey;
    private String privateKey;

    /**
     * Instantiates a new Mrn entity dto.
     */
    public MRNEntityDto() {

    }

    /**
     * Instantiates a new Mrn entity dto.
     *
     * @param mrnEntity the mrn entity
     */
    public MRNEntityDto(MRNEntity mrnEntity) {
        this.id = mrnEntity.getId();
        this.name = mrnEntity.getName();
        this.mrn = mrnEntity.getMrn();
        this.certificate = mrnEntity.getCertificate();
        this.publicKey = mrnEntity.getPublicKey();
        this.privateKey = mrnEntity.getPrivateKey();
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
     * Gets name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets name.
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets mrn.
     *
     * @return the mrn
     */
    public String getMrn() {
        return mrn;
    }

    /**
     * Sets mrn.
     *
     * @param mrn the mrn
     */
    public void setMrn(String mrn) {
        this.mrn = mrn;
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
     * Sets private key.
     *
     * @param privateKey the private key
     */
    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
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
        if (!(o instanceof MRNEntityDto)) return false;
        MRNEntityDto that = (MRNEntityDto) o;
        return Objects.equals(id, that.id) && mrn.equals(that.mrn);
    }

    /**
     * Overrides the hashcode generation of the object.
     *
     * @return the generated hashcode
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, mrn);
    }

    /**
     * Translated the MRN Entiry DTO object to a domain MRN Entity.
     *
     * @return The corresponding MRN entity object
     */
    public MRNEntity toMRNEntity() {
        MRNEntity entity = new MRNEntity();
        entity.setId(this.id);
        entity.setName(this.name);
        entity.setMrn(this.mrn);
        entity.setCertificate(this.certificate);
        entity.setPublicKey(this.publicKey);
        entity.setPrivateKey(this.privateKey);
        return entity;
    }

}
