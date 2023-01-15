package nl.litpho.mybatis.generator.test.utils

import org.mybatis.generator.config.Configuration
import org.mybatis.generator.config.xml.ConfigurationParser
import java.io.File
import java.util.*

fun connectionProperties(
    jdbcUrl: String,
    driverClassName: String = "org.h2.Driver",
    username: String = "sa",
    password: String = "",
    projectDir: String = ".",
    generatedSourceRoot: String = "$projectDir/build/generatedSources",
): Properties =
    Properties().apply {
        this["jdbcUrl"] = jdbcUrl
        this["driverClassName"] = driverClassName
        this["username"] = username
        this["password"] = password
        this["projectDir"] = projectDir
        this["generatedSourceRoot"] = generatedSourceRoot
    }

fun createAndCleanDirectory(location: String): File =
    File(location).apply {
        deleteRecursively()
        mkdirs()
        File("$location/java").mkdirs()
        File("$location/resources").mkdirs()
    }

fun readConfigurationFromXml(location: String, warnings: MutableList<String>, extraProperties: Properties = Properties()): Configuration =
    object {}.javaClass.getResourceAsStream(location).use {
        ConfigurationParser(extraProperties, warnings).parseConfiguration(it)
    }
