plugins {
    id("semanticz.library")
    alias(libs.plugins.lombok)
}

project.description = "RDF4J connector"

dependencies {
    implementation(project(":semanticz-connectors-common"))
    implementation(libs.jena.arq)
    implementation(libs.rdf4j.repository.sail)
    implementation(libs.rdf4j.sail.memory)
    implementation(libs.rdf4j.rio.turtle)
    implementation(libs.rdf4j.queryresultio.sparqljson)

    testImplementation(libs.junit.jupiter)
}