package nl.litpho.mybatis.generator.plugins.naming

import nl.litpho.mybatis.generator.plugins.asciidoc.AsciidocConfiguration
import nl.litpho.mybatis.generator.plugins.util.ConfigurationUtil
import nl.litpho.mybatis.generator.plugins.util.readConfigurationFromYaml
import org.mybatis.generator.api.IntrospectedTable
import org.mybatis.generator.api.PluginAdapter
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType
import org.mybatis.generator.config.SqlMapGeneratorConfiguration

class NamingPlugin : PluginAdapter() {

    private lateinit var configuration: NamingConfiguration

    override fun validate(warnings: MutableList<String>): Boolean {
        val configurationLocation: String = properties.getProperty("configuration")
            ?: throw RuntimeException("Invalid configuration location for NamingPlugin")

        this.configuration = readConfigurationFromYaml<NamingYaml>(configurationLocation).toConfiguration()
        ConfigurationUtil.addConfiguration(configuration)

        return true
    }

    override fun initialized(introspectedTable: IntrospectedTable) {
        val asciidocConfiguration = ConfigurationUtil.getPluginConfigurationNull<AsciidocConfiguration>()
        if (asciidocConfiguration != null) {
            asciidocConfiguration.allColumns[introspectedTable.fullyQualifiedTableNameAtRuntime] = introspectedTable.allColumns
            asciidocConfiguration.nonPrimaryKeyColumns[introspectedTable.fullyQualifiedTableNameAtRuntime] = introspectedTable.nonPrimaryKeyColumns
        }

        val namingConfigurationEntry =
            configuration.namingConfigurationEntryMap[introspectedTable.aliasedFullyQualifiedTableNameAtRuntime]
        if (namingConfigurationEntry != null) {
            initializeIntrospectedTable(introspectedTable, namingConfigurationEntry)

            introspectedTable.allColumns.forEach { introspectedColumn ->
                namingConfigurationEntry.columnOverrides[introspectedColumn.actualColumnName]
                    ?.updateWith(introspectedTable, introspectedColumn)
            }
        }

        super.initialized(introspectedTable)
    }

    private fun initializeIntrospectedTable(
        introspectedTable: IntrospectedTable,
        namingConfigurationEntry: NamingConfigurationEntry
    ) {
        val typeName = calculateTypeName(introspectedTable, namingConfigurationEntry)

        with(introspectedTable) {
            baseRecordType = "${context.javaModelGeneratorConfiguration.targetPackage}.$typeName"
            recordWithBLOBsType = "${context.javaModelGeneratorConfiguration.targetPackage}.${typeName}WithBLOBs"
            primaryKeyType = "${context.javaModelGeneratorConfiguration.targetPackage}.${typeName}Key"
            exampleType = "${context.javaModelGeneratorConfiguration.targetPackage}.${typeName}Example"
            kotlinRecordType = "${context.javaModelGeneratorConfiguration.targetPackage}.$typeName"
            myBatis3JavaMapperType = "${context.javaClientGeneratorConfiguration.targetPackage}.${typeName}Mapper"
            myBatisDynamicSqlSupportType =
                "${context.javaClientGeneratorConfiguration.targetPackage}.${typeName}DynamicSqlSupport"
            myBatisDynamicSQLTableObjectName = typeName
            myBatis3SqlProviderType =
                "${context.javaClientGeneratorConfiguration.targetPackage}.${typeName}SqlProvider"

            val sqlMapGeneratorConfiguration: SqlMapGeneratorConfiguration? = context.sqlMapGeneratorConfiguration
            if (sqlMapGeneratorConfiguration != null) {
                myBatis3XmlMapperFileName = "${typeName}Mapper.xml"
            }

            primaryKeyColumns.removeIf { it.actualColumnName in namingConfigurationEntry.ignoredColumns }
            baseColumns.removeIf { it.actualColumnName in namingConfigurationEntry.ignoredColumns }
            blobColumns.removeIf { it.actualColumnName in namingConfigurationEntry.ignoredColumns }
        }
    }

    private fun calculateTypeName(
        introspectedTable: IntrospectedTable,
        namingConfigurationEntry: NamingConfigurationEntry
    ): String =
        if (namingConfigurationEntry.type == null) {
            val fqjt = FullyQualifiedJavaType(introspectedTable.baseRecordType)
            namingConfigurationEntry.prefix + fqjt.shortName
        } else {
            namingConfigurationEntry.prefix + namingConfigurationEntry.type
        }
}
