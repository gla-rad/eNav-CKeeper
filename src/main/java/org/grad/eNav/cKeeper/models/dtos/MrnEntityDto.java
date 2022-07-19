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

import org.grad.eNav.cKeeper.models.domain.MrnEntity;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.math.BigInteger;
import java.util.Objects;

/**
 * The type MRN Entity DTO Class.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class MrnEntityDto {

    // Class Variables
    private BigInteger id;
    @NotNull
    private String name;
    @NotNull
    private String mrn;
    @Pattern(regexp="^(0|[1-9][0-9]*)$")
    private String mmsi;

    /**
     * Instantiates a new Mrn entity dto.
     */
    public MrnEntityDto() {

    }

    /**
     * Instantiates a new Mrn entity dto.
     *
     * @param name the name
     * @param mrn  the mrn
     */
    public MrnEntityDto(String name, String mrn, String mmsi) {
        this.name = name;
        this.mrn = mrn;
        this.mmsi = mmsi;
    }

    /**
     * Instantiates a new Mrn entity dto.
     *
     * @param mrnEntity the mrn entity
     */
    public MrnEntityDto(MrnEntity mrnEntity) {
        // Sanity Check
        if(Objects.isNull(mrnEntity)) {
            return;
        }

        // Otherwise, populate the fields from the MRN entity domain object
        this.id = mrnEntity.getId();
        this.name = mrnEntity.getName();
        this.mrn = mrnEntity.getMrn();
        this.mmsi = mrnEntity.getMmsi();
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
     * Overrides the equality operator of the class.
     *
     * @param o the object to check the equality
     * @return whether the two objects are equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MrnEntityDto)) return false;
        MrnEntityDto that = (MrnEntityDto) o;
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
    public MrnEntity toMRNEntity() {
        MrnEntity entity = new MrnEntity();
        entity.setId(this.id);
        entity.setName(this.name);
        entity.setMrn(this.mrn);
        entity.setMmsi(this.mmsi);
        return entity;
    }

}
