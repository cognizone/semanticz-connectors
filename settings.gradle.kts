pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "pl.allegro.tech.build.axion-release") {
                useVersion("1.18.6")
            }
        }
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
