import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // Applies the correct loom variant based on the Minecraft version, and wires Stonecutter's
    // per-version generated sources into the Kotlin/Java source sets.
    id("dev.kikugie.loom-back-compat")
    kotlin("jvm") version "2.4.0"
    id("com.github.gmazzo.buildconfig") version "5.5.1"
}

version = "${property("mod.version")}+${sc.current.version}"
group = property("mod.group") as String

base {
    // Version is automatically appended by Gradle, so just include base name + MC version
    archivesName = "${property("mod.archives_base_name")}-mc${sc.current.version}"
}

repositories {
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
    maven("https://api.modrinth.com/maven")
    maven("https://maven.fabricmc.net/")
    maven("https://libraries.minecraft.net/")
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    // Applies Mojang Mappings on obfuscated versions; no-op on 26.1.2/26.2, which already ship unobfuscated.
    loomx.applyMojangMappings()

    implementation(include("io.github.classgraph:classgraph:${sc.properties["deps.class_graph"] as String}")!!)

    implementation("net.fabricmc:fabric-loader:${sc.properties["deps.fabric_loader"] as String}")
    implementation("net.fabricmc.fabric-api:fabric-api:${sc.properties["deps.fabric_api"] as String}")
    implementation("net.fabricmc:fabric-language-kotlin:${sc.properties["deps.fabric_kotlin"] as String}")

    runtimeOnly("me.djtheredstoner:DevAuth-fabric:1.2.1")
    // DevAuth needs Apache HttpClient at runtime but doesn't declare it as a dependency -
    // Minecraft used to bundle it as a vanilla library but 26.1+ no longer does.
    runtimeOnly("org.apache.httpcomponents:httpclient:4.5.14")
}

// Compile options
tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}

val requiredJava = property("mod.java_version") as String

kotlin {
    jvmToolchain(requiredJava.toInt())
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(requiredJava))
    }

    // withSourcesJar()
}

tasks.jar {
    inputs.property("archivesName", base.archivesName)

    // rootDir is the true settings.gradle.kts directory regardless of how deep Stonecutter
    // nests the active version's generated subproject.
    from("$rootDir/LICENSE") {
        rename { "${it}_${base.archivesName.get()}" }
    }
}

// ---------------------
// Resource processing
// ---------------------

buildConfig {
    packageName(property("mod.group") as String)

    buildConfigField("String", "MOD_ID", "\"${property("mod.id")}\"")
    buildConfigField("String", "MOD_NAME", "\"${property("mod.name")}\"")
    buildConfigField("String", "MOD_VERSION", "\"${property("mod.version")}\"")
}

val expandProperties = mapOf(
    "mod_id" to property("mod.id") as String,
    "mod_version" to property("mod.version") as String,
    "mod_name" to property("mod.name") as String,
    "maven_group" to property("mod.group") as String,
    "mod_class" to property("mod.class") as String,
    "minecraft_version" to "~${sc.current.version}",
    "loader_version" to (sc.properties["deps.fabric_loader"] as String),
    "java_version" to requiredJava,
)

tasks.processResources {
    filesMatching(listOf("fabric.mod.json", "**/*.mixins.json")) {
        expand(expandProperties)
    }
}
