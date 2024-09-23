plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
    implementation("pl.allegro.tech.build:axion-release-plugin:1.18.8")
    implementation("org.owasp:dependency-check-gradle:10.0.4")
}