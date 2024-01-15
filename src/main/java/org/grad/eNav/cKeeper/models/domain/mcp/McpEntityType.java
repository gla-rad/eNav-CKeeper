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

package org.grad.eNav.cKeeper.models.domain.mcp;

import com.fasterxml.jackson.annotation.JsonValue;
import org.grad.eNav.cKeeper.models.dtos.mcp.*;

import java.util.Arrays;

public enum McpEntityType {
    DEVICE("device", McpDeviceDto.class),
    SERVICE("service", McpServiceDto.class),
    USER("user", McpUserDto.class),
    VESSEL("vessel", McpVesselDto.class),
    ROLE("role", McpRoleDto.class);

    // Enum Variables
    private final String value;
    private Class<? extends McpEntityBase> entityClass;

    /**
     * Enum Constructor
     *
     * @param value the enum value
     */
    McpEntityType(final String value, Class<? extends McpEntityBase> entityClass) {
        this.value = value;
        this.entityClass = entityClass;
    }

    /**
     * Gets value.
     *
     * @return the value
     */
    @JsonValue
    public String getValue() { return value; }

    /**
     * Gets entity class.
     *
     * @return the entity class
     */
    public Class<? extends McpEntityBase> getEntityClass() {
        return entityClass;
    }

    /**
     * Find the enum entry that corresponds to the provided value.
     *
     * @param value the enum value
     * @return The respective enum entry
     */
    public static McpEntityType fromValue(String value) {
        return Arrays.stream(McpEntityType.values())
                .filter(t -> t.getValue().compareTo(value)==0)
                .findFirst()
                .orElse(null);
    }

    /**
     * Find the enum entry that corresponds to the provided entity Class.
     *
     * @param entityClass the entity class
     * @return The respective enum entry
     */
    public static McpEntityType fromEntityClass(Class<? extends McpEntityBase> entityClass) {
        return Arrays.stream(McpEntityType.values())
                .filter(t -> t.getEntityClass().equals(entityClass))
                .findFirst()
                .orElse(null);
    }
}
