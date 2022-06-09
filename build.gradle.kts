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

val modVersion = "1.100.6"

dependencies {
    implementation("org.squiddev:cc-tweaked-1.16.5:${modVersion}")

    implementation("org.apache.logging.log4j:log4j-api:2.16.0")
    implementation("com.google.guava:guava") {
        version { strictly("22.0") }
    }
    implementation("it.unimi.dsi:fastutil:8.3.0")
    implementation("org.ow2.asm:asm:8.0.1")
    implementation("org.apache.commons:commons-lang3:3.6")
    implementation("io.netty:netty-all:4.1.52.Final")

    var otVersion = "1.5.0"
    implementation(platform("io.opentelemetry:opentelemetry-bom:$otVersion"))

    // Tracing
    implementation("io.opentelemetry:opentelemetry-api")
    implementation("io.opentelemetry:opentelemetry-sdk")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    implementation("io.opentelemetry:opentelemetry-extension-trace-propagators")
    implementation("io.opentelemetry:opentelemetry-semconv:$otVersion-alpha")

    // Metrics
    implementation("io.opentelemetry:opentelemetry-api-metrics:$otVersion-alpha")
    implementation("io.opentelemetry:opentelemetry-sdk-metrics:$otVersion-alpha")
    implementation("io.opentelemetry:opentelemetry-exporter-prometheus:$otVersion-alpha")
    implementation("io.prometheus:simpleclient_common:0.11.0")

    runtimeOnly("org.apache.logging.log4j:log4j-core:2.16.0")
    runtimeOnly("io.opentelemetry.instrumentation:opentelemetry-log4j-2.13.2:$otVersion-alpha")
    runtimeOnly("io.grpc:grpc-netty:1.40.1") {
        exclude(mapOf("group" to "io.netty")) // We bundle our own netty above.
    }
    runtimeOnly("org.slf4j:slf4j-log4j12:1.7.32")
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

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    mergeServiceFiles()
}
