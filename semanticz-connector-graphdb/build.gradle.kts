plugins {
    id("semanticz.library")
    alias(libs.plugins.lombok)
}

project.description = "GraphDB connector"

dependencies {
    implementation(project(":semanticz-connectors-common"))
    implementation(libs.jena.arq)
    testImplementation(libs.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
}
