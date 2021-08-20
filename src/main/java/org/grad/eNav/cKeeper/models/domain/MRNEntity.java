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

import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.hibernate.annotations.Type;
import org.hibernate.search.annotations.*;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Objects;

/**
 * The type MRN Entity.
 */
@Entity
@Table(name = "mrn_entity")
@Cacheable
@Indexed
@NormalizerDef(name = "lowercase", filters = @TokenFilterDef(factory = LowerCaseFilterFactory.class))
public class MRNEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Field(name = "id_sort", analyze = Analyze.NO, normalizer = @Normalizer(definition = "lowercase"))
    @SortableField(forField = "id_sort")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private BigInteger id;

    @NotNull
    @Field()
    @Field(name = "name_sort", analyze = Analyze.NO, normalizer = @Normalizer(definition = "lowercase"))
    @SortableField(forField = "name_sort")
    @Column(name = "name")
    private String name;

    @NotNull
    @Field()
    @Field(name = "mrn_sort", analyze = Analyze.NO, normalizer = @Normalizer(definition = "lowercase"))
    @SortableField(forField = "mrn_sort")
    @Column(name = "mrn")
    private String mrn;

    @Type(type="text")
    @Column(name = "certificate")
    private String certificate;

    @Type(type="text")
    @Column(name = "publicKey")
    private String publicKey;

    @Type(type="text")
    @Column(name = "privateKey")
    private String privateKey;

    /**
     * Instantiates a new Entity.
     */
    public MRNEntity() {
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
        if (!(o instanceof MRNEntity)) return false;
        MRNEntity entity = (MRNEntity) o;
        return Objects.equals(id, entity.id) && mrn.equals(entity.mrn);
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

}
