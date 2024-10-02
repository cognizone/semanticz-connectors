dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "semanticz-connectors"
include("semanticz-connector-fuseki")
include("semanticz-connector-graphdb")
include("semanticz-connector-jenamemory")
include("semanticz-connector-stardog")
include("semanticz-connector-virtuoso")
include("semanticz-connectors-common")
include("semanticz-connectors-spring")