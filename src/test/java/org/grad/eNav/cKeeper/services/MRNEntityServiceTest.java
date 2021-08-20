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

package org.grad.eNav.cKeeper.services;

import org.grad.eNav.cKeeper.exceptions.DataNotFoundException;
import org.grad.eNav.cKeeper.models.domain.MRNEntity;
import org.grad.eNav.cKeeper.models.dtos.MRNEntityDto;
import org.grad.eNav.cKeeper.models.dtos.datatables.*;
import org.grad.eNav.cKeeper.repos.MRNEntityRepo;
import org.hibernate.search.jpa.FullTextQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import javax.persistence.EntityManager;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MRNEntityServiceTest {

    /**
     * The Tested Service.
     */
    @InjectMocks
    @Spy
    MRNEntityService mrnEntityService;

    /**
     * The Entity Manager mock.
     */
    @Mock
    EntityManager entityManager;

    /**
     * The Station Repository Mock.
     */
    @Mock
    private MRNEntityRepo mrnEntityRepo;

    // Test Variables
    private List<MRNEntity> entities;
    private Pageable pageable;
    private MRNEntity newEntity;
    private MRNEntity existingEntity;

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setup() {
        // Initialise the MRN entities list
        this.entities = new ArrayList<>();
        for(long i=0; i<10; i++) {
            MRNEntity entity = new MRNEntity();
            entity.setId(BigInteger.valueOf(i));
            entity.setName("Entity Name");
            entity.setMrn("urn:mrn:mcp:device:mcc:grad:test" + i);
            entity.setCertificate("CertificateNo" +i);
            entity.setPublicKey("PublicKeyNo" + i);
            entity.setPrivateKey("PrivateKeyNo" + i);
            this.entities.add(entity);
        }

        // Create a pageable definition
        this.pageable = PageRequest.of(0, 5);

        // Create a new MRN entity
        this.newEntity = new MRNEntity();
        this.newEntity.setName("New Entity Name");
        this.newEntity.setMrn("urn:mrn:mcp:device:mcc:grad:test-new");
        this.newEntity.setCertificate("CertificateNew");
        this.newEntity.setPublicKey("PublicKeyNew");
        this.newEntity.setPrivateKey("PrivateKeyNew");

        // Create a new MRN entity
        this.existingEntity = new MRNEntity();
        this.existingEntity.setId(BigInteger.ONE);
        this.existingEntity.setName("Existing Entity Name");
        this.existingEntity.setMrn("urn:mrn:mcp:device:mcc:grad:test-existing");
        this.existingEntity.setCertificate("CertificateExisting");
        this.existingEntity.setPublicKey("PublicKeyExisting");
        this.existingEntity.setPrivateKey("PrivateKeyExisting");
    }

    /**
     * Test that we can retrieve all the MRN entities currently present in the
     * database.
     */
    @Test
    void testFindAll() {
        // Created a result page to be returned by the mocked repository
        doReturn(this.entities).when(this.mrnEntityRepo).findAll();

        // Perform the service call
        List<MRNEntityDto> result = this.mrnEntityService.findAll();

        // Test the result
        assertEquals(this.entities.size(), result.size());

        // Test each of the result entries
        for(int i=0; i < result.size(); i++){
            assertEquals(new MRNEntityDto(this.entities.get(i)), result.get(i));
        }
    }

    /**
     * Test that we can retrieve all the MRN entities currently present in the
     * database through a paged call.
     */
    @Test
    void testFindAllPaged() {
        // Created a result page to be returned by the mocked repository
        Page<MRNEntity> page = new PageImpl<>(this.entities.subList(0, 5), this.pageable, this.entities.size());
        doReturn(page).when(this.mrnEntityRepo).findAll(this.pageable);

        // Perform the service call
        Page<MRNEntityDto> result = this.mrnEntityService.findAll(pageable);

        // Test the result
        assertEquals(page.getSize(), result.getSize());

        // Test each of the result entries
        for(int i=0; i < result.getSize(); i++){
            assertEquals(new MRNEntityDto(this.entities.get(i)), result.getContent().get(i));
        }
    }

    /**
     * Test that we can retrieve a single MRN entity based on the MRN entity
     * ID.
     */
    @Test
    void testFindOne() {
        doReturn(Optional.of(this.existingEntity)).when(this.mrnEntityRepo).findById(this.existingEntity.getId());

        // Perform the service call
        MRNEntityDto result = this.mrnEntityService.findOne(this.existingEntity.getId());

        // Make sure the eager relationships repo call was called
        verify(this.mrnEntityRepo, times(1)).findById(this.existingEntity.getId());

        // Test the result
        assertNotNull(result);
        assertEquals(this.existingEntity.getId(), result.getId());
        assertEquals(this.existingEntity.getName(), result.getName());
        assertEquals(this.existingEntity.getCertificate(), result.getCertificate());
        assertEquals(this.existingEntity.getPublicKey(), result.getPublicKey());
        assertEquals(this.existingEntity.getPrivateKey(), result.getPrivateKey());
    }

    /**
     * Test that we if the provided MRN entity ID does NOT exist, then when
     * trying to retrieve the respective MRN entity will return a
     * DataNotFoundException.
     */
    @Test
    void testFindOneNotFound() {
        // Perform the service call
        assertThrows(DataNotFoundException.class, () ->
                this.mrnEntityService.findOne(this.existingEntity.getId())
        );
    }

    /**
     * Test that we can retrieve a single MRN entity based on the MRN entity
     * MRN.
     */
    @Test
    void testFindOneByMrn() {
        doReturn(Optional.of(this.existingEntity)).when(this.mrnEntityRepo).findByMrn(this.existingEntity.getMrn());

        // Perform the service call
        MRNEntityDto result = this.mrnEntityService.findOneByMrn(this.existingEntity.getMrn());

        // Make sure the eager relationships repo call was called
        verify(this.mrnEntityRepo, times(1)).findByMrn(this.existingEntity.getMrn());

        // Test the result
        assertNotNull(result);
        assertEquals(this.existingEntity.getId(), result.getId());
        assertEquals(this.existingEntity.getName(), result.getName());
        assertEquals(this.existingEntity.getCertificate(), result.getCertificate());
        assertEquals(this.existingEntity.getPublicKey(), result.getPublicKey());
        assertEquals(this.existingEntity.getPrivateKey(), result.getPrivateKey());
    }

    /**
     * Test that we if the provided MRN entity MRN does NOT exist, then when
     * trying to retrieve the respective MRN entity will return a
     * DataNotFoundException.
     */
    @Test
    void testFindOneNotFoundByMrn() {
        // Perform the service call
        assertThrows(DataNotFoundException.class, () ->
                this.mrnEntityService.findOne(this.existingEntity.getId())
        );
    }

    /**
     * Test that we can save correctly a new MRN entity if all the validation
     * checks are successful.
     */
    @Test
    void testCreate() {
        doReturn(this.newEntity).when(this.mrnEntityRepo).save(any());

        // Perform the service call
        MRNEntityDto result = this.mrnEntityService.save(new MRNEntityDto(this.newEntity));

        // Test the result
        assertEquals(this.newEntity.getId(), result.getId());
        assertEquals(this.newEntity.getName(), result.getName());
        assertEquals(this.newEntity.getCertificate(), result.getCertificate());
        assertEquals(this.newEntity.getPublicKey(), result.getPublicKey());
        assertEquals(this.newEntity.getPrivateKey(), result.getPrivateKey());

        // Also that a saving call took place in the repository
        verify(this.mrnEntityRepo, times(1)).save(this.newEntity);
    }

    /**
     * Test that we can update correctly an existing station if all the
     * validation checks are successful.
     */
    @Test
    void testUpdate() {
        doReturn(this.existingEntity).when(this.mrnEntityRepo).save(any());

        // Perform the service call
        MRNEntityDto result = this.mrnEntityService.save(new MRNEntityDto(this.existingEntity));

        // Test the result
        assertEquals(this.existingEntity.getId(), result.getId());
        assertEquals(this.existingEntity.getName(), result.getName());
        assertEquals(this.existingEntity.getCertificate(), result.getCertificate());
        assertEquals(this.existingEntity.getPublicKey(), result.getPublicKey());
        assertEquals(this.existingEntity.getPrivateKey(), result.getPrivateKey());

        // Also that a saving call took place in the repository
        verify(this.mrnEntityRepo, times(1)).save(this.existingEntity);
    }

    /**
     * Test that we can successfully delete an existing station.
     */
    @Test
    void testDelete() {
        doReturn(Boolean.TRUE).when(this.mrnEntityRepo).existsById(this.existingEntity.getId());
        doNothing().when(this.mrnEntityRepo).deleteById(this.existingEntity.getId());

        // Perform the service call
        this.mrnEntityService.delete(this.existingEntity.getId());

        // Verify that a deletion call took place in the repository
        verify(this.mrnEntityRepo, times(1)).deleteById(this.existingEntity.getId());
    }

    /**
     * Test that if we try to delete a non-existing station then a
     * DataNotFoundException will be thrown.
     */
    @Test
    void testDeleteNotFound() {
        doReturn(Boolean.FALSE).when(this.mrnEntityRepo).existsById(this.existingEntity.getId());

        // Perform the service call
        assertThrows(DataNotFoundException.class, () ->
                this.mrnEntityService.delete(this.existingEntity.getId())
        );
    }

    /**
     * Test that we can retrieve the paged list of stations for a Datatables
     * pagination request (which by the way also includes search and sorting
     * definitions).
     */
    @Test
    void testGetStationsForDatatables() {
        // First create the pagination request
        DtPagingRequest dtPagingRequest = new DtPagingRequest();
        dtPagingRequest.setStart(0);
        dtPagingRequest.setLength(5);

        // Set the pagination request columns
        dtPagingRequest.setColumns(new ArrayList());
        Stream.of("id", "name", "mrn")
                .map(DtColumn::new)
                .forEach(dtPagingRequest.getColumns()::add);

        // Set the pagination request ordering
        DtOrder dtOrder = new DtOrder();
        dtOrder.setColumn(0);
        dtOrder.setDir(DtDirection.asc);
        dtPagingRequest.setOrder(Collections.singletonList(dtOrder));

        // Set the pagination search
        DtSearch dtSearch = new DtSearch();
        dtSearch.setValue("search-term");
        dtPagingRequest.setSearch(dtSearch);

        // Mock the full text query
        FullTextQuery mockedQuery = mock(FullTextQuery.class);
        doReturn(this.entities.subList(0, 5)).when(mockedQuery).getResultList();
        doReturn(mockedQuery).when(this.mrnEntityService).searchStationsQuery(any());

        // Perform the service call
        DtPage<MRNEntityDto> result = this.mrnEntityService.handleDatatablesPagingRequest(dtPagingRequest);

        // Validate the result
        assertNotNull(result);
        assertEquals(5, result.getRecordsFiltered());

        // Test each of the result entries
        for(int i=0; i < result.getRecordsFiltered(); i++){
            assertEquals(new MRNEntityDto(this.entities.get(i)), result.getData().get(i));
        }
    }

}