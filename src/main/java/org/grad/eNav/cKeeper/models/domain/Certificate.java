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

package org.grad.eNav.cKeeper.models.domain;

import org.grad.eNav.cKeeper.utils.X509Utils;
import org.hibernate.annotations.*;
import org.hibernate.annotations.Cache;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Objects;

/**
 * The type Certificate.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Entity
@Table(name = "certificates")
@Cacheable
public class Certificate {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "certificate_generator")
    @SequenceGenerator(name="certificate_generator", sequenceName = "certificate_seq",  allocationSize = 1)
    private BigInteger id;

    @NotNull
    @Type(type="text")
    @Column(name = "certificate")
    private String certificate;

    @NotNull
    @Type(type="text")
    @Column(name = "publicKey")
    private String publicKey;

    @Type(type="text")
    @Column(name = "privateKey")
    private String privateKey;

    @Column(name = "startDate")
    private Date startDate;

    @Column(name = "endDate")
    private Date endDate;

    @Column(name = "mcpMirId")
    private String mcpMirId;

    @Column(name = "revoked")
    private Boolean revoked;

    @ManyToOne()
    @JoinColumn(name = "mrnEntityId", nullable = false)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private MrnEntity mrnEntity;

    /**
     * Empty Constructor
     */
    public Certificate() {

    }

    /**
     * Certificate based on an X.509 certificate issues by the MCP Identity
     * Registry.
     *
     * @param x509Certificate the X.509 certificate
     */
    public Certificate(String mcpMirId, X509Certificate x509Certificate) throws IOException {
        this.setCertificate(X509Utils.formatCertificate(x509Certificate));
        this.setPublicKey(X509Utils.formatPublicKey(x509Certificate.getPublicKey()));
        this.setPrivateKey(null);
        this.setStartDate(x509Certificate.getNotBefore());
        this.setEndDate(x509Certificate.getNotAfter());
        this.setMcpMirId(mcpMirId);
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
     * Gets certificate.
     *
     * @return the certificate
     */
    public String getCertificate() {
        return certificate;
    }

    /**
     * Sets certificate.
     *
     * @param certificate the certificate
     */
    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    /**
     * Gets public key.
     *
     * @return the public key
     */
    public String getPublicKey() {
        return publicKey;
    }

    /**
     * Sets public key.
     *
     * @param publicKey the public key
     */
    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    /**
     * Gets private key.
     *
     * @return the private key
     */
    public String getPrivateKey() {
        return privateKey;
    }

    /**
     * Sets private key.
     *
     * @param privateKey the private key
     */
    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    /**
     * Gets start date.
     *
     * @return the start date
     */
    public Date getStartDate() {
        return startDate;
    }

    /**
     * Sets start date.
     *
     * @param startDate the start date
     */
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    /**
     * Gets end date.
     *
     * @return the end date
     */
    public Date getEndDate() {
        return endDate;
    }

    /**
     * Sets end date.
     *
     * @param endDate the end date
     */
    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    /**
     * Gets mcp mir id.
     *
     * @return the mcp mir id
     */
    public String getMcpMirId() {
        return mcpMirId;
    }

    /**
     * Sets mcp mir id.
     *
     * @param mcpMirId the mcp mir id
     */
    public void setMcpMirId(String mcpMirId) {
        this.mcpMirId = mcpMirId;
    }

    /**
     * Gets revoked.
     *
     * @return the revoked
     */
    public Boolean getRevoked() {
        return revoked;
    }

    /**
     * Sets revoked.
     *
     * @param revoked the revoked
     */
    public void setRevoked(Boolean revoked) {
        this.revoked = revoked;
    }

    /**
     * Gets mrn entity.
     *
     * @return the mrn entity
     */
    public MrnEntity getMrnEntity() {
        return mrnEntity;
    }

    /**
     * Sets mrn entity.
     *
     * @param mrnEntity the mrn entity
     */
    public void setMrnEntity(MrnEntity mrnEntity) {
        this.mrnEntity = mrnEntity;
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
        if (!(o instanceof Certificate)) return false;
        Certificate that = (Certificate) o;
        return id.equals(that.id) && mrnEntity.equals(that.mrnEntity);
    }

    /**
     * Overrides the hashcode generation of the object.
     *
     * @return the generated hashcode
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, mrnEntity);
    }
}
