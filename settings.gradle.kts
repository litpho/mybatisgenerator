pluginManagement {
    plugins {
        val kotlinVersion: String by settings
        id("org.jetbrains.kotlin.jvm") version kotlinVersion
    }
}

rootProject.name = "mybatisgenerator"

include("mybatis-library")
include("mybatisgenerator-plugins")
