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

import org.grad.eNav.cKeeper.models.domain.mcp.McpEntityType;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.*;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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
public class MrnEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @ScaledNumberField(name = "id_sort", decimalScale=0, sortable = Sortable.YES)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "mrn_entity_generator")
    @SequenceGenerator(name = "mrn_entity_generator", sequenceName = "mrn_entity_seq", allocationSize = 1)
    private BigInteger id;

    @NotNull
    @KeywordField(sortable = Sortable.YES)
    @Column(name = "name", unique=true)
    private String name;

    @NotNull
    @KeywordField(sortable = Sortable.YES)
    @Column(name = "mrn", unique=true)
    private String mrn;

    @KeywordField(sortable = Sortable.YES)
    @Column(name = "mmsi", unique=true)
    private String mmsi;

    @NotNull
    @KeywordField(normalizer = "lowercase", sortable = Sortable.YES)
    @Enumerated(EnumType.STRING)
    @Column(name = "entityType", nullable = false)
    private McpEntityType entityType;

    @NotNull
    @KeywordField(normalizer = "lowercase", sortable = Sortable.YES)
    @Column(name = "version")
    private String version;

    @OneToMany(mappedBy = "mrnEntity", cascade = CascadeType.REMOVE)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private Set<Certificate> certificates = new HashSet<>();

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
     * Gets mmsi.
     *
     * @return the mmsi
     */
    public String getMmsi() {
        return mmsi;
    }

    /**
     * Sets mmsi.
     *
     * @param mmsi the mmsi
     */
    public void setMmsi(String mmsi) {
        this.mmsi = mmsi;
    }

    /**
     * Gets entity type.
     *
     * @return the entity type
     */
    public McpEntityType getEntityType() {
        return entityType;
    }

    /**
     * Sets entity type.
     *
     * @param entityType the entity type
     */
    public void setEntityType(McpEntityType entityType) {
        this.entityType = entityType;
    }

    /**
     * Gets version.
     *
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets version.
     *
     * @param version the version
     */
    public void setVersion(String version) {
        this.version = version;
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
     * Overrides the equality operator of the class.
     *
     * @param o the object to check the equality
     * @return whether the two objects are equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MrnEntity)) return false;
        MrnEntity entity = (MrnEntity) o;
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
