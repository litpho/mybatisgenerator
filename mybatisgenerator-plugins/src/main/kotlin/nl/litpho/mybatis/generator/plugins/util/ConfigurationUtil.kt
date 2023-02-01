package nl.litpho.mybatis.generator.plugins.util

import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import nl.litpho.mybatis.generator.plugins.PluginConfiguration
import java.io.File
import java.io.FileInputStream
import kotlin.reflect.KClass

fun <T> readConfigurationsFromYaml(configurationLocation: String, generator: () -> T): T =
    readConfigurationsFromYaml(configurationLocation.split(','), generator)

private fun <T> readConfigurationsFromYaml(configurationLocations: List<String>, generator: () -> T): T =
    generator.invoke().also { data ->
        val reader = YAMLMapper(YAMLFactory()).apply {
            registerModule(kotlinModule())
        }.readerForUpdating(data)

        configurationLocations
            .map { location -> println(location); location }
            .map { location -> FileInputStream(validateConfigurationLocation(location)) }
            .forEach { fis -> fis.use { reader.readValue(it) } }
    }

private fun validateConfigurationLocation(configurationLocation: String): File =
    File(configurationLocation).also {
        if (!it.exists()) {
            throw RuntimeException("Configuration file $configurationLocation not found")
        }
    }

class ConfigurationUtil {
    companion object {
        val pluginConfigurations: MutableMap<KClass<out PluginConfiguration>, PluginConfiguration> = mutableMapOf()

        inline fun <reified T : PluginConfiguration> getPluginConfigurationNull(): T? =
            pluginConfigurations[T::class] as T?

        inline fun <reified T : PluginConfiguration> getPluginConfigurationRequired(): T = getPluginConfigurationNull()
            ?: throw IllegalStateException("No PluginConfiguration for class '${T::class.java.canonicalName}' found")

        fun addConfiguration(pluginConfiguration: PluginConfiguration) {
            pluginConfigurations[pluginConfiguration::class] = pluginConfiguration
        }
    }
}
