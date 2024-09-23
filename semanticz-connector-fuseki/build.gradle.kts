plugins {
    id("semanticz.library")
}

project.description = "Fuseki connector"

dependencies {
    implementation(project(":semanticz-connectors-common"))
    implementation(libs.spring.web)
    implementation(libs.guava)
    implementation(libs.jena.arq)
    implementation(libs.jena.fuseki.main)
    testImplementation(libs.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
}