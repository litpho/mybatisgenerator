package nl.litpho.mybatis.generator.plugins.asciidoc

import nl.litpho.mybatis.generator.file.GeneratedFlatFile
import nl.litpho.mybatis.generator.plugins.subpackage.SubpackageConfiguration
import org.mybatis.generator.api.IntrospectedTable

class GeneratedTablesPerDiagramFile(
    fileName: String,
    targetPackage: String,
    targetProject: String,
    private val tablesPerDiagram: Map<IntrospectedTable, List<String>>,
    private val groupNames: List<String>,
    private val subpackageConfiguration: SubpackageConfiguration?
) : GeneratedFlatFile(fileName, targetPackage, targetProject) {

    override fun getFormattedContent(): String {
        val tableList: List<IntrospectedTable> = tablesPerDiagram.keys.sortedBy { it.aliasedFullyQualifiedTableNameAtRuntime }
        val sb = StringBuilder()
        sb.append(",")
        sb.append(groupNames.joinToString(", "))
        if (subpackageConfiguration != null) {
            sb.append(",")
        }
        sb.append("\n")
        for (table in tableList) {
            sb.append(table.aliasedFullyQualifiedTableNameAtRuntime)
            val groupList = tablesPerDiagram[table]
            for (name in groupNames) {
                if (groupList != null && groupList.contains(name)) {
                    sb.append(",X")
                } else {
                    sb.append(",")
                }
            }
            if (subpackageConfiguration != null) {
                sb.append(",").append(subpackageConfiguration.getSubpackage(table.aliasedFullyQualifiedTableNameAtRuntime))
            }
            sb.append("\n")
        }
        return sb.toString()
    }
}
