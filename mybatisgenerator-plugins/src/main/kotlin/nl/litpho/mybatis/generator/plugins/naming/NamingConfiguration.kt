package nl.litpho.mybatis.generator.plugins.naming

import com.fasterxml.jackson.annotation.JsonMerge
import nl.litpho.mybatis.generator.plugins.PluginConfiguration
import nl.litpho.mybatis.generator.plugins.util.createLogger
import org.mybatis.generator.api.IntrospectedColumn
import org.mybatis.generator.api.IntrospectedTable
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType

data class NamingYaml(
    @JsonMerge
    var default: Default? = null,
    @JsonMerge
    var tables: MutableMap<String, Table> = mutableMapOf(),
    @JsonMerge
    var typeAliases: MutableMap<String, String> = mutableMapOf(),
) {

    fun toConfiguration(): NamingConfiguration = NamingConfiguration(this)

    data class Default(
        var prefix: String? = null,
        @JsonMerge
        var columns: MutableMap<String, Column> = mutableMapOf(),
        @JsonMerge
        var ignoredColumns: MutableList<String> = mutableListOf(),
    )

    data class Table(
        var type: String? = null,
        var prefix: String? = null,
        @JsonMerge
        var columns: MutableMap<String, Column> = mutableMapOf(),
        @JsonMerge
        var ignoredColumns: MutableList<String> = mutableListOf(),
    )

    data class Column(
        var type: String? = null,
        var property: String? = null,
        var defaultValue: String? = null,
    )
}

data class NamingConfiguration(private val namingYaml: NamingYaml) : PluginConfiguration {

    val namingConfigurationEntryMap: Map<String, NamingConfigurationEntry> =
        namingYaml.tables.map {
            it.key to NamingConfigurationEntry(
                it.value.type,
                it.value.prefix ?: namingYaml.default?.prefix ?: "",
                it.value.columns,
                it.value.ignoredColumns,
                namingYaml.default?.ignoredColumns,
                namingYaml.default?.columns,
                namingYaml.typeAliases,
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
    ignoredColumns: List<String>,
    defaultIgnoredColumns: List<String>?,
    private val defaultColumns: MutableMap<String, NamingYaml.Column>?,
    private val typeAliases: MutableMap<String, String>,
) {
    val columnOverrides: Map<String, ColumnBasedJavaPropertyOverride> =
        columns.mapValues { ColumnBasedJavaPropertyOverride(it.value.property, it.value.type.applyTypeAlias()) }
            .toMutableMap()
            .let { map ->
                defaultColumns?.forEach { (key, defCol) ->
                    val original: ColumnBasedJavaPropertyOverride? = map[key]
                    val override =
                        ColumnBasedJavaPropertyOverride(
                            original?.property ?: defCol.property,
                            original?.type ?: defCol.type.applyTypeAlias(),
                        )
                    map[key] = override
                }
                map
            }
    val columnDefaultValues: Map<String, String> =
        columns.filter { it.value.defaultValue != null }
            .mapValues { it.value.defaultValue!! }
            .toMutableMap()
            .let { map ->
                defaultColumns?.forEach { (key, defCol) ->
                    val original: String? = map[key]
                    val override = original ?: defCol.defaultValue
                    override?.let { map[key] = it }
                }
                map
            }
    val ignoredColumns: List<String> = ignoredColumns + (defaultIgnoredColumns ?: emptyList())

    private fun String?.applyTypeAlias(): String? =
        if (this == null || !this.startsWith("$")) {
            this
        } else {
            typeAliases[this.drop(1)] ?: this
        }
}

class ColumnBasedJavaPropertyOverride(val property: String?, val type: String?) {

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
