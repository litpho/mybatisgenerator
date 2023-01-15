package nl.litpho.mybatis.generator.plugins.domainenum

import nl.litpho.mybatis.generator.plugins.domainenum.DomainEnumGenerator.Companion.createDomeinEnumForTable
import nl.litpho.mybatis.generator.plugins.domainenum.DomainEnumGenerator.Companion.createTypedDomeinEnumTables
import nl.litpho.mybatis.generator.plugins.naming.NamingConfiguration
import nl.litpho.mybatis.generator.plugins.subpackage.SubpackageConfiguration
import nl.litpho.mybatis.generator.plugins.util.ConfigurationUtil
import nl.litpho.mybatis.generator.plugins.util.readConfigurationFromYaml
import org.mybatis.generator.api.GeneratedJavaFile
import org.mybatis.generator.api.GeneratedXmlFile
import org.mybatis.generator.api.IntrospectedTable
import org.mybatis.generator.api.PluginAdapter
import org.mybatis.generator.api.dom.java.Interface
import org.mybatis.generator.api.dom.java.TopLevelClass
import org.mybatis.generator.api.dom.kotlin.KotlinFile
import org.mybatis.generator.api.dom.kotlin.KotlinType

class DomainEnumPlugin : PluginAdapter() {

    private lateinit var configuration: DomainEnumConfiguration

    override fun validate(warnings: MutableList<String>): Boolean {
        val configurationLocation: String = properties.getProperty("configuration")
            ?: throw RuntimeException("Invalid configuration location for NamingPlugin")

        this.configuration = readConfigurationFromYaml<DomainEnumYaml>(configurationLocation).toConfiguration(context)
        ConfigurationUtil.addConfiguration(configuration)

        return true
    }

    override fun initialized(introspectedTable: IntrospectedTable) {
        super.initialized(introspectedTable)

        val namingConfiguration = ConfigurationUtil.getPluginConfigurationRequired<NamingConfiguration>()
        val subpackageConfiguration = ConfigurationUtil.getPluginConfigurationNull<SubpackageConfiguration>()
        val domainEnumUsageDecorator = DomainEnumUsageDecorator(introspectedTable, context)
        domainEnumUsageDecorator.useDomainEnums(configuration, namingConfiguration, subpackageConfiguration)
    }

    override fun contextGenerateAdditionalJavaFiles(introspectedTable: IntrospectedTable): List<GeneratedJavaFile> {
        val earlierFiles: MutableList<GeneratedJavaFile>? = super.contextGenerateAdditionalJavaFiles(introspectedTable)
        val project: String = context.javaModelGeneratorConfiguration.targetProject
        if (!isDomeinEnumTable(introspectedTable)) {
            return mutableListOf()
        }

        val generatedJavaFiles = if (earlierFiles == null) {
            mutableListOf()
        } else {
            mutableListOf<GeneratedJavaFile>().apply { addAll(earlierFiles) }
        }

        if (isTypedDomeinEnumTable(introspectedTable)) {
            generatedJavaFiles.addAll(
                createTypedDomeinEnumTables(
                    project,
                    introspectedTable,
                    configuration.tables.getValue(introspectedTable.fullyQualifiedTableNameAtRuntime),
                    context,
                    configuration,
                ),
            )
        } else if (isPrefixDomeinEnumTable(introspectedTable)) {
            generatedJavaFiles.addAll(createDomeinEnumForTable(project, introspectedTable, context, configuration))
        }

        return generatedJavaFiles
    }

    override fun modelPrimaryKeyClassGenerated(topLevelClass: TopLevelClass, introspectedTable: IntrospectedTable): Boolean =
        !isDomeinEnumTable(introspectedTable)

    override fun modelBaseRecordClassGenerated(topLevelClass: TopLevelClass, introspectedTable: IntrospectedTable): Boolean =
        !isDomeinEnumTable(introspectedTable)

    override fun modelExampleClassGenerated(topLevelClass: TopLevelClass, introspectedTable: IntrospectedTable): Boolean =
        !isDomeinEnumTable(introspectedTable)

    override fun sqlMapGenerated(sqlMap: GeneratedXmlFile, introspectedTable: IntrospectedTable): Boolean = !isDomeinEnumTable(introspectedTable)

    override fun clientGenerated(interfaze: Interface, introspectedTable: IntrospectedTable): Boolean = !isDomeinEnumTable(introspectedTable)

    override fun dynamicSqlSupportGenerated(supportClass: TopLevelClass, introspectedTable: IntrospectedTable): Boolean =
        !isDomeinEnumTable(introspectedTable)

    override fun mapperGenerated(mapperFile: KotlinFile, mapper: KotlinType, introspectedTable: IntrospectedTable): Boolean =
        !isDomeinEnumTable(introspectedTable)

    override fun dynamicSqlSupportGenerated(kotlinFile: KotlinFile, outerSupportObject: KotlinType, innerSupportClass: KotlinType, introspectedTable: IntrospectedTable): Boolean =
        !isDomeinEnumTable(introspectedTable)

    private fun isDomeinEnumTable(introspectedTable: IntrospectedTable): Boolean =
        isTypedDomeinEnumTable(introspectedTable) || isPrefixDomeinEnumTable(introspectedTable)

    private fun isTypedDomeinEnumTable(introspectedTable: IntrospectedTable): Boolean =
        configuration.tables.containsKey(introspectedTable.fullyQualifiedTableNameAtRuntime)

    private fun isPrefixDomeinEnumTable(introspectedTable: IntrospectedTable): Boolean =
        configuration.prefixes.any { introspectedTable.fullyQualifiedTableNameAtRuntime.startsWith(it) }
}
