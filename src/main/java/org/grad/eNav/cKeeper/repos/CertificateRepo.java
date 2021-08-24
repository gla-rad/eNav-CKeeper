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

import org.grad.eNav.cKeeper.models.domain.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigInteger;
import java.util.Set;

/**
 * Spring Data JPA repository for the Certificate.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public interface CertificateRepo extends JpaRepository<Certificate, BigInteger> {

    /**
     * Find all using the MRN Entity Id.
     *
     * @return The ID of the MRN Entity to get the certificates for
     */
    Set<Certificate> findAllByMrnEntityId(BigInteger mrnEntityId);

}
