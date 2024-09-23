dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "semanticz-connectors"
include("semanticz-connector-graphdb")
include("semanticz-connector-stardog")
include("semanticz-connector-virtuoso")
include("semanticz-connectors-common")
include("semanticz-connector-fuseki")

