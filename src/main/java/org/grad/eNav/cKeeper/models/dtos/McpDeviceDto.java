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

import javax.validation.constraints.NotNull;
import java.math.BigInteger;
import java.util.Objects;

/**
 * The MCP Device DTO Class.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class McpDeviceDto {

    // Class Variable
    private BigInteger id;
    private String idOrganization;
    @NotNull
    private String mrn;
    @NotNull
    private String name;
    private String permissions;
    private String createdAt;
    private String updatedAt;
    private String homeMMSUrl;

    /**
     * Instantiates a new Mcp device dto.
     */
    public McpDeviceDto() {

    }

    /**
     * Instantiates a new Mcp device dto.
     *
     * @param name the name
     * @param mrn  the mrn
     */
    public McpDeviceDto(String name, String mrn) {
        this.name = name;
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
     * Gets permissions.
     *
     * @return the permissions
     */
    public String getPermissions() {
        return permissions;
    }

    /**
     * Sets permissions.
     *
     * @param permissions the permissions
     */
    public void setPermissions(String permissions) {
        this.permissions = permissions;
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
     * Gets home mms url.
     *
     * @return the home mms url
     */
    public String getHomeMMSUrl() {
        return homeMMSUrl;
    }

    /**
     * Sets home mms url.
     *
     * @param homeMMSUrl the home mms url
     */
    public void setHomeMMSUrl(String homeMMSUrl) {
        this.homeMMSUrl = homeMMSUrl;
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
        if (!(o instanceof McpDeviceDto)) return false;
        McpDeviceDto deviceDto = (McpDeviceDto) o;
        return mrn.equals(deviceDto.mrn) && name.equals(deviceDto.name);
    }

    /**
     * Overrides the hashcode generation of the object.
     *
     * @return the generated hashcode
     */
    @Override
    public int hashCode() {
        return Objects.hash(mrn, name);
    }
}
