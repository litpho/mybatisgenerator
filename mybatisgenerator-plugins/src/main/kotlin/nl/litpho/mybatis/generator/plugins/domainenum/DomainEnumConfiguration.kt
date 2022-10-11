package nl.litpho.mybatis.generator.plugins.domainenum

import nl.litpho.mybatis.generator.plugins.PluginConfiguration
import org.mybatis.generator.api.IntrospectedTable
import org.mybatis.generator.config.Context
import java.util.Optional

data class DomainEnumYaml(
    var packageSuffix: String? = null,
    var targetPackage: String? = null,
    var prefixes: MutableList<String> = mutableListOf(),
    var tables: MutableList<TableData> = mutableListOf()
) {

    fun toConfiguration(context: Context): DomainEnumConfiguration = DomainEnumConfiguration(this, context)

    data class TableData(
        var name: String? = null,
        var valueColumn: String? = null,
        var descriptionColumn: String? = null,
        var orderColumn: String? = null,
        var generateEnumValue: Boolean = false,
        var excludeColumns: MutableList<String> = mutableListOf(),
        var excludeTypes: MutableList<String> = mutableListOf()
    )
}

class DomainEnumConfiguration(configuration: DomainEnumYaml, context: Context) : PluginConfiguration {

    val targetPackage: String = configuration.targetPackage ?: context.javaModelGeneratorConfiguration.targetPackage

    val prefixes: MutableList<String> = configuration.prefixes

    val tables: Map<String, DomainEnumTableDefinition> = configuration.tables.associate { it.name!! to DomainEnumTableDefinition(it) }

    private val domainEnumDatabaseValuePairs = DomainEnumDatabaseValuePairs()

    private val enumConstantsByTable: MutableMap<IntrospectedTable, List<String>> = mutableMapOf()

    fun addDomainEnumDatabaseValues(domainEnumClassName: String, databaseValue: String, value: String) {
        domainEnumDatabaseValuePairs.put(domainEnumClassName, databaseValue, value)
    }

    fun getDomainEnumDatabaseValuePairs(domainEnumClassName: String, databaseValue: String): String =
        domainEnumDatabaseValuePairs.get(domainEnumClassName, databaseValue).orElse(databaseValue)

    fun isDomainEnumTable(tableName: String): Boolean =
        if (tables.containsKey(tableName)) {
            true
        } else {
            prefixes.any { tableName.startsWith(it) }
        }

    fun addEnumConstants(introspectedTable: IntrospectedTable, enumConstants: List<String>) {
        enumConstantsByTable[introspectedTable] = enumConstants
    }

    fun getEnumConstants(introspectedTable: IntrospectedTable): List<String>? = enumConstantsByTable[introspectedTable]

    class DomainEnumTableDefinition(data: DomainEnumYaml.TableData) {
        val name: String = requireNotNull(data.name)
        val valueColumn: String = requireNotNull(data.valueColumn)
        val descriptionColumn: String? = data.descriptionColumn
        val orderColumn: String? = data.orderColumn
        val generateEnumValue: Boolean = data.generateEnumValue
        val excludeColumns: MutableList<String> = data.excludeColumns

        init {
            if (generateEnumValue) {
                excludeColumns.add(valueColumn)
            }
        }
    }

    class DomainEnumDatabaseValuePairs {

        private val valueDatabaseValueMap: MutableMap<String, String> = mutableMapOf()

        fun put(enumType: String, databaseValue: String, value: String) {
            this.valueDatabaseValueMap["$enumType.$databaseValue"] = value
        }

        fun get(enumType: String, databaseValue: String): Optional<String> =
            Optional.ofNullable<String>(getNull(enumType, databaseValue))

        private fun getNull(enumType: String, databaseValue: String): String? = valueDatabaseValueMap["$enumType.$databaseValue"]
    }
}
