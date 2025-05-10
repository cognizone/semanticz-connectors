plugins {
    id("semanticz.library")
}

project.description = "Neptune connector"

dependencies {
    implementation(project(":semanticz-connectors-common"))
    implementation(libs.vavr)
    implementation(libs.httpclient)
    implementation(libs.jena.arq)
    implementation(libs.jena.rdfconnection)
    testImplementation(libs.junit.jupiter)
    testImplementation (libs.mockito.core)
    testImplementation (libs.mockito.junit.jupiter)
}