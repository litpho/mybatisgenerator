package nl.litpho.mybatis.generator.plugins.naming

import nl.litpho.mybatis.generator.plugins.PluginConfiguration
import nl.litpho.mybatis.generator.plugins.util.createLogger
import org.mybatis.generator.api.IntrospectedColumn
import org.mybatis.generator.api.IntrospectedTable
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType

data class NamingYaml(
    var default: Default? = null,
    var tables: MutableMap<String, Table> = mutableMapOf(),
    var typeAliases: MutableMap<String, String> = mutableMapOf()
) {

    fun toConfiguration(): NamingConfiguration = NamingConfiguration(this)

    data class Default(
        var prefix: String? = null,
        var columns: MutableMap<String, Column> = mutableMapOf(),
        var ignoredColumns: MutableList<String> = mutableListOf()
    )

    data class Table(
        var type: String? = null,
        var prefix: String? = null,
        var columns: MutableMap<String, Column> = mutableMapOf(),
        var ignoredColumns: MutableList<String> = mutableListOf()
    )

    data class Column(
        var type: String? = null,
        var property: String? = null,
        var defaultValue: String? = null
    )
}

data class NamingConfiguration(private val namingYaml: NamingYaml) : PluginConfiguration {

    val namingConfigurationEntryMap: Map<String, NamingConfigurationEntry> =
        namingYaml.tables.map {
            it.key to NamingConfigurationEntry(
                it.value.type,
                it.value.prefix ?: namingYaml.default?.prefix ?: "",
                it.value.columns,
                namingYaml.default?.columns,
                namingYaml.typeAliases
            )
        }.toMap()

    fun getTableConfiguration(table: String?): NamingConfigurationEntry? = namingConfigurationEntryMap[table]

    fun getParseResultForType(type: String): NamingConfigurationEntry? =
        namingConfigurationEntryMap.values.firstOrNull { it.type == type }
}

class NamingConfigurationEntry(
    val type: String?,
    val prefix: String,
    columns: MutableMap<String, NamingYaml.Column>,
    defaultColumns: MutableMap<String, NamingYaml.Column>?,
    private val typeAliases: MutableMap<String, String>
) {
    val columnOverrides: Map<String, ColumnBasedJavaPropertyOverride> =
        columns.mapValues { ColumnBasedJavaPropertyOverride(it.value.property, it.value.type.applyTypeAlias()) }
    val columnDefaultValues: Map<String, String> =
        columns.filter { it.value.defaultValue != null }.mapValues { it.value.defaultValue!! }
//    val ignoredColumns: List<String> =
//        columns.filter { !it.ignore }.map { it.name!! }.toList()

    private fun String?.applyTypeAlias(): String? =
        if (this == null || !this.startsWith("$")) {
            this
        } else {
            typeAliases[this.drop(1)] ?: this
        }
}

class ColumnBasedJavaPropertyOverride(private val property: String?, private val type: String?) {

    private val logger = createLogger<ColumnBasedJavaPropertyOverride>()

    fun updateWith(introspectedTable: IntrospectedTable, introspectedColumn: IntrospectedColumn) {
        if (property != null) {
            logger.warn("Changing property for ${introspectedTable.fullyQualifiedTableNameAtRuntime}.${introspectedColumn.actualColumnName} to $property")
            introspectedColumn.javaProperty = property
        }

        if (type != null) {
            logger.warn("Changing type for ${introspectedTable.fullyQualifiedTableNameAtRuntime}.${introspectedColumn.actualColumnName} to $type")
            introspectedColumn.fullyQualifiedJavaType = FullyQualifiedJavaType(type)
        }
    }
}
