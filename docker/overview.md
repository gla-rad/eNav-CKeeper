# The GLA e-Navigation Service Architecture - Certificate Keeper Service

## Quick Reference
* Maintained by:<br/>
[GRAD](https://www.gla-rad.org/)
* Where to get help:<br/>
[Unix & Linux](https://unix.stackexchange.com/help/on-topic),
[Stack Overflow](https://stackoverflow.com/help/on-topic),
[GRAD Wiki](https://rnavlab.gla-rad.org/wiki/E-Navigation_Service_Architecture)
(for GRAD members only)

## What is e-Navigation
The maritime domain is facing a number for challenges, mainly due to the
increasing demand, that may increase the risk of an accident or loss of life.
These challenges require technological solutions and e-Navigation is one such
solution. The International Maritime Organization ([IMO](https://www.imo.org/))
adopted a ‘Strategy for the development and implementation of e‐Navigation’
(MSC85/26, Annexes 20 and 21), providing the following definition of
e‐Navigation:

<div style="padding: 4px;
    background:lightgreen;
    border:2px;
    border-style:solid;
    border-radius:20px;
    color:black">
E-Navigation, as defined by the IMO, is the harmonised collection, integration,
exchange, presentation and analysis of maritime information on-board and ashore
by electronic means to enhance berth-to-berth navigation and related services,
for safety and security at sea and protection of the marine environment.
</div>

In response, the International Association of Lighthouse Authorities 
([IALA](https://www.iala-aism.org/)) published a number of guidelines such as 
[G1113](https://www.iala-aism.org/product/g1113/) and
[G1114](https://www.iala-aism.org/product/g1114/), which establish the relevant
principles for the design and implementation of harmonised shore-based technical
system architectures and propose a set of best practices to be followed. In
these, the terms Common Shore‐Based System (CSS) and Common Shore‐based System
Architecture (CSSA) were introduced to describe the shore‐based technical system
of the IMO’s overarching architecture.

To ensure the secure communication between ship and CSSA, the International
Electrotechnical Commission (IEC), in coordination with IALA, compiled a set of
system architecture and operational requirements for e-Navigation into a
standard better known as [SECOM](https://webstore.iec.ch/publication/64543).
This provides mechanisms for secure data exchange, as well as a TS interface
design that is in accordance with the service guidelines and templates defined
by IALA. Although SECOM is just a conceptual standard, the Maritime Connectivity
Platform ([MCP](https://maritimeconnectivity.net/)) provides an actual
implementation of a decentralised framework that supports SECOM.

## What is the GRAD e-Navigation Service Architecture
The GLA follow the developments on e-Navigation closely, contributing through
their role as an IALA member whenever possible. As part of their efforts, a
prototype GLA e-Navigation Service Architecture is being developed by the GLA
Research and Development Directorate (GRAD), to be used as the basis for the
provision of the future GLA e-Navigation services.

As a concept, the CSSA is based on the Service Oriented Architecture (SOA). A
pure-SOA approach however was found to be a bit cumbersome for the GLA
operations, as it usually requires the entire IT landscape being compatible,
resulting in high investment costs. In the context of e-Navigation, this could
become a serious problem, since different components of the system are designed
by independent teams/manufacturers. Instead, a more flexible microservice
architecture was opted for. This is based on a break-down of the larger
functional blocks into small independent services, each responsible for
performing its own orchestration, maintaining its own data and communicating
through lightweight mechanisms such as HTTP/HTTPS. It should be pointed out that
SOA and the microservice architecture are not necessarily that different.
Sometimes, microservices are even considered as an extension or a more
fine-grained version of SOA.

## The e-Navigation Certificate Keeper Service
In the GLA e-Navigation Service Architecture an additional microservice is
required to interface with the MCP. The **Certificate Keeper** microservice has
been designed explicitly for that purpose. It interfaces with the MCP MIR
securely using the TLS/SSL protocol, and is able to generate a new MCP X.509
certificate for each of the architecture elements, including the services, the
users and of course the advertised VAtoN. The generated certificates along with
their public/private key pairs are cached in a local microservice database, so
that it is easier to generate signatures for the transmitted messages, as well
as verify incoming messages based on the provided signatures. The microservice
has an additional feature, where it can be used as a stand-alone application on
the client side, in order to provide an easier and more robust way to verify the
messages originating from the CSSA, especially in cases of intermittent
connectivity.

## How to use this image
This image can be used in two ways (based on the use or not of the Spring Cloud
Config server). 
* Enabling the cloud config client and using the configurations located in an 
online repository.
* Disabling the cloud config client and using the configuration provided
locally.

### Cloud Config Configuration
In order to run the image in a **Cloud Config** configuration, you just need
to provide the environment variables that allow is to connect to the cloud
config server. This is assumed to be provided the GRAD e-Navigation Service
Architecture
[Eureka Service](https://hub.docker.com/repository/docker/glarad/enav-eureka/).

The available environment variables are:
    
    ENAV_CLOUD_CONFIG_URI=<The URL of the eureka cloud configuration server>
    ENAV_CLOUD_CONFIG_BRANCH=<The cloud configuration repository branch to be used>
    ENAV_CLOUD_CONFIG_USERNAME=<The cloud configration server username>
    ENAV_CLOUD_CONFIG_PASSWORD=<The cloud configration server password>
    
The parameters will be picked up and used to populate the default
**bootstrap.properties** of the service that look as follows:

    server.port=8764
    spring.application.name=cKeeper
    spring.application.version=<application.version>
    
    # The Spring Cloud Discovery Config
    spring.cloud.config.uri=${ENAV_CLOUD_CONFIG_URI}
    spring.cloud.config.username=${ENAV_CLOUD_CONFIG_USERNAME}
    spring.cloud.config.password=${ENAV_CLOUD_CONFIG_PASSWORD}
    spring.cloud.config.label=${ENAV_CLOUD_CONFIG_BRANCH}
    spring.cloud.config.fail-fast=false

As you can see, the service is called **cKeeper** and uses the **8764** 
port when running.

To run the image, along with the aforementioned environment variables, you can
use the following command:

    docker run -t -i --rm \
        -p 8764:8764 \
        -e ENAV_CLOUD_CONFIG_URI='<cloud config server url>' \
        -e ENAV_CLOUD_CONFIG_BRANCH='<cloud config config repository branch>' \
        -e ENAV_CLOUD_CONFIG_USERNAME='<config config repository username>' \
        -e ENAV_CLOUD_CONFIG_PASSWORD='<config config repository passord>' \
        <image-id>

### Local Config Configuration
In order to run the image in a **Local Config** configuration, you just need
to mount a local configuration directory that contains the necessary 
**.properties** files (including bootstrap) into the **/conf** directory of the
image.

This can be done in the following way:

    docker run -t -i --rm \
        -p 8764:8764 \
        -v /path/to/config-directory/on/machine:/conf \
        <image-id>

Examples of the required properties files can be seen below.

For bootstrapping, we need to disable the cloud config client, and clear our the
environment variable inputs:
    
    server.port=8764
    spring.application.name=cKeeper
    spring.application.version=<application.version>
    
    # Disable the cloud config
    spring.cloud.config.enabled=false
    
    # Clear out the environment variables
    spring.cloud.config.uri=
    spring.cloud.config.username=
    spring.cloud.config.password=
    spring.cloud.config.label=

While the application properties need to provide the service with an OAuth2.0
server like keycloak, logging configuration, the eureka client connection etc.:

    # Configuration Variables
    service.variable.eureka.server.name=<eureka.server.name>
    service.variable.eureka.server.port=<eureka.server.port>
    service.variable.keycloak.server.name=<keycloak.server.name>
    service.variable.keycloak.server.port=<keycloak.server.port>
    service.variable.keycloak.server.realm=<keycloak.realm>
    service.variable.database.server.name=<database.server.name>
    service.variable.database.server.port=<database.server.port>
    service.variable.mcp.identity-registry.server.name=<mcp.identity-registry.server.name>
    
    # Eureka Client Configuration
    eureka.client.service-url.defaultZone=http://${service.variable.eureka.server.name}:${service.variable.eureka.server.port}/eureka/
    eureka.client.registryFetchIntervalSeconds=5
    eureka.instance.preferIpAddress=true
    eureka.instance.leaseRenewalIntervalInSeconds=10
    eureka.instance.metadata-map.startup=${random.int}
    
    # Spring-boot Admin Configuration
    spring.boot.admin.client.url=http://${service.variable.server.eureka.name}:${service.variable.server.eureka.port}/admin
    
    # Logging Configuration
    logging.file.name=/var/log/${spring.application.name}.log
    logging.file.max-size=10MB
    logging.pattern.rolling-file-name=${spring.application.name}-%d{yyyy-MM-dd}.%i.log
    
    # Management Endpoints
    management.endpoint.logfile.external-file=/var/log/${spring.application.name}.log
    management.endpoints.web.exposure.include=*
    management.endpoint.health.show-details=always
    management.endpoint.httpexchanges.enabled=true
    management.endpoint.health.probes.enabled: true
    
    # Springdoc cconfiguration
    springdoc.swagger-ui.path=/swagger-ui.html
    springdoc.packagesToScan=org.grad.eNav.cKeeper.controllers
    
    # Spring JPA Configuration - PostgreSQL
    spring.jpa.generate-ddl=true
    spring.jpa.hibernate.ddl-auto=update
    spring.jpa.hibernate.show-sql=true
    spring.jpa.properties.hibernate.search.backend.directory.root=./lucene/
    spring.jpa.properties.hibernate.search.schema_management.strategy=create-or-update
    spring.jpa.properties.hibernate.search.backend.analysis.configurer=class:org.grad.eNav.cKeeper.config.CustomLuceneAnalysisConfigurer
    
    # Datasource Configuration
    spring.datasource.url=jdbc:postgresql://${service.variable.database.server.name}:${service.variable.database.server.port}/c_keeper
    spring.datasource.username=<changeit>
    spring.datasource.password=<changeit>
    
    ## Keycloak Configuration
    spring.security.oauth2.client.registration.keycloak.client-id=c-keeper
    spring.security.oauth2.client.registration.keycloak.client-secret=<changeit>
    spring.security.oauth2.client.registration.keycloak.client-name=Keycloak
    spring.security.oauth2.client.registration.keycloak.provider=keycloak
    spring.security.oauth2.client.registration.keycloak.authorization-grant-type=authorization_code
    spring.security.oauth2.client.registration.keycloak.scope=openid
    spring.security.oauth2.client.registration.keycloak.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}
    spring.security.oauth2.client.provider.keycloak.issuer-uri=http://${service.variable.keycloak.server.name}:${service.variable.keycloak.server.port}/realms/${service.variable.keycloak.server.realm}
    spring.security.oauth2.client.provider.keycloak.user-name-attribute=preferred_username
    spring.security.oauth2.resourceserver.jwt.issuer-uri=http://${service.variable.keycloak.server.name}:${service.variable.keycloak.server.port}/realms/${service.variable.keycloak.server.realm}
    
    # Open the correct resources without authentication
    gla.rad.ckeeper.resources.open:/,/index,/webjars/**,/css/**,/lib/**,/images/**,/src/**,/api/signatures/mmsi/verify/**
    
    # Front-end Information
    gla.rad.ckeeper.info.name=Certificate Keeper
    gla.rad.ckeeper.info.version=${spring.application.version}
    gla.rad.ckeeper.info.operatorName=Research and Development Directorate of GLA of UK and Ireland
    gla.rad.ckeeper.info.operatorContact=Nikolaos.Vastardis@gla-rad.org
    gla.rad.ckeeper.info.operatorUrl=https://www.gla-rad.org/
    gla.rad.ckeeper.info.copyright=\u00A9 2023 GLA Research & Development
    
    # MCP Configuration
    gla.rad.ckeeper.mcp.host=${service.variable.mcp.identity-registry.server.name}
    gla.rad.ckeeper.mcp.organisation=grad
    gla.rad.ckeeper.mcp.mcpOrgPrefix=urn:mrn:mcp:org:mcc
    gla.rad.ckeeper.mcp.mcpEntityPrefix=urn:mrn:mcp
    gla.rad.ckeeper.mcp.mcpEntitySuffix=mcc
    gla.rad.ckeeper.mcp.keyStore=<path.to.keystore>
    gla.rad.ckeeper.mcp.keyStorePassword=<changeit>
    gla.rad.ckeeper.mcp.keyStoreType=<PKCS12/JKS>
    gla.rad.ckeeper.mcp.trustStore=<path.to.truststore>
    gla.rad.ckeeper.mcp.trustStorePassword=<changeit>
    gla.rad.ckeeper.mcp.trustStoreType=<PKCS12/JKS>
    gla.rad.ckeeper.mcp.trustStore.rootCertificate.alias=mcp-root
    gla.rad.ckeeper.mcp.trustStore.rootCertificate.thumbprintAlgorithm=SHA-1
    
    # X509 Certificate Configuration
    gla.rad.ckeeper.x509.keypair.curve=secp256r1
    gla.rad.ckeeper.x509.cert.algorithm=SHA256withCVC-ECDSA
    gla.rad.ckeeper.x509.cert.dirName=<certificate.dir.name>
    gla.rad.ckeeper.x509.cert.yearDuration=1

## Operation
The **Certificate Keeper** microservice’s main purpose is to integrate the
architecture with an MCP instance. This is achieved via an X.509 certificate
from the MCP MIR, generated by a user with administration rights for a given
organisation. This allows the **Certificate Keeper** to connect to the MIR via
SSL/TLS and perform various administration functions such as registering new
entities, managing them and generating certificates for these.

The certificate generation process can be seen as one of the weakest links in
the security operation of the MIR, mainly in terms of potentially leaking the
certificate’s private key to unauthorised parties eavesdropping on the
generation operation. To mitigate this issue, the MIR allows the generation of
certificates through an X.509 Certificate Signing Request (CSR), which is
already pre-signed by a key pair generated locally. The MIR will subsequently
generate the new certificate using the provided CSR, as well as the CA
certificate and its own key pair.

The current implementation of the “Certificate Keeper” has two major
improvements:

1. It is able to generate certificates not only for the VAtoN being transmitted
   for all entities involved in the architecture, including the microservice
   components, any vessels participating in the e-Navigation operations etc.
2. It is able to not only automatically generate certificates for an entity when
   these are required, but also synchronises its own database with the latest
   certificates generated in the MIR through other operations, at least if there
   is a valid connection.

The most important for the MIR integration is the MRN, since this is the
primary identifier used. MRNs however follow strict rules, and can easily be
derived from the name and type of the entity, so the **Certificate Keeper** is
able auto-generate it. The MMSI on the other hand, is not a mandatory attribute,
but it is really useful for validating AIS/VDES transmissions.

The default signature generation method, is based on the Elliptic Curve Digital
Signature Algorithm (ECDSA), using a 256 bit hash-key and the secp256r1 curve.
This produces signatures of 512-bit length. This cryptographic algorithm choice
is also compatible with the requirements of SECOM, and therefore the output of
this operation can be utilised by the
[SECOMLib](https://github.com/gla-rad/SECOMLib) SecomSignatureProvider
implementations (as is the case in the “AtoN Service”), to populate the
signatures of the SECOM messages.

## Contributing
For contributing in this project, please have a look at the Github repository
[eNav-CKeeper](https://github.com/gla-rad/eNav-CKeeper). Pull
requests are welcome. For major changes, please open an issue first to discuss
what you would like to change.

Please make sure to update tests as appropriate.

## License
Distributed under the Apache License, Version 2.0.

## Contact
Nikolaos Vastardis - 
[Nikolaos.Vastardis@gla-rad.org](mailto:Nikolaos.Vastardis@gla-rad.org)
