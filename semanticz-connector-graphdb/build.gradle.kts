plugins {
    id("semanticz.library")
    alias(libs.plugins.lombok)
}

project.description = "GraphDB connector"

dependencies {
    implementation(project(":semanticz-connectors-common"))
    implementation(libs.jena.arq)
    implementation(libs.jena.rdfconnection)
    testImplementation(libs.junit.jupiter)
}