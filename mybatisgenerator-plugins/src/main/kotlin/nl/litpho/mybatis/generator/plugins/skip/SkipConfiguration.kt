package nl.litpho.mybatis.generator.plugins.skip

import com.fasterxml.jackson.annotation.JsonMerge
import nl.litpho.mybatis.generator.plugins.PluginConfiguration
import org.mybatis.generator.api.IntrospectedTable

data class SkipYaml(
    @JsonMerge
    var ignorePrefixes: MutableList<String> = mutableListOf(),
    @JsonMerge
    var ignoreSuffixes: MutableList<String> = mutableListOf(),
    @JsonMerge
    var ignoreTables: MutableList<String> = mutableListOf(),
) {
    fun toConfiguration(): SkipConfiguration = SkipConfiguration(this)
}

class SkipConfiguration(skipYaml: SkipYaml) : PluginConfiguration {

    private val ignoreTables = skipYaml.ignoreTables.toList()
    private val ignorePrefixes = skipYaml.ignorePrefixes.toList()
    private val ignoreSuffixes = skipYaml.ignoreSuffixes.toList()

    fun isIgnored(introspectedTable: IntrospectedTable): Boolean =
        isIgnored(introspectedTable.fullyQualifiedTableNameAtRuntime)

    private fun isIgnored(table: String): Boolean =
        ignoreTables.any { table.equals(it, ignoreCase = true) } ||
            ignorePrefixes.any { table.startsWith(it, ignoreCase = true) } ||
            ignoreSuffixes.any { table.endsWith(it, ignoreCase = true) }
}
