package nl.litpho.mybatis.generator.plugins.asciidoc

import nl.litpho.mybatis.generator.plugins.asciidoc.AsciidocConfiguration.GroupDefinition
import nl.litpho.mybatis.generator.plugins.domainenum.DomainEnumConfiguration
import nl.litpho.mybatis.generator.plugins.skip.SkipConfiguration
import nl.litpho.mybatis.generator.plugins.subpackage.SubpackageConfiguration
import nl.litpho.mybatis.generator.plugins.util.ConfigurationUtil
import nl.litpho.mybatis.generator.plugins.util.ConfigurationUtil.Companion.getPluginConfigurationNull
import nl.litpho.mybatis.generator.plugins.util.createLogger
import nl.litpho.mybatis.generator.plugins.util.readConfigurationFromYaml
import org.mybatis.generator.api.GeneratedJavaFile
import org.mybatis.generator.api.GeneratedXmlFile
import org.mybatis.generator.api.IntrospectedTable
import org.mybatis.generator.api.PluginAdapter
import org.mybatis.generator.logging.Log
import java.io.File

private const val OUTPUT_DIR_PARAMETER: String = "outputDir"

class AsciidocPlugin : PluginAdapter() {

    private val logger: Log = createLogger<AsciidocPlugin>()

    private val introspectedTables: MutableList<IntrospectedTable> = mutableListOf()

    private lateinit var configuration: AsciidocConfiguration

    private lateinit var outputDir: String

    override fun validate(warnings: List<String>): Boolean {
        val configurationLocation: String = properties.getProperty("configuration")
            ?: throw RuntimeException("Invalid configuration location for AsciidocPlugin")

        this.configuration = readConfigurationFromYaml<AsciidocYaml>(configurationLocation).toConfiguration()
        ConfigurationUtil.addConfiguration(configuration)

        this.outputDir = properties.getProperty(OUTPUT_DIR_PARAMETER) ?: throw RuntimeException("outputDir should not be null")

        return true
    }

    override fun contextGenerateAdditionalJavaFiles(introspectedTable: IntrospectedTable): List<GeneratedJavaFile> {
        introspectedTables.add(introspectedTable)
        return super.contextGenerateAdditionalJavaFiles(introspectedTable)
    }

    override fun contextGenerateAdditionalXmlFiles(): List<GeneratedXmlFile> {
        val earlierFiles: MutableList<GeneratedXmlFile>? = super.contextGenerateAdditionalXmlFiles()
        val generatedXmlFiles = if (earlierFiles == null) {
            mutableListOf()
        } else {
            mutableListOf<GeneratedXmlFile>().apply { addAll(earlierFiles) }
        }
        logger.warn("Generating Asciidoc file")
        val allTables: Map<String, IntrospectedTable> = introspectedTables.associateBy { it.fullyQualifiedTableNameAtRuntime }.toMap()
        val domeinEnumPluginConfiguration = getPluginConfigurationNull<DomainEnumConfiguration>()
        val subPackagePluginConfiguration = getPluginConfigurationNull<SubpackageConfiguration>()
        val skipTablePluginConfiguration = getPluginConfigurationNull<SkipConfiguration>()
        val tablesPerDiagram: MutableMap<IntrospectedTable, MutableList<String>> = mutableMapOf()
        val groupModels: MutableList<AsciidocGroupModel> = mutableListOf()

        // Groups
        for (group: GroupDefinition in configuration.groups) {
            val groupModel = AsciidocGroupModel(
                group,
                context,
                allTables,
                domeinEnumPluginConfiguration,
                skipTablePluginConfiguration,
                subPackagePluginConfiguration
            )
            groupModel.calculateTablesToDocument()
            groupModels.add(groupModel)
            generatedXmlFiles.addAll(
                generateFiles(groupModel, allTables, tablesPerDiagram, subPackagePluginConfiguration, skipTablePluginConfiguration)
            )
        }

        // Rest group
        val restGroup: GroupDefinition? = configuration.restGroup
        if (restGroup != null) {
            val groupModel = AsciidocGroupModel(
                restGroup,
                context,
                allTables,
                domeinEnumPluginConfiguration,
                skipTablePluginConfiguration,
                subPackagePluginConfiguration
            )
            groupModel.calculateTablesLeftToDocument(groupModels)
            groupModels.add(groupModel)
            generatedXmlFiles.addAll(
                generateFiles(groupModel, allTables, tablesPerDiagram, subPackagePluginConfiguration, skipTablePluginConfiguration)
            )
        }

        // Generate usage tables
        if (configuration.generateTablesTxt) {
            val groupFileNames = groupModels.map { it.group.filename }
            val generatedTablesPerDiagramFile = GeneratedTablesPerDiagramFile(
                "tables-per-diagram.csv",
                "",
                outputDir,
                tablesPerDiagram,
                groupFileNames,
                subPackagePluginConfiguration
            )
            generatedXmlFiles.add(generatedTablesPerDiagramFile)
        }

        return generatedXmlFiles
    }

    private fun generateFiles(
        groupModel: AsciidocGroupModel,
        allTables: Map<String, IntrospectedTable>,
        tablesPerDiagram: MutableMap<IntrospectedTable, MutableList<String>>,
        subPackagePluginConfiguration: SubpackageConfiguration?,
        skipTablePluginConfiguration: SkipConfiguration?
    ): List<GeneratedXmlFile> {
        val generatedXmlFiles: MutableList<GeneratedXmlFile> = mutableListOf()
        generatedXmlFiles.add(generateDiagram(groupModel, allTables, tablesPerDiagram))
        generatedXmlFiles.add(generateTableDescription(groupModel))
        if (configuration.generateTablesTxt) {
            val generatedTableUsageFile = GeneratedTableUsageFile(
                "${groupModel.group.filename}-tables.txt",
                "",
                outputDir,
                groupModel,
                allTables,
                subPackagePluginConfiguration,
                skipTablePluginConfiguration
            )
            generatedXmlFiles.add(generatedTableUsageFile)
        }

        return generatedXmlFiles
    }

    private fun generateDiagram(
        groupModel: AsciidocGroupModel,
        allTables: Map<String, IntrospectedTable>,
        tablesPerDiagram: MutableMap<IntrospectedTable, MutableList<String>>
    ): GeneratedAsciidocDiagramFile {
        val domeinEnumPluginConfiguration: DomainEnumConfiguration? = getPluginConfigurationNull()
        val subPackagePluginConfiguration: SubpackageConfiguration? = getPluginConfigurationNull()
        for (table: IntrospectedTable in groupModel.tablesToDocument) {
            tablesPerDiagram.computeIfAbsent(table) { mutableListOf() }.add(groupModel.group.filename)
        }
        File(outputDir).mkdirs()
        val diagram = PlantUMLDiagram(
            groupModel.group.name,
            context.javaModelGeneratorConfiguration.targetPackage,
            groupModel.tablesToDocument,
            groupModel.includedEnums,
            allTables,
            groupModel.importedKeys,
            domeinEnumPluginConfiguration,
            subPackagePluginConfiguration,
            groupModel.group
        )
        return GeneratedAsciidocDiagramFile(groupModel.group, outputDir, diagram)
    }

    private fun generateTableDescription(groupModel: AsciidocGroupModel): GeneratedAsciidocTableDescriptionFile =
        GeneratedAsciidocTableDescriptionFile(groupModel.group, outputDir, groupModel)
}
