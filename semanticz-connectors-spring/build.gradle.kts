plugins {
    id("semanticz.library")
    alias(libs.plugins.lombok)
}

project.description = "Spring utilities"

dependencies {
    implementation(project(":semanticz-connectors-common"))
    implementation(project(":semanticz-connector-fuseki"))
    implementation(project(":semanticz-connector-graphdb"))
    implementation(project(":semanticz-connector-stardog"))
    implementation(project(":semanticz-connector-virtuoso"))

    implementation(libs.guava)
    implementation(libs.spring.core)
    implementation(libs.spring.context)
}

tasks.test {
    useJUnitPlatform()
}