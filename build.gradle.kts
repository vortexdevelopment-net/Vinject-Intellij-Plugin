import org.jetbrains.kotlin.ir.backend.js.compile

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij.platform") version "2.3.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "net.vortexdevelopment"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
    jcenter()
    maven("https://jitpack.io")
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation("org.jetbrains:annotations:26.0.1")
    implementation("com.github.jagrosh:DiscordIPC:master-SNAPSHOT")
    intellijPlatform {
        intellijIdeaCommunity("2024.3.5")

        bundledPlugin("com.intellij.java")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
//intellij {
//    version.set("2024.2")
//    type.set("IC") // Target IDE Platform
//
//    plugins.set(listOf("java", "maven", "gradle"))
//}

sourceSets {
    main {
        java.srcDirs("src/main/java")
        resources.srcDirs("src/main/resources")
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
        }
    }

    withType<Jar> {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest {
            attributes["Main-Class"] = "net.vortexdevelopment.plugin.vinject.Plugin"
        }
    }

    withType<ProcessResources> {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    shadowJar {
        archiveClassifier.set("shadow")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        // Include all dependencies in the shadow jar
        from(sourceSets.main.get().output)

        configurations = listOf(project.configurations.runtimeClasspath.get())

        // Include DiscordIPC and required dependencies
        dependencies {
            include(dependency("com.github.jagrosh:DiscordIPC:.*"))
            include(dependency("org.json:json:.*"))
            include(dependency("org.slf4j:slf4j-api:.*"))
            // Exclude Kotlin/coroutines as they're provided by IntelliJ
            exclude(dependency("org.jetbrains.kotlin:.*"))
            exclude(dependency("org.jetbrains.kotlinx:.*"))
        }

        // Relocate to avoid conflicts
        relocate("org.json", "net.vortexdevelopment.plugin.vinject.lib.org.json")
        relocate("org.slf4j", "net.vortexdevelopment.plugin.vinject.lib.org.slf4j")

        // Exclude Kotlin and coroutines classes - use IntelliJ's versions
        exclude("kotlin/**")
        exclude("kotlinx/**")
        exclude("META-INF/kotlin/**")
        exclude("META-INF/versions/**")

        // Exclude signing and manifest files
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
        exclude("META-INF/DEPENDENCIES", "META-INF/LICENSE*", "META-INF/NOTICE*")

        mergeServiceFiles()
    }

    // Configure the regular jar task to include dependencies from shadowJar
    jar {
        // Include the shadowJar contents in the regular jar
        dependsOn(shadowJar)
        from(zipTree(shadowJar.get().archiveFile))

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        // Exclude conflicting files again to be safe
        exclude("kotlin/**")
        exclude("kotlinx/**")
        exclude("META-INF/kotlin/**")
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }

    buildPlugin {
        // Now depends on the jar that includes our dependencies
        dependsOn(jar)
    }

    prepareSandbox {
        // Now depends on the jar that includes our dependencies
        dependsOn(jar)
    }

    patchPluginXml {
        sinceBuild.set("232")
        untilBuild.set("252.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
