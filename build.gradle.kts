plugins {
    application
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "cc.tweaked"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://squiddev.cc/maven")
}

val modVersion = "1.95.3"

dependencies {
    implementation("org.apache.logging.log4j:log4j-api:2.14.1")
    implementation("org.apache.logging.log4j:log4j-core:2.14.1")
    implementation("com.google.guava:guava:22.0")
    implementation("it.unimi.dsi:fastutil:8.3.0")
    implementation("org.ow2.asm:asm:8.0.1")
    implementation("org.apache.commons:commons-lang3:3.6")

    implementation("org.squiddev:cc-tweaked-1.15.2:${modVersion}")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

application {
    mainClass.set("cc.tweaked.eval.Main")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            mapOf(
                "Implementation-Title" to "eval.tweaked.cc",
                "Implementation-Version" to modVersion,
                "Implementation-Vendor" to "SquidDev",
            )
        )
    }
}
