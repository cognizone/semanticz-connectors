plugins {
    java
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
}

group = "zone.cogni.example.springwithfuseki"
version = "0.0.1-SNAPSHOT"
project.description = "Example combining Spring and Fuseki"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("zone.cogni.semanticz:semanticz-connectors-spring:1.0.0")
    implementation("zone.cogni.semanticz:semanticz-connectors-common:1.0.0")
    implementation("org.apache.jena:jena-arq:4.10.0")

    implementation("org.springframework.boot:spring-boot-starter-web")
}