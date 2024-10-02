plugins {
    id("semanticz.library")
}

project.description = "Stardog connector"

dependencies {
    implementation(project(":semanticz-connectors-common"))
    implementation(libs.jena.arq)
    testImplementation(libs.junit.jupiter)
}
