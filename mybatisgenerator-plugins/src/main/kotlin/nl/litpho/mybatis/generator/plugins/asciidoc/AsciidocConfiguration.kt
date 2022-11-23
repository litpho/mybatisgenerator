package nl.litpho.mybatis.generator.plugins.asciidoc

import nl.litpho.mybatis.generator.plugins.PluginConfiguration
import org.mybatis.generator.api.IntrospectedColumn

data class AsciidocYaml(
    var style: Style = Style(),
    var groups: MutableList<GroupData> = mutableListOf(),
    var restGroup: GroupData? = null
) {

    fun toConfiguration(): AsciidocConfiguration = AsciidocConfiguration(this)

    data class Style(
        var theme: String? = null,
        var externalBackgroundColor: String = "LightGrey",
        var enumDotColor: String = "DarkSalmon",
        var tableDotColor: String = "MediumTurquoise",
        var ortho: Boolean = true
    )

    data class GroupData(
        var name: String? = null,
        var filename: String? = null,
        var rootPackage: String? = null,
        var includeRecursive: Boolean = false,
        var includeTables: MutableList<String> = mutableListOf(),
        var includePackages: MutableList<String> = mutableListOf(),
        var excludeTables: MutableList<String> = mutableListOf()
    )
}

class AsciidocConfiguration(configuration: AsciidocYaml) : PluginConfiguration {

    val style: AsciidocYaml.Style? = configuration.style

    val groups: List<GroupDefinition> = configuration.groups
        .map { groupData ->
            GroupDefinition(
                requireNotNull(groupData.name),
                requireNotNull(groupData.filename),
                groupData.rootPackage,
                groupData.includeRecursive,
                groupData.includeTables,
                groupData.includePackages,
                groupData.excludeTables
            )
        }

    val restGroup: GroupDefinition? = configuration.restGroup?.let { groupData ->
        GroupDefinition(
            requireNotNull(groupData.name),
            requireNotNull(groupData.filename),
            groupData.rootPackage,
            groupData.includeRecursive,
            groupData.includeTables,
            groupData.includePackages,
            groupData.excludeTables
        )
    }

    val allColumns: MutableMap<String, List<IntrospectedColumn>> = mutableMapOf()
    val nonPrimaryKeyColumns: MutableMap<String, List<IntrospectedColumn>> = mutableMapOf()

    data class GroupDefinition(
        val name: String,
        val filename: String,
        val rootPackage: String?,
        val includeRecursive: Boolean,
        val includeTables: MutableList<String>,
        val includePackages: MutableList<String>,
        val excludeTables: MutableList<String>
    )
}
