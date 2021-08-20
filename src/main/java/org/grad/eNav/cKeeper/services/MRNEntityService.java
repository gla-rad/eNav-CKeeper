/*
 * Copyright (c) 2021 GLA Research and Development Directorate
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.grad.eNav.cKeeper.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.search.Query;
import org.grad.eNav.cKeeper.exceptions.DataNotFoundException;
import org.grad.eNav.cKeeper.exceptions.SavingFailedException;
import org.grad.eNav.cKeeper.models.domain.MRNEntity;
import org.grad.eNav.cKeeper.models.dtos.MRNEntityDto;
import org.grad.eNav.cKeeper.models.dtos.datatables.DtPage;
import org.grad.eNav.cKeeper.models.dtos.datatables.DtPagingRequest;
import org.grad.eNav.cKeeper.repos.MRNEntityRepo;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The MRN Entity Service Class
 *
 * Service Implementation for managing MRN Entities.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Service
@Slf4j
public class MRNEntityService {

    /**
     * The Entity Manager.
     */
    @Autowired
    EntityManager entityManager;

    /**
     * The MRN Entity Repo
     */
    @Autowired
    MRNEntityRepo mrnEntityRepo;

    // Service Variables
    private final String[] searchFields = new String[] {
            "name",
            "mrn"
    };

    /**
     * Get all the MRN Entities.
     *
     * @return the list of MRN Entities
     */
    @Transactional(readOnly = true)
    public List<MRNEntityDto> findAll() {
        log.debug("Request to get all MRN Entities");
        return this.mrnEntityRepo.findAll()
                .stream()
                .map(MRNEntityDto::new)
                .collect(Collectors.toList());
    }

    /**
     * Get all the MRN Entities in a pageable search.
     *
     * @param pageable the pagination information
     * @return the list of MRN Entities
     */
    @Transactional(readOnly = true)
    public Page<MRNEntityDto> findAll(Pageable pageable) {
        log.debug("Request to get all MRN Entities in a pageable search");
        return this.mrnEntityRepo.findAll(pageable)
                .map(MRNEntityDto::new);
    }

    /**
     * Get one MRN Entity by ID.
     *
     * @param id the ID of the MRN Entity
     * @return the node
     */
    @Transactional(readOnly = true)
    public MRNEntityDto findOne(BigInteger id) {
        log.debug("Request to get MRN Entity : {}", id);
        return this.mrnEntityRepo.findById(id)
                .map(MRNEntityDto::new)
                .orElseThrow(() ->
                        new DataNotFoundException(String.format("No MRN Entity node found for the provided ID: %d", id))
                );
    }

    /**
     * Get one MRN Entity by MRN.
     *
     * @param mrn the MRN of the node
     * @return the node
     */
    @Transactional(readOnly = true)
    public MRNEntityDto findOneByMrn(String mrn) {
        log.debug("Request to get MRN Entity with MRN : {}", mrn);
        return this.mrnEntityRepo.findByMrn(mrn)
                .map(MRNEntityDto::new)
                .orElseThrow(() ->
                        new DataNotFoundException(String.format("No MRN Entity found for the provided MRN: %s", mrn))
                );
    }

    /**
     * Save a node.
     *
     * @param entity the MRN Entity to save
     * @return the persisted MRN Entity
     */
    public MRNEntityDto save(MRNEntityDto entity) {
        log.debug("Request to save MRN Entity : {}", entity);
        return Optional.of(entity)
                .map(MRNEntityDto::toMRNEntity)
                .map(this.mrnEntityRepo::save)
                .map(MRNEntityDto::new)
                .orElseThrow(() ->
                        new SavingFailedException(String.format("Cannot save invalid MRN Entity object"))
                );
    }

    /**
     * Delete the node by ID.
     *
     * @param id the ID of the node
     */
    public void delete(BigInteger id) {
        log.debug("Request to delete MRN Entity : {}", id);
        if(this.mrnEntityRepo.existsById(id)) {
            // Finally delete the station node
            this.mrnEntityRepo.deleteById(id);
        } else {
            throw new DataNotFoundException(String.format("No MRN Entity found for the provided ID: %d", id));
        }
    }

    /**
     * Delete the MRN Entity by MRN.
     *
     * @param mrn the MRN of the MRN Entity
     */
    public void deleteByMRN(String mrn) throws DataNotFoundException {
        log.debug("Request to delete MRN Entity with MRN : {}", mrn);
        this.delete(this.findOneByMrn(mrn).getId());
    }

    /**
     * Handles a datatables pagination request and returns the results list in
     * an appropriate format to be viewed by a datatables jQuery table.
     *
     * @param dtPagingRequest the Datatables pagination request
     * @return the Datatables paged response
     */
    @Transactional(readOnly = true)
    public DtPage<MRNEntityDto> handleDatatablesPagingRequest(DtPagingRequest dtPagingRequest) {
        // Create the search query
        FullTextQuery searchQuery = this.searchStationsQuery(dtPagingRequest.getSearch().getValue());
        searchQuery.setFirstResult(dtPagingRequest.getStart());
        searchQuery.setMaxResults(dtPagingRequest.getLength());

        // Add sorting if requested
        Optional.of(dtPagingRequest)
                .map(DtPagingRequest::getLucenceSort)
                .filter(ls -> ls.getSort().length > 0)
                .ifPresent(searchQuery::setSort);

        // Perform the search query and return the datatables page result
        return Optional.of(searchQuery)
                .map(FullTextQuery::getResultList)
                .map(entities -> new PageImpl<>(entities, dtPagingRequest.toPageRequest(), searchQuery.getResultSize()))
                .map(Page.class::cast)
                .map(page -> page.map(entity -> new MRNEntityDto((MRNEntity)entity)))
                .map(page -> new DtPage<>((Page<MRNEntityDto>)page, dtPagingRequest))
                .orElseGet(DtPage::new);
    }

    /**
     * Constructs a hibernate search query using Lucene based on the provided
     * search test. This query will be based solely on the stations table and
     * will include the following fields:
     * - Name
     * - IP Address
     * - MMSI
     *
     * @param searchText the text to be searched
     * @return the full text query
     */
    protected FullTextQuery searchStationsQuery(String searchText) {
        FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(entityManager);
        QueryBuilder queryBuilder = fullTextEntityManager.getSearchFactory()
                .buildQueryBuilder()
                .forEntity(MRNEntity.class)
                .get();

        Query luceneQuery = queryBuilder
                .keyword()
                .wildcard()
                .onFields(this.searchFields)
                .matching(Optional.ofNullable(searchText).orElse("").toLowerCase() + "*")
                .createQuery();

        return fullTextEntityManager.createFullTextQuery(luceneQuery, MRNEntity.class);
    }

}
