import net.neoforged.moddevgradle.dsl.ModDevExtension
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import net.neoforged.moddevgradle.legacyforge.dsl.LegacyForgeExtension
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    `java-library`
    `maven-publish`
    // Both ModDevGradle variants ship in the same artifact and are declared apply-false: they land
    // on every node's classpath, but exactly one is applied per node depending on its loader (see
    // below). moddev = modern NeoForge (>=1.20.2); legacyforge = MinecraftForge + NeoForge 1.20.1.
    id("net.neoforged.moddev") version "2.0.142" apply false
    id("net.neoforged.moddev.legacyforge") version "2.0.142" apply false
    idea
}

// gradle.properties values. In Kotlin DSL they aren't implicit variables like in Groovy;
// they must be declared with `by project` delegation.
val mod_version: String by project
val mod_group_id: String by project
val mod_id: String by project
val mod_name: String by project
val mod_license: String by project
val loader_version_range: String by project

// Per-Minecraft-version values, chosen by the active Stonecutter node. Kept here rather than in
// gradle.properties because they differ between 1.21 and 1.21.1.
val minecraft_version = stonecutter.current.version

// Loader is read from the node-id suffix (e.g. "1.20.1-forge"). Bare nodes ("1.21.1", "1.21") are
// modern NeoForge. Modern moddev only supports NeoForge >=1.20.2, so all Forge and NeoForge 1.20.1
// go through the legacyforge plugin instead.
val nodeId = stonecutter.current.project
val loader = when {
    nodeId.endsWith("-forge") -> "forge"
    nodeId.endsWith("-neoforge") -> "neoforge"
    else -> "neoforge"
}
val useLegacyPlugin = loader == "forge" || minecraft_version == "1.20.1"

// Per-node build metadata, keyed on the node id (which carries the loader) so the two loaders at
// MC 1.20.1 can differ. loaderVersion = NeoForge or Forge coordinate; ironsSpellbooks/ironsLib =
// the compileOnly deps (Modrinth). Dev-only libs (geckolib/curios/...) stay in gradle.properties.
val nodeSpec: Map<String, String> = when (nodeId) {
    "1.21.1" -> mapOf(
        "loaderVersion" to "21.1.241", "loaderRange" to "[21.1.0,)",
        "parchmentMc" to "1.21.1", "parchmentMappings" to "2024.11.17",
        "mcRange" to "[1.21.1]",
        "ironsSpellbooks" to "1.21.1-3.16.2", "ironsLib" to "1.21.1-2.1.0",
    )
    "1.21" -> mapOf(
        "loaderVersion" to "21.0.167", "loaderRange" to "[21.0.0,)",
        "parchmentMc" to "1.21", "parchmentMappings" to "2024.07.28",
        "mcRange" to "[1.21]",
        "ironsSpellbooks" to "1.21.1-3.16.2", "ironsLib" to "1.21.1-2.1.0",
    )
    "1.20.1-forge" -> mapOf(
        "loaderVersion" to "1.20.1-47.3.0", "loaderRange" to "[47,)",
        "parchmentMc" to "1.20.1", "parchmentMappings" to "2023.09.03",
        "mcRange" to "[1.20.1]",
        "ironsSpellbooks" to "1.20.1-3.16.2", "ironsLib" to "1.20.1-2.1.0",
    )
    // NeoForge 1.20.1 (47.x) is a soft-fork of Forge 47.x under the same net.minecraftforge API, so
    // the source is shared with the Forge node via `//? if <1.21`. Iron's Spells is frozen at 3.4.0
    // here; that version predates the irons-lib split, so there is no irons-lib dependency.
    "1.20.1-neoforge" -> mapOf(
        "loaderVersion" to "1.20.1-47.1.106", "loaderRange" to "[47,)",
        "parchmentMc" to "1.20.1", "parchmentMappings" to "2023.09.03",
        "mcRange" to "[1.20.1]",
        "ironsSpellbooks" to "1.20.1-3.4.0",
    )
    else -> error("Unmapped node: $nodeId")
}
val neo_version = nodeSpec.getValue("loaderVersion")
val neo_version_range = nodeSpec.getValue("loaderRange")
val parchment_minecraft_version = nodeSpec.getValue("parchmentMc")
val parchment_mappings_version = nodeSpec.getValue("parchmentMappings")
val minecraft_version_range = nodeSpec.getValue("mcRange")
val irons_spellbooks_version = nodeSpec.getValue("ironsSpellbooks")
// Nullable: NeoForge 1.20.1 (Iron's Spells 3.4.0) predates the irons-lib split, so it has none.
val irons_lib_version = nodeSpec["ironsLib"]
val geckolib_version: String by project
val player_animation_version: String by project
val curios_version: String by project

// Resource-pack format for pack.mcmeta, by MC version (1.20.1 = 15, 1.21 / 1.21.1 = 34). Without a
// pack.mcmeta, Forge 1.20.1 warns "failed to load a valid ResourcePackInfo" and skips the assets.
val pack_format = when (minecraft_version) {
    "1.21.1", "1.21" -> "34"
    "1.20.1" -> "15"
    else -> error("No pack_format mapped for Minecraft $minecraft_version")
}

// Apply only this node's loader plugin (both entered the classpath as apply-false above).
if (useLegacyPlugin) {
    apply(plugin = "net.neoforged.moddev.legacyforge")
} else {
    apply(plugin = "net.neoforged.moddev")
}

version = mod_version
group = mod_group_id

// Pasta mods de uma instancia, usada pela task 'deployToInstance' (dev, opcional).
// Defina instance_mods_dir no seu ~/.gradle/gradle.properties (fora do repo).
val instanceModsDir = findProperty("instance_mods_dir")

// Jars usados SO pelo dev client (runClient). Ausentes (CI), o build segue: a compilacao usa o
// Modrinth, so a execucao precisa deles. rootProject.file porque com Stonecutter este build roda
// no contexto de cada versao (versions/<v>/); sem Stonecutter, rootProject == project.
val devRuntimeJars = listOfNotNull(
    "libs/irons_spellbooks-$irons_spellbooks_version.jar",
    irons_lib_version?.let { "libs/irons_lib-$it.jar" },
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
    // Include the MC version and loader so every node produces a distinct, self-describing jar
    // (e.g. spellcooldownhud-1.20.1-forge-0.1.1.jar) -- needed since two loaders share MC 1.20.1,
    // and so all versions can sit side by side when testing or uploading to CurseForge.
    archivesName.set("$mod_id-$minecraft_version-$loader")
}

java {
    // Modern NeoForge (1.21.x) runs on Java 21; Forge / NeoForge 1.20.1 (legacy) on Java 17.
    toolchain.languageVersion.set(JavaLanguageVersion.of(if (useLegacyPlugin) 17 else 21))
}

// Version-specific: only the concrete plugin extension knows `version`. Forge and NeoForge 1.20.1
// (both legacy) use different Maven coordinates, so they set it differently.
if (useLegacyPlugin) {
    configure<LegacyForgeExtension> {
        if (loader == "forge") {
            version = neo_version                 // net.minecraftforge:forge (e.g. 1.20.1-47.3.0)
        } else {
            enable { neoForgeVersion = neo_version } // net.neoforged:forge (e.g. 1.20.1-47.1.106)
        }
    }
} else {
    configure<NeoForgeExtension> {
        version = neo_version
    }
}

// Shared config, written once against the base type common to both plugins (ModDevExtension).
configure<ModDevExtension> {
    // Parchment (nicer param names in dev) only on the modern path for now; the legacy plugin uses
    // MCP mappings and we don't want to entangle the two before it's verified. Dev-only anyway.
    if (!useLegacyPlugin) {
        parchment {
            mappingsVersion = parchment_mappings_version
            minecraftVersion = parchment_minecraft_version
        }
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

// Expose the loader to Stonecutter source preprocessing (`//? if forge { ... }`); the MC version
// alone can't tell the loaders apart on 1.20.1.
stonecutter.constants["forge"] = loader == "forge"
stonecutter.constants["neoforge"] = loader == "neoforge"

// Configuration 'localRuntime': dependencias presentes no runtime de teste mas nao publicadas.
val localRuntime by configurations.creating
configurations.named("runtimeClasspath") {
    extendsFrom(localRuntime)
}

dependencies {
    // A API que este mod le. compileOnly porque nao republicamos a dependencia.
    compileOnly("maven.modrinth:irons-spells-n-spellbooks:$irons_spellbooks_version")
    // irons-lib nao existe para o Iron's Spells 3.4.0 (NeoForge 1.20.1); o codigo nao importa
    // io.redspace.ironslib, entao e so pular quando o no nao tem versao de lib.
    irons_lib_version?.let { compileOnly("maven.modrinth:irons-lib:$it") }

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
        "pack_format" to pack_format,
    )
    inputs.properties(replaceProperties)
    expand(replaceProperties)
    // Modern NeoForge reads META-INF/neoforge.mods.toml; the legacy loaders (Forge and NeoForge
    // 1.20.1) read META-INF/mods.toml and differ only in the loader dependency modid (forge vs
    // neoforge), so each of the three has its own template dir.
    val templateDir = when {
        !useLegacyPlugin -> "src/main/templates"
        loader == "forge" -> "src/main/templates-forge"
        else -> "src/main/templates-neoforge-legacy"
    }
    from(rootProject.file(templateDir))
    // pack.mcmeta goes to the jar root for every loader (only pack_format differs, by MC version).
    from(rootProject.file("src/main/templates-shared"))
    into(layout.buildDirectory.dir("generated/sources/modMetadata"))
}
sourceSets["main"].resources.srcDir(generateModMetadata)
the<ModDevExtension>().ideSyncTask(generateModMetadata)

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
