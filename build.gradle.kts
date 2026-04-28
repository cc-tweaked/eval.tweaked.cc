plugins {
    application
    id("com.gradleup.shadow") version "9.3.0"
}

group = "cc.tweaked"
version = "1.0"
val modVersion = "1.118.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()

    exclusiveContent {
        forRepository {
            maven("https://maven.squiddev.cc")
        }
        filter {
            includeGroup("cc.tweaked")
        }
    }
}

dependencies {
    implementation("cc.tweaked:cc-tweaked-1.20.1-core:$modVersion")

    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("com.google.guava:guava:33.5.0-jre")

    // Instrumentation
    val otVersion = "1.58.0"
    implementation(platform("io.opentelemetry:opentelemetry-bom:$otVersion"))

    implementation("io.opentelemetry:opentelemetry-api")
    implementation("io.opentelemetry:opentelemetry-api")
    implementation("io.opentelemetry:opentelemetry-sdk")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp") {
        // Disable the okhttp exporter and use the JDK one instead.
        exclude(group = "io.opentelemetry", module = "opentelemetry-exporter-sender-okhttp")
    }
    runtimeOnly("io.opentelemetry:opentelemetry-exporter-sender-jdk")

    implementation("io.opentelemetry:opentelemetry-extension-trace-propagators")

    implementation("io.opentelemetry.semconv:opentelemetry-semconv:1.37.0")

    // Force a more recent Netty version
    runtimeOnly(platform("io.netty:netty-bom:4.2.9.Final"))
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
    minimize {
        exclude(dependency("io.opentelemetry:opentelemetry-exporter-sender-jdk:.*"))
    }
}

tasks.withType(AbstractArchiveTask::class.java).configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    filePermissions {}
    dirPermissions {}
}
