package nl.litpho.mybatis.generator.plugins.naming

import nl.litpho.mybatis.generator.plugins.PluginConfiguration
import nl.litpho.mybatis.generator.plugins.util.createLogger
import org.mybatis.generator.api.IntrospectedColumn
import org.mybatis.generator.api.IntrospectedTable
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType

data class NamingYaml(var prefix: String? = null, var tables: MutableList<TableData> = mutableListOf()) {

    fun toConfiguration(): NamingConfiguration = NamingConfiguration(this)

    data class TableData(
        var name: String? = null,
        var type: String? = null,
        var prefix: String? = null,
        var columns: MutableList<ColumnData> = mutableListOf()
    )

    data class ColumnData(
        var name: String? = null,
        var type: String? = null,
        var property: String? = null,
        var defaultValue: String? = null
    )
}

data class NamingConfiguration(private val namingYaml: NamingYaml) : PluginConfiguration {

    val namingConfigurationEntryMap: Map<String, NamingConfigurationEntry> =
        namingYaml.tables.associate {
            it.name!! to NamingConfigurationEntry(
                it.type,
                it.prefix ?: namingYaml.prefix ?: "",
                it.columns
            )
        }

    fun getTableConfiguration(table: String?): NamingConfigurationEntry? = namingConfigurationEntryMap[table]

    fun getParseResultForType(type: String): NamingConfigurationEntry? =
        namingConfigurationEntryMap.values.firstOrNull { it.type == type }
}

class NamingConfigurationEntry(
    val type: String?,
    val prefix: String,
    columns: MutableList<NamingYaml.ColumnData>
) {
    val columnOverrides: Map<String, ColumnBasedJavaPropertyOverride> =
        columns.associate { it.name!! to ColumnBasedJavaPropertyOverride(it.property, it.type) }
//        val columnDefaultValues: Map<String, String> =
//            columns.filter { it.defaultValue != null }.associate { it.name!! to it.defaultValue!! }
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
