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

package org.grad.eNav.cKeeper.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.grad.eNav.cKeeper.exceptions.DataNotFoundException;
import org.grad.eNav.cKeeper.models.dtos.CertificateDto;
import org.grad.eNav.cKeeper.models.dtos.MrnEntityDto;
import org.grad.eNav.cKeeper.models.dtos.datatables.*;
import org.grad.eNav.cKeeper.services.CertificateService;
import org.grad.eNav.cKeeper.services.MrnEntityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(controllers = MrnEntityController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
class MrnEntityControllerTest {

    /**
     * The Mock MVC.
     */
    @Autowired
    MockMvc mockMvc;

    /**
     * The JSON Object Mapper.
     */
    @Autowired
    ObjectMapper objectMapper;

    /**
     * The MRN Entity Service mock.
     */
    @MockBean
    MrnEntityService mrnEntityService;

    /**
     * The Certificate Service mock.
     */
    @MockBean
    CertificateService certificateService;

    // Test Variables
    private List<MrnEntityDto> entities;
    private Pageable pageable;
    private MrnEntityDto newEntity;
    private MrnEntityDto existingEntity;
    private CertificateDto certificateDto;

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setUp() {
        // Initialise the MRN entities list
        this.entities = new ArrayList<>();
        for(long i=0; i<10; i++) {
            MrnEntityDto entity = new MrnEntityDto();
            entity.setId(BigInteger.valueOf(i));
            entity.setName("Entity Name");
            entity.setMrn("urn:mrn:mcp:device:mcc:grad:test" + i);
            this.entities.add(entity);
        }

        // Create a pageable definition
        this.pageable = PageRequest.of(0, 5);

        // Create a new MRN entity
        this.newEntity = new MrnEntityDto();
        this.newEntity.setName("New Entity Name");
        this.newEntity.setMrn("urn:mrn:mcp:device:mcc:grad:test-new");

        // Create an MRN entity with an ID
        this.existingEntity = new MrnEntityDto();
        this.existingEntity.setId(BigInteger.ONE);
        this.existingEntity.setName("Existing Entity Name");
        this.existingEntity.setMrn("urn:mrn:mcp:device:mcc:grad:test-existing");

        // Create a certificate to be assigned to the existing MRN entity
        this.certificateDto = new CertificateDto();
        this.certificateDto.setId(BigInteger.ONE);
        this.certificateDto.setMrnEntityId(this.existingEntity.getId());
        this.certificateDto.setPublicKey("PUBLIC KEY");
        this.certificateDto.setStartDate(new Date());
        this.certificateDto.setEndDate(new Date());
        this.certificateDto.setRevoked(Boolean.FALSE);
    }

    /**
     * Test that we can retrieve all the MRN entities currently in the database
     * in a paged result.
     */
    @Test
    void testGetMrnEntities() throws Exception {
        // Created a result page to be returned by the mocked service
        Page<MrnEntityDto> page = new PageImpl<>(this.entities.subList(0, 5), this.pageable, this.entities.size());
        doReturn(page).when(this.mrnEntityService).findAll(any());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(get("/api/mrn-entities"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        // Parse and validate the response
        MrnEntityDto[] result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), MrnEntityDto[].class);
        assertEquals(5, Arrays.asList(result).size());
    }

    /**
     * Test that the API supports the jQuery Datatables server-side paging
     * and search requests.
     */
    @Test
    void testGetMrnEntitiesForDatatables() throws Exception {
        // Create a test datatables paging request
        DtColumn dtColumn = new DtColumn("id");
        dtColumn.setName("ID");
        dtColumn.setOrderable(true);
        DtOrder dtOrder = new DtOrder();
        dtOrder.setColumn(0);
        dtOrder.setDir(DtDirection.asc);
        DtPagingRequest dtPagingRequest = new DtPagingRequest();
        dtPagingRequest.setStart(0);
        dtPagingRequest.setLength(this.entities.size());
        dtPagingRequest.setDraw(1);
        dtPagingRequest.setSearch(new DtSearch());
        dtPagingRequest.setOrder(Collections.singletonList(dtOrder));
        dtPagingRequest.setColumns(Collections.singletonList(dtColumn));

        // Create a mocked datatables paging response
        DtPage<MrnEntityDto> dtPage = new DtPage<>();
        dtPage.setData(this.entities);
        dtPage.setDraw(1);
        dtPage.setRecordsFiltered(this.entities.size());
        dtPage.setRecordsTotal(this.entities.size());

        // Mock the service call for creating a new instance
        doReturn(dtPage).when(this.mrnEntityService).handleDatatablesPagingRequest(any());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(post("/api/mrn-entities/dt")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(dtPagingRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // Parse and validate the response
        DtPage<MrnEntityDto> result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), DtPage.class);
        assertEquals(this.entities.size(), result.getData().size());
    }

    /**
     * Test that we can correctly retrieve a single MRN entity based on the
     * provided entry ID.
     */
    @Test
    void testGetMrnEntity() throws Exception {
        doReturn(this.existingEntity).when(this.mrnEntityService).findOne(this.existingEntity.getId());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(get("/api/mrn-entities/{id}", this.existingEntity.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        // Parse and validate the response
        MrnEntityDto result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), MrnEntityDto.class);
        assertEquals(this.existingEntity, result);
    }

    /**
     * Test that if we do NOT find the MRN entity we are looking for, an HTTP
     * NOT_FOUND response will be returned.
     */
    @Test
    void testGetStationNotFound() throws Exception {
        Long id = 0L;
        doThrow(DataNotFoundException.class).when(this.mrnEntityService).findOne(any());

        // Perform the MVC request
        this.mockMvc.perform(get("/api/mrn-entities/{id}", id))
                .andExpect(status().isNotFound());
    }

    /**
     * Test that we can create a new MRN entity correctly through a POST request.
     * The incoming MRN entity should NOT have an ID, while the returned
     * value will have the ID field populated.
     */
    @Test
    void testCreateMrnEntity() throws Exception {
        // Mock the service call for creating a new instance
        doReturn(this.existingEntity).when(this.mrnEntityService).save(any());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(post("/api/mrn-entities")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.newEntity)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        // Parse and validate the response
        MrnEntityDto result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), MrnEntityDto.class);
        assertEquals(this.existingEntity, result);
    }

    /**
     * Test that if we try to create an MRN entity with an existing ID field,
     * an HTTP BAD_REQUEST response will be returned, with a description of
     * the error in the header.
     */
    @Test
    void testCreateMrnEntityWithId() throws Exception {
        // Perform the MVC request
        this.mockMvc.perform(post("/api/mrn-entities")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.existingEntity)))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    /**
     * Test that we can update an existing MRN entity correctly through a PUT
     * request. The incoming MRN entity should always have an ID.
     */
    @Test
    void testUpdateMrnEntity() throws Exception {
        // Mock the service call for updating an existing instance
        doReturn(this.existingEntity).when(this.mrnEntityService).save(any());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(put("/api/mrn-entities/{id}", this.existingEntity.getId())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.existingEntity)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        // Parse and validate the response
        MrnEntityDto result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), MrnEntityDto.class);
        assertEquals(this.existingEntity, result);
    }

    /**
     * Test that we can correctly delete an existing MRN entity by using a valid
     * ID.
     */
    @Test
    void testDeleteMrnEntity() throws Exception {
        // Perform the MVC request
        this.mockMvc.perform(delete("/api/mrn-entities/{id}", this.existingEntity.getId())
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
    }

    /**
     * Test that if we do NOT find the MRN entity we are trying to delete, an
     * HTTP NOT_FOUND response will be returned.
     */
    @Test
    void testDeleteMrnEntityNotFound() throws Exception {
        doThrow(DataNotFoundException.class).when(this.mrnEntityService).delete(any());

        // Perform the MVC request
        this.mockMvc.perform(delete("/api/mrn-entities/{id}", this.existingEntity.getId()))
                .andExpect(status().isNotFound());
    }

    /**
     * Test that we can retrieve the certificates assigned to a given MRN entity
     * using just the MRN entity ID.
     */
    @Test
    void testGetMrnEntityCertificates()  throws Exception {
        doReturn(Collections.singleton(this.certificateDto)).when(this.certificateService).findAllByMrnEntityId(this.existingEntity.getId());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(get("/api/mrn-entities/{id}/certificates", this.existingEntity.getId())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.existingEntity)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        // Parse and validate the response
        CertificateDto[] result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), CertificateDto[].class);
        assertEquals(1, result.length);
        assertEquals(this.certificateDto, result[0]);
    }

    /**
     * Test that we can generate a new X.509 certificate for the provided MRN
     * entity, identified through it's MRN Entity ID.
     */
    @Test
    void testPutMrnEntityCertificate() throws Exception {
        doReturn(this.certificateDto).when(this.certificateService).generateMrnEntityCertificate(this.existingEntity.getId());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(put("/api/mrn-entities/{id}/certificates", this.existingEntity.getId())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.existingEntity)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        // Parse and validate the response
        CertificateDto result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), CertificateDto.class);
        assertEquals(this.certificateDto, result);
    }

    /**
     * Test that if we fail to generate a new X.509 certificate for the provided
     * MRN entity, an HTTP BAD REQUEST will the returned.
     */
    @Test
    void testPutMrnEntityCertificateFailure() throws Exception {
        doThrow(IOException.class).when(this.certificateService).generateMrnEntityCertificate(this.existingEntity.getId());

        // Perform the MVC request
        this.mockMvc.perform(put("/api/mrn-entities/{id}/certificates", this.existingEntity.getId())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.existingEntity)))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

}