package nl.litpho.mybatis.generator.plugins.subpackage

import nl.litpho.mybatis.generator.plugins.PluginConfiguration

data class SubpackageYaml(
    var prefixes: MutableList<Prefix> = mutableListOf(),
    var subpackages: MutableMap<String, List<String>> = mutableMapOf(),
) {

    fun toConfiguration(): SubpackageConfiguration = SubpackageConfiguration(this)

    data class Prefix(
        var prefix: String? = null,
        var subpackage: String? = null,
    )
}

class SubpackageConfiguration(subpackageYaml: SubpackageYaml) : PluginConfiguration {

    private val prefixes: Map<String, String> =
        subpackageYaml.prefixes.associate { it.prefix!! to it.subpackage!! }

    private val tables: Map<String, String> =
        subpackageYaml.subpackages.flatMap { entry -> entry.value.map { value -> value to entry.key } }.toMap()

    fun getSubpackage(table: String): String = tables[table] ?: getPrefixedTable(table) ?: ""

    private fun getPrefixedTable(table: String): String? =
        prefixes.entries.filter { table.startsWith(it.key) }.map { it.value }.firstOrNull()
}
