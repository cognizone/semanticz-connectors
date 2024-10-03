# RDF Store Connectors

This repository contains connectors to RDF stores. Its goal is to provide unified API for accessing various back-end 
implementations and to optimize them in future with implementations more optimized towards individual stores.

The project structure is as follows:
- `semanticz-connectors-common` - common utilities and interfaces used by the individual connectors
- `semanticz-connectors-spring` - integration to Spring, allowing to fetch a connector as a Spring bean
- individual connectors in `semanticz-connector-` modules:
  - `semanticz-connector-fuseki` - `SparqlService`-based implementation  
  - `semanticz-connector-graphdb` - `SparqlService`-based implementation  
  - `semanticz-connector-stardog` - `SparqlService`-based implementation  
  - `semanticz-connector-jenamemory` - `SparqlService`-based implementation, `RdfStoreService` implementations  
  - `semanticz-connector-virtuoso` - `SparqlService` implementation, `RdfStoreService` implementations

## Building

```bash
./gradlew clean build
```

## Usage

The library currently supports Java 11, Jena 4, Spring 5 - full list of versions is in [gradle/libs.versions.toml](gradle/libs.versions.toml).

Two scenarios of usage are anticipated.

### Usage in Spring

1. Update your dependencies with Spring support, e.g. for gradle:
`implementation("zone.cogni.semanticz:semanticz-connectors-spring:1.0.0")`

TBD

### Usage of a dedicated connector in any API

1. Update your dependencies with the support of the required connector, e.g. for gradle:
`implementation("zone.cogni.semanticz:semanticz-connector-fuseki:1.0.0")`
