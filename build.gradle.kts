import groovy.xml.dom.DOMCategory.attributes

plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "dev.emortal"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()

    maven("https://repo.emortal.dev/snapshots")
    maven("https://repo.emortal.dev/releases")

    maven("https://jitpack.io")
}

dependencies {
    // Used for utils package, parsing protobufs from json
    implementation("com.google.protobuf:protobuf-java-util:3.21.12")

    implementation("com.github.Minestom:Minestom:eb06ba8664")
    implementation("com.github.EmortalMC:TNT:4ef1b53482")

    implementation("dev.emortal.minestom:core:6d09fe5")
    implementation("net.kyori:adventure-text-minimessage:4.12.0")

//    implementation("dev.emortal.minestom:core:local")

    implementation("dev.emortal.api:kurushimi-sdk:e0ddc5a")
}

tasks {
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        mergeServiceFiles()

        manifest {
            attributes (
                "Main-Class" to "dev.emortal.minestom.lobby.Entrypoint",
                "Multi-Release" to true
            )
        }
    }

    withType<AbstractArchiveTask> {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }

    build { dependsOn(shadowJar) }

}