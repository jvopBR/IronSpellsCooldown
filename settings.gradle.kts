pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.kikugie.dev/releases")
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.7"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        // NeoForge (moddev moderno): id == versao do MC. Loaders legado (Forge / NeoForge 1.20.1):
        // id com sufixo do loader, pra distinguir dois loaders na mesma versao do MC.
        versions("1.21.1", "1.21")
        version("1.20.1-forge", "1.20.1")
        version("1.20.1-neoforge", "1.20.1")
        vcsVersion = "1.21.1"
    }
}

rootProject.name = "SpellCooldownHud"
