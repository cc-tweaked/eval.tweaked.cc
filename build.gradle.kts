plugins {
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "cc.tweaked"
version = "1.0-SNAPSHOT"
val modVersion = "1.108.0"

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

    implementation("org.slf4j:slf4j-api:2.0.0")
    implementation("com.google.guava:guava:31.1-jre")

    val otVersion = "1.29.0"
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
    runtimeOnly("io.grpc:grpc-netty:1.40.1")
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
    minimize() {
        exclude(dependency("org.apache.logging.log4j:.*:.*"))
        exclude(dependency("io.opentelemetry.*:.*:.*"))
    }
}

tasks.withType(AbstractArchiveTask::class.java).configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    dirMode = Integer.valueOf("755", 8)
    fileMode = Integer.valueOf("664", 8)
}
