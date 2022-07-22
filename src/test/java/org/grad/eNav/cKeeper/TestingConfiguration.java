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

package org.grad.eNav.cKeeper;

import org.grad.eNav.cKeeper.components.DomainDtoMapper;
import org.grad.eNav.cKeeper.config.GlobalConfig;
import org.grad.eNav.cKeeper.models.domain.Certificate;
import org.grad.eNav.cKeeper.models.domain.MrnEntity;
import org.grad.eNav.cKeeper.models.dtos.CertificateDto;
import org.grad.eNav.cKeeper.models.dtos.MrnEntityDto;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.openfeign.support.PageJacksonModule;
import org.springframework.cloud.openfeign.support.SortJacksonModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * The Test Configuration.
 */
@TestConfiguration
@Import(GlobalConfig.class)
public class TestingConfiguration {

    /**
     * Support for Jackson Page Deserialization.
     */
    @Bean
    public com.fasterxml.jackson.databind.Module pageJacksonModule() {
        return new PageJacksonModule();
    }

    /**
     * Support for Jackson Page Sorting Deserialization.
     */
    @Bean
    public com.fasterxml.jackson.databind.Module sortJacksonModule() {
        return new SortJacksonModule();
    }

    /**
     * MRN Entity Mapper from Domain to DTO.
     */
    @Bean
    public DomainDtoMapper mrnEntityDomainToDtoMapper() {
        return new DomainDtoMapper<MrnEntity, MrnEntityDto>();
    }

    /**
     * MRN Entity Mapper from DTO to Domain.
     */
    @Bean
    public DomainDtoMapper mrnEntityDtoToDomainMapper() {
        return new DomainDtoMapper<MrnEntityDto, MrnEntity>();
    }

    /**
     * MRN Entity Mapper from Domain to DTO.
     */
    @Bean
    public DomainDtoMapper certificateDomainToDtoMapper() {
        return new DomainDtoMapper<Certificate, CertificateDto>();
    }

}
