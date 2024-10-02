plugins {
    id("semanticz.library")
    alias(libs.plugins.lombok)
}

project.description = "Fuseki connector"

dependencies {
    implementation(project(":semanticz-connectors-common"))
    implementation(libs.jena.arq)
    implementation(libs.jena.fuseki.main)
    testImplementation(libs.junit.jupiter)
}