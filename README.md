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
- `examples` - project exemplifying the usage.

## Building

```bash
./gradlew clean build
```

## Usage

| semanticz-connectors | Java | Jena |
|----------------------|------|------|
| 1.x                  | 11   | 4    |
| 2.x                  | 17   | 5    |

Full list of versions of the given branch is in [gradle/libs.versions.toml](gradle/libs.versions.toml).

Two usage scenarios are anticipated:
1. Dedicated connector usage, e.g. for gradle:
```
implementation("zone.cogni.semanticz:semanticz-connector-fuseki:1.0.0")
implementation("zone.cogni.semanticz:semanticz-connectors-common:1.0.0")
```

2. Spring usage, e.g. for gradle:
```
implementation("zone.cogni.semanticz:semanticz-connectors-spring:1.0.0")
implementation("zone.cogni.semanticz:semanticz-connectors-common:1.0.0")
```

For details refer to the example project [example-spring-with-fuseki](examples/example-spring-with-fuseki).