plugins {
    application
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "cc.tweaked"
version = "1.0-SNAPSHOT"
val modVersion = "1.106.1"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
    maven("https://squiddev.cc/maven") {
        content {
            includeGroup("cc.tweaked")
            includeModule("org.squiddev", "Cobalt")
        }
    }
}

dependencies {
    implementation("cc.tweaked:cc-tweaked-1.19.4-core:$modVersion")

    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("com.google.guava:guava") {
        version { strictly("22.0") }
    }
    implementation("it.unimi.dsi:fastutil:8.3.0")
    implementation("org.ow2.asm:asm:8.0.1")
    implementation("org.apache.commons:commons-lang3:3.6")
    implementation("io.netty:netty-all:4.1.52.Final")

    val otVersion = "1.21.0"
    implementation(platform("io.opentelemetry:opentelemetry-bom:$otVersion"))
    implementation(platform("io.opentelemetry:opentelemetry-bom-alpha:$otVersion-alpha"))

    // Tracing
    implementation("io.opentelemetry:opentelemetry-api")
    implementation("io.opentelemetry:opentelemetry-api")
    implementation("io.opentelemetry:opentelemetry-sdk")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    implementation("io.opentelemetry:opentelemetry-extension-trace-propagators")
    implementation("io.opentelemetry:opentelemetry-semconv")

    runtimeOnly("org.apache.logging.log4j:log4j-core:2.19.0")
    runtimeOnly("io.opentelemetry.instrumentation:opentelemetry-log4j-context-data-2.17-autoconfigure:$otVersion-alpha")
    runtimeOnly("io.grpc:grpc-netty:1.40.1") {
        exclude(mapOf("group" to "io.netty")) // We bundle our own netty above.
    }
    runtimeOnly("org.apache.logging.log4j:log4j-slf4j2-impl:2.19.0")
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
