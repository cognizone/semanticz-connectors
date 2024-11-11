plugins {
    `java-library`
    pmd
    jacoco
    id("io.freefair.lombok") version "8.10"
    id("maven-publish")
    id("signing")
}


repositories {
    mavenCentral()
}


group = "zone.cogni.semanticz"


version = project.version

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set("semanticz-connectors")
                description.set("This repository contains connectors to RDF stores. Its goal is to provide unified API for accessing various back-end implementations and to optimize them in future with implementations more optimized towards individual stores.")
                url.set("https://github.com/cognizone/semanticz-connectors")

                scm {
                    connection.set("scm:git@github.com:cognizone/semanticz-connectors.git")
                    developerConnection.set("scm:git@github.com:cognizone/semanticz-connectors.git")
                    url.set("https://github.com/cognizone/semanticz-connectors")
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
                        email.set("semanticz@cogni.zone")
                    }
                }
            }
        }
    }

    repositories {
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


signing {
    if (project.hasProperty("publishToMavenCentral")) {
        sign(publishing.publications["mavenJava"])
    }
}
