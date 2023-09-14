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

package org.grad.eNav.cKeeper.repos;

import org.grad.eNav.cKeeper.models.domain.MrnEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigInteger;
import java.util.Optional;

/**
 * Spring Data JPA repository for the MRN Entity.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public interface MRNEntityRepo extends JpaRepository<MrnEntity, BigInteger> {

    /**
     * Find one using the Entity name.
     *
     * @param name the name of the entity
     * @return The Entity matching the name
     */
    Optional<MrnEntity> findByName(String name);

    /**
     * Find one using the Entity name and the version.
     *
     * @param name the name of the entity
     * @param version the version of the service entity
     * @return The Entity matching the name and the version
     */
    Optional<MrnEntity> findByNameAndVersion(String name, String version);

    /**
     * Find one using the Entity MRN.
     *
     * @param mrn the MRN of the entity
     * @return The Entity matching the MRN
     */
    Optional<MrnEntity> findByMrn(String mrn);

    /**
     * Find one using the Entity MRN and the version.
     *
     * @param mrn the MRN of the entity
     * @param version the version of the service entity
     * @return The Entity matching the name and the version
     */
    Optional<MrnEntity> findByMrnAndVersion(String mrn, String version);

    /**
     * Find one using the Entity MMSI.
     *
     * @param mmsi the MMSI of the entity
     * @return The Entity matching the MMSI
     */
    Optional<MrnEntity> findByMmsi(String mmsi);

}
