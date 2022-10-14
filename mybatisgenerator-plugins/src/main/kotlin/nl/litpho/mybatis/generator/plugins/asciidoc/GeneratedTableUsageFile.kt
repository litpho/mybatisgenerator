package nl.litpho.mybatis.generator.plugins.asciidoc

import nl.litpho.mybatis.generator.file.GeneratedFlatFile
import nl.litpho.mybatis.generator.plugins.skip.SkipConfiguration
import nl.litpho.mybatis.generator.plugins.subpackage.SubpackageConfiguration
import org.mybatis.generator.api.IntrospectedTable
import java.util.SortedSet

class GeneratedTableUsageFile(
    fileName: String,
    targetPackage: String,
    targetProject: String,
    private val groupModel: AsciidocGroupModel,
    private val allTables: Map<String, IntrospectedTable>,
    private val subPackagePluginConfiguration: SubpackageConfiguration?,
    private val skipTablePluginConfiguration: SkipConfiguration?
) : GeneratedFlatFile(fileName, targetPackage, targetProject) {

    override fun getFormattedContent(): String =
        listOf(getIncludeWarnings(), getExcludeWarnings(), getTablesToDocumentIncludingEnums())
            .flatten()
            .joinToString("\n")

    private fun getIncludeWarnings(): List<String> {
        if (subPackagePluginConfiguration == null || skipTablePluginConfiguration == null) {
            return emptyList()
        }

        val includeRecursive = groupModel.group.includeRecursive
        val includeTables = groupModel.group.includeTables
        val includePackages = groupModel.group.includePackages
        val rootPackage = groupModel.group.rootPackage

        val list: MutableList<String> = mutableListOf()
        for (includedTable in includeTables) {
            if (!skipTablePluginConfiguration.isIgnored(allTables.getValue(includedTable))) {
                val implicitSubpackage = subPackagePluginConfiguration.getSubpackage(includedTable)
                if (includePackages.contains(implicitSubpackage)) {
                    list.add("$includedTable wordt al impliciet geinclude als package $implicitSubpackage")
                }
                if (rootPackage != null && includeRecursive && implicitSubpackage.startsWith("$rootPackage.")) {
                    list.add("$includedTable wordt al impliciet recursief geinclude als package $implicitSubpackage")
                }
            }
        }

        return list.toList()
    }

    private fun getExcludeWarnings(): List<String> {
        val excludeTables = groupModel.group.excludeTables
        val includeTables = groupModel.group.includeTables
        val includePackages = groupModel.group.includePackages

        val list: MutableList<String> = mutableListOf()
        for (excludedTable in excludeTables) {
            if (includeTables.contains(excludedTable)) {
                list.add("$excludedTable wordt zowel expliciet geinclude als geexclude")
            } else {
                if (subPackagePluginConfiguration != null) {
                    val implicitSubpackage = subPackagePluginConfiguration.getSubpackage(excludedTable)
                    if (!includePackages.contains(implicitSubpackage)) {
                        list.add("$excludedTable wordt overbodig geexclude")
                    }
                }
            }
        }

        return list.toList()
    }

    private fun getTablesToDocumentIncludingEnums(): List<String> {
        val tables: SortedSet<String> = groupModel.tablesToDocument
            .map { it.fullyQualifiedTableNameAtRuntime }
            .toSortedSet()
        tables.addAll(groupModel.includedEnums)

        return tables.toList()
    }
}
