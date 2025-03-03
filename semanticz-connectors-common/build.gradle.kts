plugins {
    id("semanticz.library")
    alias(libs.plugins.lombok)
}

project.description = "Connector utilities"

dependencies {
    implementation(libs.jena.arq)
    implementation(libs.jena.rdfconnection)
    implementation(libs.junit.jupiter)
}