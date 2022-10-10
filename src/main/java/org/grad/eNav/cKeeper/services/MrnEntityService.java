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

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.search.Sort;
import org.grad.eNav.cKeeper.exceptions.*;
import org.grad.eNav.cKeeper.models.domain.MrnEntity;
import org.grad.eNav.cKeeper.models.domain.mcp.McpEntityType;
import org.grad.eNav.cKeeper.models.dtos.datatables.DtPagingRequest;
import org.grad.eNav.cKeeper.models.dtos.mcp.*;
import org.grad.eNav.cKeeper.repos.MRNEntityRepo;
import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.*;

/**
 * The MRN Entity Service Class
 *
 * Service Implementation for managing MRN Entities.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Service
@Slf4j
public class MrnEntityService {

    /**
     * The Entity Manager.
     */
    @Autowired
    EntityManager entityManager;

    /**
     * The MCP Service.
     */
    @Autowired
    McpService mcpService;

    /**
     * The MRN Entity Repo
     */
    @Autowired
    MRNEntityRepo mrnEntityRepo;

    // Service Variables
    private final String[] searchFields = new String[] {
            "name",
            "mrn",
            "mmsi"
    };
    private final String[] searchFieldsWithSort = new String[] {
            "id"
    };

    /**
     * Get all the MRN Entities.
     *
     * @return the list of MRN Entities
     */
    @Transactional(readOnly = true)
    public List<MrnEntity> findAll() {
        log.debug("Request to get all MRN Entities");
        return this.mrnEntityRepo.findAll();
    }

    /**
     * Get all the MRN Entities in a pageable search.
     *
     * @param pageable the pagination information
     * @return the list of MRN Entities
     */
    @Transactional(readOnly = true)
    public Page<MrnEntity> findAll(@NotNull Pageable pageable) {
        log.debug("Request to get all MRN Entities in a pageable search");
        return this.mrnEntityRepo.findAll(pageable);
    }

    /**
     * Get one MRN Entity by ID.
     *
     * @param id the ID of the MRN Entity
     * @return the node
     */
    @Transactional(readOnly = true)
    public MrnEntity findOne(@NotNull BigInteger id) {
        log.debug("Request to get MRN Entity : {}", id);
        return this.mrnEntityRepo.findById(id)
                .orElseThrow(() ->
                        new DataNotFoundException(String.format("No MRN Entity node found for the provided ID: %d", id))
                );
    }

    /**
     * Get one MRN Entity by name.
     *
     * @param name the name of the entity
     * @return the node
     */
    @Transactional(readOnly = true)
    public MrnEntity findOneByName(@NotNull String name) {
        log.debug("Request to get MRN Entity with MRN : {}", name);
        return this.mrnEntityRepo.findByName(name)
                .orElseThrow(() ->
                        new DataNotFoundException(String.format("No MRN Entity found for the provided name: %s", name))
                );
    }

    /**
     * Get one MRN Entity by MRN.
     *
     * @param mrn the MRN of the node
     * @return the node
     */
    @Transactional(readOnly = true)
    public MrnEntity findOneByMrn(@NotNull String mrn) {
        log.debug("Request to get MRN Entity with MRN : {}", mrn);
        return this.mrnEntityRepo.findByMrn(mrn)
                .orElseThrow(() ->
                        new DataNotFoundException(String.format("No MRN Entity found for the provided MRN: %s", mrn))
                );
    }

    /**
     * Get one MRN Entity by MMSI.
     *
     * @param mmsi the MMSI of the node
     * @return the node
     */
    @Transactional(readOnly = true)
    public MrnEntity findOneByMmsi(@NotNull String mmsi) {
        log.debug("Request to get MRN Entity with MMSI : {}", mmsi);
        return this.mrnEntityRepo.findByMmsi(mmsi)
                .orElseThrow(() ->
                        new DataNotFoundException(String.format("No MRN Entity found for the provided MRN: %s", mmsi))
                );
    }

    /**
     * Save an MRN entity.
     *
     * @param mrnEntity the MRN Entity DTO to be saved
     * @return the persisted MRN Entity DTO
     */
    @Transactional
    public MrnEntity save(@NotNull MrnEntity mrnEntity) {
        log.debug("Request to save MRN Entity : {}", mrnEntity);

        // Sanity Check
        if(Objects.nonNull(mrnEntity.getId()) && !this.mrnEntityRepo.existsById(mrnEntity.getId())) {
            throw new DataNotFoundException(String.format("No MRN Entity found for the provided ID: %d", mrnEntity.getId()));
        }

        // Services should always have a version
        if(mrnEntity.getEntityType() == McpEntityType.SERVICE && Objects.isNull(mrnEntity.getVersion())) {
            throw new ValidationException(String.format("No version provided by the MRN Entity service with MRN: %s", mrnEntity.getMrn()));
        }

        // Save the MRN Entity
        return Optional.of(mrnEntity)
                .map(entity -> {
                    McpEntityBase mcpEntity;
                    try {
                        // First try to identify if the entity exists in the MRN
                        // MIR, and if no we are going to create it, otherwise
                        // update it
                        try {
                            mcpEntity = this.mcpService.getMcpEntity(entity.getMrn(), entity.getVersion(), entity.getEntityType().getEntityClass());
                        } catch(DataNotFoundException ex) {
                            this.log.warn("MCP entry for the MRN device with MRN {} not found", entity.getMrn());
                            mcpEntity = entity.getEntityType().getEntityClass().getDeclaredConstructor().newInstance();
                            mcpEntity.setMrn(entity.getMrn());
                        }

                        // We can only update the name of the MCP entity (and version for new services)
                        if(McpDeviceDto.class.isInstance(mcpEntity)) {
                            ((McpDeviceDto)mcpEntity).setName(entity.getName());
                        } else if(McpServiceDto.class.isInstance(mcpEntity)) {
                            ((McpServiceDto)mcpEntity).setName(entity.getName());
                            if(Objects.isNull(mcpEntity.getId())) {
                                ((McpServiceDto) mcpEntity).setInstanceVersion(entity.getVersion());
                            }
                        } else if(McpVesselDto.class.isInstance(mcpEntity)) {
                            ((McpVesselDto)mcpEntity).setName(entity.getName());
                        } else if(McpUserDto.class.isInstance(mcpEntity)) {
                            ((McpUserDto)mcpEntity).setFirstName(entity.getName().split(" ")[0]);
                            ((McpUserDto)mcpEntity).setLastName(entity.getName().split(" ")[1]);
                        } else if(McpRoleDto.class.isInstance(mcpEntity)) {
                            ((McpRoleDto)mcpEntity).setRoleName(entity.getName());
                        }

                        // Choose whether to create or update
                        if(Objects.isNull(mcpEntity.getId())) {
                            mcpEntity = this.mcpService.createMcpEntity(mcpEntity);
                        } else {
                            mcpEntity = this.mcpService.updateMcpEntity(mcpEntity.getMrn(), mcpEntity);
                        }

                        // Always read the MRN from the MCP MIR
                        entity.setMrn(mcpEntity.getMrn());
                    } catch (IOException | McpConnectivityException ex) {
                        // If the MCP connectivity failed, don't continue
                        log.error(ex.getMessage());
                        return null;
                    } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException ex) {
                        // If we get a reflections change, also don't continue
                        log.error(ex.getMessage());
                        return null;
                    }
                    return entity;
                })
                .map(this.mrnEntityRepo::save)
                .orElseThrow(() ->
                        new SavingFailedException(String.format("Cannot save invalid MRN Entity object"))
                );
    }

    /**
     * Delete the MRN entity by ID.
     *
     * @param id the ID of the MRN entity
     */
    @Transactional
    public void delete(@NotNull BigInteger id) {
        log.debug("Request to delete MRN Entity : {}", id);

        // Sanity Check
        if(!this.mrnEntityRepo.existsById(id)) {
            throw new DataNotFoundException(String.format("No MRN Entity found for the provided ID: %d", id));
        }

        // Check and update the MCP Identity Registry
        this.mrnEntityRepo.findById(id)
                .map(entity -> {
                    try {
                        this.mcpService.deleteMcpEntity(entity.getMrn(), entity.getVersion(), entity.getEntityType().getEntityClass());
                    } catch(DeletingFailedException ex) {
                        // Not found? Not problem!
                    } catch(IOException | McpConnectivityException ex) {
                        // If the MCP connectivity failed, don't continue
                        return null;
                    }
                    return entity.getId();
                })
                .orElseThrow(() ->
                        new DeletingFailedException(String.format("Cannot delete invalid MRN Entity object"))
                );
        // Finally, delete the station node
        this.mrnEntityRepo.deleteById(id);
    }

    /**
     * Delete the MRN Entity by MRN.
     *
     * @param mrn the MRN of the MRN Entity
     */
    public void deleteByMRN(@NotNull String mrn) throws DataNotFoundException {
        log.debug("Request to delete MRN Entity with MRN : {}", mrn);
        this.delete(this.findOneByMrn(mrn).getId());
    }

    /**
     * This utility function can be used to retrieve an MRN entity based on its
     * specified name. If that entry does not exist, a new one will be generated
     * and persisted in the database, using all the provided parameters (i.e.
     * MRN, MMSI, type etc).
     *
     * @param entityName        The entity name to identify the entry with
     * @param mrn               The MRN to be used for new entries
     * @param mmsi              The MMSI to be used for new entries
     * @param entityType        The type of the enity to be used for new entries
     * @return the identified or new MRN Entity entry
     */
    public MrnEntity getOrCreate(@NotNull String entityName, @NotNull String mrn, String mmsi, @NotNull McpEntityType entityType) {
        return this.mrnEntityRepo.findByName(entityName)
                .orElseGet(() -> {
                    try {
                        final MrnEntity newMrnEntity = new MrnEntity();
                        newMrnEntity.setName(entityName);
                        newMrnEntity.setMrn(mrn);
                        newMrnEntity.setMmsi(mmsi);
                        newMrnEntity.setEntityType(entityType);
                        newMrnEntity.setVersion(entityType == McpEntityType.SERVICE ? "0.0.1" : null);
                        return this.save(newMrnEntity);
                    } catch (Exception ex) {
                        throw new SavingFailedException(ex.getMessage());
                    }
                });
    }

    /**
     * Handles a datatables pagination request and returns the results list in
     * an appropriate format to be viewed by a datatables jQuery table.
     *
     * @param dtPagingRequest the Datatables pagination request
     * @return the Datatables paged response
     */
    @Transactional(readOnly = true)
    public Page<MrnEntity> handleDatatablesPagingRequest(DtPagingRequest dtPagingRequest) {
        // Create the search query
        SearchQuery searchQuery = this.searchMRNEntitiesQuery(
                dtPagingRequest.getSearch().getValue(),
                dtPagingRequest.getLucenceSort(Arrays.asList(searchFieldsWithSort))
        );

        // Perform the search query and return the datatables page result
        return Optional.of(searchQuery)
                .map(query -> query.fetch(dtPagingRequest.getStart(), dtPagingRequest.getLength()))
                .map(searchResult -> new PageImpl<MrnEntity>(searchResult.hits(), dtPagingRequest.toPageRequest(), searchResult.total().hitCount()))
                .orElseGet(() -> new PageImpl<>(Collections.emptyList(), dtPagingRequest.toPageRequest(), 0));
    }

    /**
     * Constructs a hibernate search query using Lucene based on the provided
     * search test. This query will be based solely on the stations table and
     * will include the following fields:
     * - Name
     * - MRN
     * - MMSI
     *
     * @param searchText the text to be searched
     * @param sort the sorting selection for the search query
     * @return the full text query
     */
    protected SearchQuery<MrnEntity> searchMRNEntitiesQuery(String searchText, Sort sort) {
        SearchSession searchSession = Search.session( entityManager );
        SearchScope<MrnEntity> scope = searchSession.scope( MrnEntity.class );
        return searchSession.search( scope )
                .extension(LuceneExtension.get())
                .where( scope.predicate().wildcard()
                        .fields( this.searchFields )
                        .matching( Optional.ofNullable(searchText).map(st -> "*"+st).orElse("") + "*" )
                        .toPredicate() )
                .sort(f -> f.fromLuceneSort(sort))
                .toQuery();
    }

}
