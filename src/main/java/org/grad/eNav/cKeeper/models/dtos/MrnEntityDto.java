/*
 * Copyright (c) 2024 GLA Research and Development Directorate
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.grad.eNav.cKeeper.models.dtos;

import org.grad.eNav.cKeeper.models.domain.MrnEntity;
import org.grad.eNav.cKeeper.models.domain.mcp.McpEntityType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
    @NotNull
    private McpEntityType entityType;
    private String version;

    /**
     * Instantiates a new Mrn entity dto.
     */
    public MrnEntityDto() {

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
        this.entityType = mrnEntity.getEntityType();
        this.version = mrnEntity.getVersion();
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


}
