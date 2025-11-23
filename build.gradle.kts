import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.2.21"
    id("fabric-loom") version "1.10-SNAPSHOT"
    id("maven-publish")
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

base {
    archivesName.set(project.property("archives_base_name") as String)
}

val targetJavaVersion = 21
java {
    toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    withSourcesJar()
}

loom {

    mods {
        register("scs") {
            sourceSet("main")
        }
    }

    runs {
        configureEach {
            ideConfigGenerated(true)
        }
    }
}

repositories {
    maven("https://maven.fabricmc.net/")
    mavenCentral()
}


dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${project.property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${project.property("kotlin_loader_version")}")

    modImplementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_api_version")}")



}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("minecraft_version", project.property("minecraft_version"))
    inputs.property("loader_version", project.property("loader_version"))
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "minecraft_version" to project.property("minecraft_version"),
            "loader_version" to project.property("loader_version"),
            "kotlin_loader_version" to project.property("kotlin_loader_version")
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)

    options.compilerArgs.add("-g:none")
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget(targetJavaVersion.toString()))

    compilerOptions {
        freeCompilerArgs.add("-Xno-param-assertions")
        freeCompilerArgs.add("-Xno-call-assertions")
        freeCompilerArgs.add("-Xno-receiver-assertions")
    }
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${project.base.archivesName}" }
    }

    exclude("**/*.kt")
    exclude("**/*.java")
    exclude("**/META-INF/versions/**")

    manifest {
        attributes(
            mapOf(
                "Implementation-Title" to "ScS Fabric Mod",
                "Implementation-Version" to project.version
            )
        )
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.named("remapJar") {
    doLast {
    }
}

tasks.register("obfuscate") {
    group = "build"
    description = "Применяет мощную обфускацию к моду"

    dependsOn("remapJar")

    doLast {
        println("Обфускация применена к моду")
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = project.property("archives_base_name") as String
            from(components["java"])
        }
    }

    repositories {
    }
}
