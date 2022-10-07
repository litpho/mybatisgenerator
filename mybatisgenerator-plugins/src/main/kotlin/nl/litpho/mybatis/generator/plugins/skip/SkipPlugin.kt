package nl.litpho.mybatis.generator.plugins.skip

import nl.litpho.mybatis.generator.plugins.util.ConfigurationUtil
import nl.litpho.mybatis.generator.plugins.util.readConfigurationFromYaml
import org.mybatis.generator.api.GeneratedXmlFile
import org.mybatis.generator.api.IntrospectedTable
import org.mybatis.generator.api.PluginAdapter
import org.mybatis.generator.api.dom.java.Interface
import org.mybatis.generator.api.dom.java.TopLevelClass
import org.mybatis.generator.api.dom.kotlin.KotlinFile
import org.mybatis.generator.api.dom.kotlin.KotlinType

open class SkipPlugin : PluginAdapter() {

    private lateinit var configuration: SkipPluginConfiguration

    override fun validate(warnings: MutableList<String>?): Boolean {
        val configurationLocation: String = properties.getProperty("configuration")
            ?: throw RuntimeException("Invalid configuration location for SkipPlugin")

        this.configuration = readConfigurationFromYaml<SkipYaml>(configurationLocation).toConfiguration()
        ConfigurationUtil.addConfiguration(configuration)

        return true
    }

    override fun modelBaseRecordClassGenerated(
        topLevelClass: TopLevelClass,
        introspectedTable: IntrospectedTable
    ): Boolean =
        super.modelBaseRecordClassGenerated(topLevelClass, introspectedTable) && !configuration.isIgnored(
            introspectedTable
        )

    override fun kotlinDataClassGenerated(
        kotlinFile: KotlinFile,
        dataClass: KotlinType,
        introspectedTable: IntrospectedTable
    ): Boolean =
        super.kotlinDataClassGenerated(kotlinFile, dataClass, introspectedTable) && !configuration.isIgnored(
            introspectedTable
        )

    override fun modelRecordWithBLOBsClassGenerated(
        topLevelClass: TopLevelClass,
        introspectedTable: IntrospectedTable
    ): Boolean =
        super.modelRecordWithBLOBsClassGenerated(topLevelClass, introspectedTable) && !configuration.isIgnored(
            introspectedTable
        )

    override fun modelExampleClassGenerated(
        topLevelClass: TopLevelClass,
        introspectedTable: IntrospectedTable
    ): Boolean =
        super.modelExampleClassGenerated(
            topLevelClass,
            introspectedTable
        ) && !configuration.isIgnored(introspectedTable)

    override fun modelPrimaryKeyClassGenerated(
        topLevelClass: TopLevelClass,
        introspectedTable: IntrospectedTable
    ): Boolean =
        super.modelPrimaryKeyClassGenerated(topLevelClass, introspectedTable) && !configuration.isIgnored(
            introspectedTable
        )

    override fun sqlMapGenerated(sqlMap: GeneratedXmlFile, introspectedTable: IntrospectedTable): Boolean =
        super.sqlMapGenerated(sqlMap, introspectedTable) && !configuration.isIgnored(introspectedTable)

    override fun clientGenerated(interfaze: Interface, introspectedTable: IntrospectedTable): Boolean =
        super.clientGenerated(interfaze, introspectedTable) && !configuration.isIgnored(introspectedTable)

    override fun dynamicSqlSupportGenerated(
        supportClass: TopLevelClass,
        introspectedTable: IntrospectedTable
    ): Boolean =
        super.dynamicSqlSupportGenerated(supportClass, introspectedTable) && !configuration.isIgnored(introspectedTable)

    override fun mapperGenerated(
        mapperFile: KotlinFile,
        mapper: KotlinType,
        introspectedTable: IntrospectedTable
    ): Boolean =
        super.mapperGenerated(mapperFile, mapper, introspectedTable) && !configuration.isIgnored(introspectedTable)

    override fun dynamicSqlSupportGenerated(
        kotlinFile: KotlinFile,
        outerSupportObject: KotlinType,
        innerSupportClass: KotlinType,
        introspectedTable: IntrospectedTable
    ): Boolean =
        super.dynamicSqlSupportGenerated(kotlinFile, outerSupportObject, innerSupportClass, introspectedTable) &&
            !configuration.isIgnored(introspectedTable)
}
