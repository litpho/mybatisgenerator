package nl.litpho.mybatis.generator.plugins.util

import nl.litpho.mybatis.generator.plugins.PluginConfiguration
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileInputStream
import kotlin.reflect.KClass

inline fun <reified T> readConfigurationFromYaml(configurationLocation: String): T =
    FileInputStream(validateConfigurationLocation(configurationLocation)).use {
        Yaml().run { loadAs(it, T::class.java) }
    }

fun validateConfigurationLocation(configurationLocation: String): File =
    File(configurationLocation).also {
        if (!it.exists()) {
            throw RuntimeException("Configuration file $configurationLocation not found")
        }
    }

class ConfigurationUtil {
    companion object {
        val pluginConfigurations: MutableMap<KClass<out PluginConfiguration>, PluginConfiguration> = mutableMapOf()

        inline fun <reified T : PluginConfiguration> getPluginConfigurationNull(): T? = pluginConfigurations[T::class] as T?

        inline fun <reified T : PluginConfiguration> getPluginConfigurationRequired(): T = getPluginConfigurationNull()
            ?: throw IllegalStateException("No PluginConfiguration for class '${T::class.java.canonicalName}' found")

        fun addConfiguration(pluginConfiguration: PluginConfiguration) {
            pluginConfigurations[pluginConfiguration::class] = pluginConfiguration
        }
    }
}
