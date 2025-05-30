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

package org.grad.eNav.cKeeper.models.dtos.mcp;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;

/**
 * The MCP Entity Abstract Class
 * <p/>
 * THis is an abstract class to define all the common variables of the MCP
 * hosted entities, such as the
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public abstract class McpEntityBase implements Serializable {

    // Class Variable
    @NotNull
    private BigInteger id;
    @NotNull
    private String idOrganization;
    @NotNull
    private String mrn;
    private String createdAt;
    private String updatedAt;
    private List<McpCertitifateDto> certificates;

    /**
     * Instantiates a new Mcp entity dto.
     */
    public McpEntityBase() {
    }

    /**
     * Instantiates a new Mcp entity dto.
     *
     * @param mrn the mrn
     */
    public McpEntityBase(String mrn) {
        this.mrn = mrn;
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
     * Gets id organization.
     *
     * @return the id organization
     */
    public String getIdOrganization() {
        return idOrganization;
    }

    /**
     * Sets id organization.
     *
     * @param idOrganization the id organization
     */
    public void setIdOrganization(String idOrganization) {
        this.idOrganization = idOrganization;
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
     * Gets created at.
     *
     * @return the created at
     */
    public String getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets created at.
     *
     * @param createdAt the created at
     */
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Gets updated at.
     *
     * @return the updated at
     */
    public String getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets updated at.
     *
     * @param updatedAt the updated at
     */
    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Gets certificates.
     *
     * @return the certificates
     */
    public List<McpCertitifateDto> getCertificates() {
        return certificates;
    }

    /**
     * Sets certificates.
     *
     * @param certificates the certificates
     */
    public void setCertificates(List<McpCertitifateDto> certificates) {
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
        if (!(o instanceof McpEntityBase)) return false;
        McpEntityBase that = (McpEntityBase) o;
        return Objects.equals(mrn, that.mrn);
    }

    /**
     * Overrides the hashcode generation of the object.
     *
     * @return the generated hashcode
     */
    @Override
    public int hashCode() {
        return Objects.hash(mrn);
    }
}
