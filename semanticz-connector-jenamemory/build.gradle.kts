plugins {
    id("semanticz.library")
    alias(libs.plugins.lombok)
}

project.description = "Jena in-memory connector"

dependencies {
    implementation(project(":semanticz-connectors-common"))
    implementation(libs.jena.arq)
    implementation(libs.jakarta.annotation.api)
    testImplementation(libs.junit.jupiter)
}