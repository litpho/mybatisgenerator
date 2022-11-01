package nl.litpho.mybatis.generator.plugins.domainenum

import nl.litpho.mybatis.generator.plugins.naming.NamingConfiguration
import nl.litpho.mybatis.generator.plugins.naming.NamingConfigurationEntry
import nl.litpho.mybatis.generator.plugins.subpackage.SubpackageConfiguration
import org.mybatis.generator.api.IntrospectedColumn
import org.mybatis.generator.api.IntrospectedTable
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType
import org.mybatis.generator.config.Context
import org.mybatis.generator.internal.util.JavaBeansUtil
import java.sql.ResultSet
import java.sql.SQLException
import java.util.Optional

class DomainEnumUsageDecorator(private val introspectedTable: IntrospectedTable, private val context: Context) {

    fun useDomainEnums(
        domainEnumConfiguration: DomainEnumConfiguration,
        namingConfiguration: NamingConfiguration,
        subpackageConfiguration: SubpackageConfiguration?
    ) {
        try {
            context.connection.use { conn ->
                val exportedKeys: ResultSet =
                    conn.metaData.getImportedKeys(conn.catalog, conn.schema, introspectedTable.fullyQualifiedTableNameAtRuntime)
                while (exportedKeys.next()) {
                    val pktableName: String = exportedKeys.getString("PKTABLE_NAME")
                    if (domainEnumConfiguration.isDomainEnumTable(pktableName)) {
                        val column: Optional<IntrospectedColumn> = introspectedTable.getColumn(exportedKeys.getString("FKCOLUMN_NAME"))
                        column.ifPresent { c: IntrospectedColumn ->
                            c.fullyQualifiedJavaType = FullyQualifiedJavaType(
                                getEnumType(
                                    pktableName,
                                    domainEnumConfiguration,
                                    namingConfiguration,
                                    subpackageConfiguration
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: SQLException) {
            throw RuntimeException(e)
        }
    }

    private fun getEnumType(
        enumTableName: String,
        domainEnumPluginConfiguration: DomainEnumConfiguration,
        namingConfiguration: NamingConfiguration?,
        subpackageConfiguration: SubpackageConfiguration?
    ): String {
        val pakkage: String = determinePackage(enumTableName, domainEnumPluginConfiguration, subpackageConfiguration)
        if (namingConfiguration != null) {
            val parseResultForTable: NamingConfigurationEntry? = namingConfiguration.getTableConfiguration(enumTableName)
            if (parseResultForTable?.type != null) {
                return "$pakkage.${parseResultForTable.prefix}${parseResultForTable.type}"
            }
        }

        if (domainEnumPluginConfiguration.tables.containsKey(enumTableName)) {
            return "$pakkage.${JavaBeansUtil.getCamelCaseString(enumTableName, true)}"
        } else {
            for (prefix in domainEnumPluginConfiguration.prefixes) {
                if (enumTableName.startsWith(prefix)) {
                    return "$pakkage.${JavaBeansUtil.getCamelCaseString(enumTableName.substring(prefix.length), true)}"
                }
            }

            throw RuntimeException("Enum $enumTableName is not matched by name or prefixes")
        }
    }

    private fun determinePackage(
        enumTableName: String,
        domainEnumConfiguration: DomainEnumConfiguration,
        subpackageConfiguration: SubpackageConfiguration?
    ): String =
        domainEnumConfiguration.targetPackage.run {
            val subPackage = subpackageConfiguration?.getSubpackage(enumTableName)
            if (subPackage?.isNotEmpty() == true) {
                "$this.$subPackage"
            } else {
                this
            }
        }
}
