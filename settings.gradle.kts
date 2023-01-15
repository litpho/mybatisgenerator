pluginManagement {
    plugins {
        val kotlinVersion: String by settings
        id("org.jetbrains.kotlin.jvm") version kotlinVersion
    }
}

rootProject.name = "mybatisgenerator"

include("mybatisgenerator-library")
include("mybatisgenerator-plugins")
include("mybatisgenerator-snowflake-library")
