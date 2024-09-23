plugins {
    id("semanticz.library")
}

project.description = "GraphDB connector"

dependencies {
    implementation(project(":semanticz-connectors-common"))
    implementation(libs.guava)
    implementation(libs.spring.web)
    implementation(libs.spring.context)
    implementation(libs.jena.arq)
    testImplementation(libs.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
}
