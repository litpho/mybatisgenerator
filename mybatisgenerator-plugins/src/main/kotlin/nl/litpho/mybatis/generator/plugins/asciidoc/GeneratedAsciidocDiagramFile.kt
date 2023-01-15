package nl.litpho.mybatis.generator.plugins.asciidoc

import nl.litpho.mybatis.generator.file.GeneratedFlatFile
import nl.litpho.mybatis.generator.plugins.asciidoc.AsciidocConfiguration.GroupDefinition

class GeneratedAsciidocDiagramFile(
    group: GroupDefinition,
    targetProject: String,
    private val diagram: PlantUMLDiagram,
) : GeneratedFlatFile("${group.filename}-diagram.adoc", "", targetProject) {

    override fun getFormattedContent(): String = diagram.getFormattedContent()
}
