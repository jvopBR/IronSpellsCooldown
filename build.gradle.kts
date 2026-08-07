import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    `java-library`
    `maven-publish`
    id("net.neoforged.moddev") version "2.0.142"
    idea
}

// Propriedades do gradle.properties. Em Kotlin DSL nao sao variaveis implicitas como no Groovy;
// precisam ser declaradas com delegacao `by project`.
val mod_version: String by project
val mod_group_id: String by project
val mod_id: String by project
val mod_name: String by project
val mod_license: String by project
val minecraft_version: String by project
val minecraft_version_range: String by project
val neo_version: String by project
val neo_version_range: String by project
val loader_version_range: String by project
val parchment_mappings_version: String by project
val parchment_minecraft_version: String by project
val irons_spellbooks_version: String by project
val irons_lib_version: String by project
val geckolib_version: String by project
val player_animation_version: String by project
val curios_version: String by project

version = mod_version
group = mod_group_id

// Pasta mods de uma instancia, usada pela task 'deployToInstance' (dev, opcional).
// Defina instance_mods_dir no seu ~/.gradle/gradle.properties (fora do repo).
val instanceModsDir = findProperty("instance_mods_dir")

// Jars usados SO pelo dev client (runClient). Ausentes (CI), o build segue: a compilacao usa o
// Modrinth, so a execucao precisa deles. rootProject.file porque com Stonecutter este build roda
// no contexto de cada versao (versions/<v>/); sem Stonecutter, rootProject == project.
val devRuntimeJars = listOf(
    "libs/irons_spellbooks-$irons_spellbooks_version.jar",
    "libs/irons_lib-$irons_lib_version.jar",
    "libs/geckolib-neoforge-$geckolib_version.jar",
    "libs/player-animation-lib-forge-$player_animation_version.jar",
    "libs/curios-neoforge-$curios_version.jar",
).map { rootProject.file(it) }

val accessTransformerFile = rootProject.file("libs/irons_spellbooks_at.cfg")
val devClientReady = devRuntimeJars.all { it.exists() } && accessTransformerFile.exists()

sourceSets {
    main {
        resources {
            srcDir(rootProject.file("src/generated/resources"))
            exclude("**/*.bbmodel")
            exclude("src/generated/**/.cache")
        }
    }
}

repositories {
    // Iron's Spells 'n Spellbooks e Iron's Lib, para compilar contra a API deles (via Modrinth,
    // para o build funcionar em maquina limpa e no CI).
    maven("https://api.modrinth.com/maven") {
        name = "Modrinth"
        content { includeGroup("maven.modrinth") }
    }
}

base {
    archivesName.set(mod_id)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

neoForge {
    version = neo_version

    parchment {
        mappingsVersion = parchment_mappings_version
        minecraftVersion = parchment_minecraft_version
    }

    // AT do Iron's Spells: sem isso o mod quebra com IllegalAccessError no boot do dev client.
    // So se aplica quando ha dev client montado (libs/ presente).
    if (devClientReady) {
        accessTransformers.from(accessTransformerFile)
    }

    // Client-only: sem run de server, gametest nem datagen.
    runs {
        create("client") {
            client()
        }
        configureEach {
            logLevel = org.slf4j.event.Level.INFO
        }
    }

    mods {
        create(mod_id) {
            sourceSet(sourceSets["main"])
        }
    }
}

// Configuration 'localRuntime': dependencias presentes no runtime de teste mas nao publicadas.
val localRuntime by configurations.creating
configurations.named("runtimeClasspath") {
    extendsFrom(localRuntime)
}

dependencies {
    // A API que este mod le. compileOnly porque nao republicamos a dependencia.
    compileOnly("maven.modrinth:irons-spells-n-spellbooks:$irons_spellbooks_version")
    compileOnly("maven.modrinth:irons-lib:$irons_lib_version")

    // As 5 dependencias do Iron's Spells, so para o runClient. Sem libs/ (CI), sao puladas.
    if (devClientReady) {
        devRuntimeJars.forEach { localRuntime(files(it)) }
    }
}

// Copia o jar buildado para a pasta mods de uma instancia do CurseForge (dev).
tasks.register<Copy>("deployToInstance") {
    description = "Copia o jar para a pasta mods de uma instancia (requer instance_mods_dir)"
    group = "spellcooldownhud"

    from(tasks.named("jar"))
    into(instanceModsDir ?: layout.buildDirectory.dir("deploy-set-instance_mods_dir"))

    doFirst {
        if (instanceModsDir == null) {
            throw GradleException(
                "Defina 'instance_mods_dir' em ~/.gradle/gradle.properties com o caminho da pasta mods da sua instancia."
            )
        }
    }
    doLast {
        logger.lifecycle("Deploy OK -> $instanceModsDir")
    }
}

// Expande as propriedades no neoforge.mods.toml (template).
val generateModMetadata by tasks.registering(ProcessResources::class) {
    val replaceProperties = mapOf(
        "minecraft_version" to minecraft_version,
        "minecraft_version_range" to minecraft_version_range,
        "neo_version" to neo_version,
        "neo_version_range" to neo_version_range,
        "loader_version_range" to loader_version_range,
        "mod_id" to mod_id,
        "mod_name" to mod_name,
        "mod_license" to mod_license,
        "mod_version" to mod_version,
    )
    inputs.properties(replaceProperties)
    expand(replaceProperties)
    from(rootProject.file("src/main/templates"))
    into(layout.buildDirectory.dir("generated/sources/modMetadata"))
}
sourceSets["main"].resources.srcDir(generateModMetadata)
neoForge.ideSyncTask(generateModMetadata)

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            // uri(File) monta file:///C:/... corretamente; interpolar o caminho do Windows gera URI invalida.
            url = uri(rootProject.file("repo"))
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
