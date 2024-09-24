plugins {
    id("semanticz.library")
}

project.description = "Connector utilities"

dependencies {
    implementation(libs.guava)
    implementation(libs.jena.arq)
    implementation(libs.jena.rdfconnection)
    implementation(libs.junit.jupiter)
    implementation(libs.assertj.core)
}

tasks.test {
    useJUnitPlatform()
}
