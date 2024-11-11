import pl.allegro.tech.build.axion.release.domain.VersionConfig
plugins {
    `java-library`
    `maven-publish`
    pmd
    jacoco
    signing
    id("pl.allegro.tech.build.axion-release")
    id("org.owasp.dependencycheck")
}


group = "zone.cogni.semanticz"

repositories {
    mavenCentral()
}

project.extensions.configure(JavaPluginExtension::class.java) {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
    withJavadocJar()
    withSourcesJar()
}



scmVersion {
    tag {
        prefix = "v"
        versionSeparator = ""
        branchPrefix = mapOf(
            "release/.*" to "release-v",
            "hotfix/.*" to "hotfix-v"
        )
    }
    nextVersion {
        suffix = "SNAPSHOT"
        separator = "-"
    }
    versionIncrementer("incrementPatch")

    // Use nextVersion's suffix and separator in versionCreator
    versionCreator = { version, position ->
        val suffix = if (position == VersionConfig.Position.SNAPSHOT) {
            "${scmVersion.nextVersion.separator}${scmVersion.nextVersion.suffix}"
        } else {
            ""
        }
        "$version$suffix"
    }
}



version = scmVersion.version

tasks.withType<JavaCompile> {
    sourceCompatibility = JavaVersion.VERSION_11.toString()
    targetCompatibility = JavaVersion.VERSION_11.toString()
}

pmd {
    isIgnoreFailures = true
    isConsoleOutput = true
    toolVersion = "7.0.0"
    rulesMinimumPriority = 5
}

tasks.register("qualityCheck") {
    dependsOn(tasks.pmdMain)
    dependsOn(tasks.pmdTest)
    dependsOn(tasks.jacocoTestReport)
    dependsOn(tasks.dependencyCheckAnalyze)
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
}

tasks.named<Jar>("jar") {
    from("$rootDir") {
        include("LICENSE")
        into("META-INF")
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set(project.name)
                description.set(project.description)
                url.set("https://github.com/cognizone/${project.name}")

                scm {
                    connection.set("scm:git@github.com:cognizone/${project.name}.git")
                    developerConnection.set("scm:git@github.com:cognizone/${project.name}.git")
                    url.set("https://github.com/cognizone/${project.name}")
                }

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("cognizone")
                        name.set("Cognizone")
                        email.set("dev@cognizone.com")
                    }
                }
            }
        }
    }

    repositories {
            // Cognizone Nexus repository
            if (project.hasProperty("publishToCognizoneNexus")) {
                maven {
                    credentials {
                        username = System.getProperty("nexus.username")
                        password = System.getProperty("nexus.password")
                    }
                    val releasesRepoUrl = "${System.getProperty("nexus.url")}/repository/cognizone-release"
                    val snapshotsRepoUrl = "${System.getProperty("nexus.url")}/repository/cognizone-snapshot"
                    url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
                    isAllowInsecureProtocol = true
                }
            }

            // Maven Central repository
            if (project.hasProperty("publishToMavenCentral")) {
                maven {
                    credentials {
                        username = System.getProperty("ossrh.username")
                        password = System.getProperty("ossrh.password")
                    }
                    val stagingRepoUrl = "${System.getProperty("ossrh.url")}/service/local/staging/deploy/maven2"
                    val snapshotsRepoUrl = "${System.getProperty("ossrh.url")}/content/repositories/snapshots"
                    url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else stagingRepoUrl)
                }
            }
        }
}

tasks.withType<Javadoc> {
    options {
        (this as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:none", true)
    }
    isFailOnError = false
}

signing {
    if (project.hasProperty("publishToMavenCentral")) {
        sign(publishing.publications["mavenJava"])
    }
}
