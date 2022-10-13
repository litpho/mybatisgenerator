package nl.litpho.mybatis.generator.plugins.subpackage

import nl.litpho.mybatis.generator.plugins.PluginConfiguration

data class SubpackageYaml(
    var prefixes: MutableList<PrefixData> = mutableListOf(),
    var tables: MutableList<FixedTable> = mutableListOf()
) {

    fun toConfiguration(): SubpackageConfiguration = SubpackageConfiguration(this)

    data class PrefixData(
        var prefix: String? = null,
        var subpackage: String? = null
    )

    data class FixedTable(
        var name: String? = null,
        var subpackage: String? = null
    )
}

class SubpackageConfiguration(subpackageYaml: SubpackageYaml) : PluginConfiguration {

    private val prefixes: Map<String, String> =
        subpackageYaml.prefixes.associate { it.prefix!! to it.subpackage!! }

    private val tables: Map<String, String> =
        subpackageYaml.tables.associate { it.name!! to it.subpackage!! }

    fun getSubpackage(table: String): String = tables[table] ?: getPrefixedTable(table) ?: ""

    private fun getPrefixedTable(table: String): String? =
        prefixes.entries.filter { table.startsWith(it.key) }.map { it.value }.firstOrNull()
}
