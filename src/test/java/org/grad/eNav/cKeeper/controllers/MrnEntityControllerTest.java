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
import org.grad.eNav.cKeeper.TestingConfiguration;
import org.grad.eNav.cKeeper.exceptions.DataNotFoundException;
import org.grad.eNav.cKeeper.models.domain.Certificate;
import org.grad.eNav.cKeeper.models.domain.MrnEntity;
import org.grad.eNav.cKeeper.models.domain.mcp.McpEntityType;
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
import org.springframework.context.annotation.Import;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(controllers = MrnEntityController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@Import(TestingConfiguration.class)
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
    private MrnEntity newEntity;
    private MrnEntity existingEntity;
    private Certificate certificate;

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
        this.newEntity = new MrnEntity();
        this.newEntity.setName("New Entity Name");
        this.newEntity.setMrn("urn:mrn:mcp:device:mcc:grad:test-new");
        this.newEntity.setEntityType(McpEntityType.DEVICE);

        // Create an MRN entity with an ID
        this.existingEntity = new MrnEntity();
        this.existingEntity.setId(BigInteger.ONE);
        this.existingEntity.setName("Existing Entity Name");
        this.existingEntity.setMrn("urn:mrn:mcp:device:mcc:grad:test-existing");
        this.existingEntity.setEntityType(McpEntityType.DEVICE);

        // Create a certificate to be assigned to the existing MRN entity
        this.certificate = new Certificate();
        this.certificate.setId(BigInteger.ONE);
        this.certificate.setMrnEntity(this.existingEntity);
        this.certificate.setPublicKey("PUBLIC KEY");
        this.certificate.setStartDate(new Date());
        this.certificate.setEndDate(new Date());
        this.certificate.setRevoked(Boolean.FALSE);
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
        MvcResult mvcResult = this.mockMvc.perform(get("/api/mrn-entity"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        // Parse and validate the response
        Page<Map<String, String>> result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), Page.class);
        assertNotNull(result);
        assertEquals(this.entities.size(), result.getTotalElements());
        assertEquals(5, result.getNumberOfElements());
        assertNotNull(result.getContent());
        for(int i=0; i< result.getNumberOfElements(); i++) {
            assertNotNull(result.getContent().get(i));
            assertEquals(this.entities.get(i).getId().intValue(), result.getContent().get(i).get("id"));
            assertEquals(this.entities.get(i).getName(), result.getContent().get(i).get("name"));
            assertEquals(this.entities.get(i).getMrn(), result.getContent().get(i).get("mrn"));
            assertEquals(this.entities.get(i).getMmsi(), result.getContent().get(i).get("mmsi"));
            assertEquals(this.entities.get(i).getEntityType(), result.getContent().get(i).get("entityType"));
            assertEquals(this.entities.get(i).getVersion(), result.getContent().get(i).get("version"));
        }
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

        // Created a result page to be returned by the mocked service
        Page<MrnEntityDto> page = new PageImpl<>(this.entities.subList(0, 5), this.pageable, this.entities.size());
        doReturn(page).when(this.mrnEntityService).handleDatatablesPagingRequest(any());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(post("/api/mrn-entity/dt")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(dtPagingRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // Parse and validate the response
        DtPage<Map<String, String>> result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), DtPage.class);
        assertNotNull(result);
        assertEquals(this.entities.size(), result.getRecordsTotal());
        assertEquals(this.entities.size(), result.getRecordsFiltered());
        assertNotNull(result.getData());
        for(int i=0; i< result.getData().size(); i++) {
            assertNotNull(result.getData().get(i));
            assertEquals(this.entities.get(i).getId().intValue(), result.getData().get(i).get("id"));
            assertEquals(this.entities.get(i).getName(), result.getData().get(i).get("name"));
            assertEquals(this.entities.get(i).getMrn(), result.getData().get(i).get("mrn"));
            assertEquals(this.entities.get(i).getMmsi(), result.getData().get(i).get("mmsi"));
            assertEquals(this.entities.get(i).getEntityType(), result.getData().get(i).get("entityType"));
            assertEquals(this.entities.get(i).getVersion(), result.getData().get(i).get("version"));
        }
    }

    /**
     * Test that we can correctly retrieve a single MRN entity based on the
     * provided entry ID.
     */
    @Test
    void testGetMrnEntity() throws Exception {
        doReturn(this.existingEntity).when(this.mrnEntityService).findOne(this.existingEntity.getId());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(get("/api/mrn-entity/{id}", this.existingEntity.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        // Parse and validate the response
        MrnEntityDto result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), MrnEntityDto.class);
        assertNotNull(result);
        assertEquals(this.existingEntity.getId(), result.getId());
        assertEquals(this.existingEntity.getName(), result.getName());
        assertEquals(this.existingEntity.getMrn(), result.getMrn());
        assertEquals(this.existingEntity.getMmsi(), result.getMmsi());
        assertEquals(this.existingEntity.getEntityType(), result.getEntityType());
        assertEquals(this.existingEntity.getVersion(), result.getVersion());
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
        this.mockMvc.perform(get("/api/mrn-entity/{id}", id))
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
        MvcResult mvcResult = this.mockMvc.perform(post("/api/mrn-entity")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.newEntity)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        // Parse and validate the response
        MrnEntityDto result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), MrnEntityDto.class);
        assertNotNull(result);
        assertEquals(this.existingEntity.getId(), result.getId());
        assertEquals(this.existingEntity.getName(), result.getName());
        assertEquals(this.existingEntity.getMrn(), result.getMrn());
        assertEquals(this.existingEntity.getMmsi(), result.getMmsi());
        assertEquals(this.existingEntity.getEntityType(), result.getEntityType());
        assertEquals(this.existingEntity.getVersion(), result.getVersion());
    }

    /**
     * Test that if we try to create an MRN entity with an existing ID field,
     * an HTTP BAD_REQUEST response will be returned, with a description of
     * the error in the header.
     */
    @Test
    void testCreateMrnEntityWithId() throws Exception {
        // Perform the MVC request
        this.mockMvc.perform(post("/api/mrn-entity")
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
        MvcResult mvcResult = this.mockMvc.perform(put("/api/mrn-entity/{id}", this.existingEntity.getId())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.existingEntity)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        // Parse and validate the response
        MrnEntityDto result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), MrnEntityDto.class);
        assertNotNull(result);
        assertEquals(this.existingEntity.getId(), result.getId());
        assertEquals(this.existingEntity.getName(), result.getName());
        assertEquals(this.existingEntity.getMrn(), result.getMrn());
        assertEquals(this.existingEntity.getMmsi(), result.getMmsi());
        assertEquals(this.existingEntity.getEntityType(), result.getEntityType());
        assertEquals(this.existingEntity.getVersion(), result.getVersion());
    }

    /**
     * Test that we can correctly delete an existing MRN entity by using a valid
     * ID.
     */
    @Test
    void testDeleteMrnEntity() throws Exception {
        // Perform the MVC request
        this.mockMvc.perform(delete("/api/mrn-entity/{id}", this.existingEntity.getId())
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
        this.mockMvc.perform(delete("/api/mrn-entity/{id}", this.existingEntity.getId()))
                .andExpect(status().isNotFound());
    }

    /**
     * Test that we can retrieve the certificates assigned to a given MRN entity
     * using just the MRN entity ID.
     */
    @Test
    void testGetMrnEntityCertificates()  throws Exception {
        doReturn(Collections.singleton(this.certificate)).when(this.certificateService).findAllByMrnEntityId(this.existingEntity.getId());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(get("/api/mrn-entity/{id}/certificates", this.existingEntity.getId())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.existingEntity)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        // Parse and validate the response
        CertificateDto[] result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), CertificateDto[].class);
        assertNotNull(result);
        assertEquals(1, result.length);
        assertNotNull(result[0]);
        assertEquals(this.certificate.getId(), result[0].getId());
        assertEquals(this.certificate.getMrnEntity().getId(), result[0].getMrnEntityId());
        assertEquals(this.certificate.getStartDate(), result[0].getStartDate());
        assertEquals(this.certificate.getEndDate(), result[0].getEndDate());
        assertEquals(this.certificate.getPublicKey(), result[0].getPublicKey());
        assertEquals(this.certificate.getRevoked(), result[0].getRevoked());
    }

    /**
     * Test that we can generate a new X.509 certificate for the provided MRN
     * entity, identified through it's MRN Entity ID.
     */
    @Test
    void testPutMrnEntityCertificate() throws Exception {
        doReturn(this.certificate).when(this.certificateService).generateMrnEntityCertificate(this.existingEntity.getId());

        // Perform the MVC request
        MvcResult mvcResult = this.mockMvc.perform(put("/api/mrn-entity/{id}/certificates", this.existingEntity.getId())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.existingEntity)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        // Parse and validate the response
        CertificateDto result = this.objectMapper.readValue(mvcResult.getResponse().getContentAsString(), CertificateDto.class);
        assertNotNull(result);
        assertEquals(this.certificate.getId(), result.getId());
        assertEquals(this.certificate.getMrnEntity().getId(), result.getMrnEntityId());
        assertEquals(this.certificate.getStartDate(), result.getStartDate());
        assertEquals(this.certificate.getEndDate(), result.getEndDate());
        assertEquals(this.certificate.getPublicKey(), result.getPublicKey());
        assertEquals(this.certificate.getRevoked(), result.getRevoked());
    }

    /**
     * Test that if we fail to generate a new X.509 certificate for the provided
     * MRN entity, an HTTP BAD REQUEST will the returned.
     */
    @Test
    void testPutMrnEntityCertificateFailure() throws Exception {
        doThrow(IOException.class).when(this.certificateService).generateMrnEntityCertificate(this.existingEntity.getId());

        // Perform the MVC request
        this.mockMvc.perform(put("/api/mrn-entity/{id}/certificates", this.existingEntity.getId())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.existingEntity)))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

}