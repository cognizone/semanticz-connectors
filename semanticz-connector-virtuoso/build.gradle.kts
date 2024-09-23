plugins {
    id("semanticz.library")
}

project.description = "Virtuoso connector"

dependencies {
    implementation(project(":semanticz-connectors-common"))
    implementation(libs.vavr)
    implementation(libs.spring.web)
    implementation(libs.jena.arq)
    implementation(libs.jena.rdfconnection)
    testImplementation(libs.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
}
