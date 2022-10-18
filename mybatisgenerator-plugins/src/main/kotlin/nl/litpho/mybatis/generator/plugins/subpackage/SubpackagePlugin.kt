package nl.litpho.mybatis.generator.plugins.subpackage

import nl.litpho.mybatis.generator.plugins.util.ConfigurationUtil
import nl.litpho.mybatis.generator.plugins.util.readConfigurationFromYaml
import org.mybatis.generator.api.IntrospectedTable
import org.mybatis.generator.api.PluginAdapter
import java.util.Properties

class SubpackagePlugin : PluginAdapter() {

    private lateinit var configuration: SubpackageConfiguration

    override fun validate(warnings: MutableList<String>?): Boolean {
        val configurationLocation: String = properties.getProperty("configuration")
            ?: throw RuntimeException("Invalid configuration location for SubpackagePlugin")

        this.configuration = readConfigurationFromYaml<SubpackageYaml>(configurationLocation).toConfiguration()
        ConfigurationUtil.addConfiguration(configuration)

        return true
    }

    override fun initialized(introspectedTable: IntrospectedTable) {
        val suffix: String = configuration.getSubpackage(introspectedTable.aliasedFullyQualifiedTableNameAtRuntime)
        if (suffix.isNotEmpty()) {
            with(introspectedTable) {
                val baseRecordType = insertSuffix(suffix, baseRecordType)
                primaryKeyType = insertSuffix(suffix, primaryKeyType)
                this.baseRecordType = baseRecordType
                kotlinRecordType = baseRecordType
                exampleType = insertSuffix(suffix, exampleType)
                myBatisDynamicSqlSupportType = insertSuffix(suffix, myBatisDynamicSqlSupportType)
            }
            with(introspectedTable) {
                myBatis3JavaMapperType = insertSuffix(suffix, myBatis3JavaMapperType)
                myBatis3XmlMapperPackage = appendSuffix(suffix, myBatis3XmlMapperPackage)
            }
        }

        super.initialized(introspectedTable)
    }

    private fun areSubpackagesEnabled(properties: Properties?): Boolean =
        properties?.getProperty("enableSubPackages").toBoolean()

    companion object {

        fun insertSuffix(suffix: String, fullyQualifiedType: String): String {
            if (suffix.isEmpty()) {
                return fullyQualifiedType
            }

            val lastDot = fullyQualifiedType.lastIndexOf('.')
            return appendSuffix(
                suffix,
                fullyQualifiedType.substring(0, lastDot)
            ) + fullyQualifiedType.substring(lastDot)
        }

        fun appendSuffix(suffix: String, pakkage: String): String = "$pakkage.$suffix"
    }
}
