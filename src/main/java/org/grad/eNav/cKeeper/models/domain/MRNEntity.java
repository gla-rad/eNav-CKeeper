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
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.search.annotations.*;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

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

    @Field()
    @Field(name = "mmsi_sort", analyze = Analyze.NO, normalizer = @Normalizer(definition = "lowercase"))
    @SortableField(forField = "mmsi_sort")
    @Column(name = "mmsi", unique=true)
    private Integer mmsi;

    @OneToMany(mappedBy = "mrnEntity", cascade = CascadeType.REMOVE)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private Set<Certificate> certificates = new HashSet<>();

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
     * Gets certificates.
     *
     * @return the certificates
     */
    public Set<Certificate> getCertificates() {
        return certificates;
    }

    /**
     * Sets certificates.
     *
     * @param certificates the certificates
     */
    public void setCertificates(Set<Certificate> certificates) {
        this.certificates = certificates;
    }

    /**
     * Gets mmsi.
     *
     * @return the mmsi
     */
    public Integer getMmsi() {
        return mmsi;
    }

    /**
     * Sets mmsi.
     *
     * @param mmsi the mmsi
     */
    public void setMmsi(Integer mmsi) {
        this.mmsi = mmsi;
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
