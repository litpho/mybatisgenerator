package nl.litpho.mybatis.generator.plugins.asciidoc

import nl.litpho.mybatis.generator.plugins.asciidoc.AsciidocConfiguration.GroupDefinition
import nl.litpho.mybatis.generator.plugins.domainenum.DomainEnumConfiguration
import nl.litpho.mybatis.generator.plugins.subpackage.SubpackageConfiguration
import org.mybatis.generator.api.IntrospectedTable
import java.util.SortedSet

private const val NO_PACKAGE = "<no-package>"

class PlantUMLDiagram(
    private val name: String,
    private val targetPackage: String,
    private val tablesToDocument: Collection<IntrospectedTable>,
    private val includedEnums: Set<String>,
    private val allTables: Map<String, IntrospectedTable>,
    private val keys: Map<IntrospectedTable, SortedSet<String>>,
    private val domainEnumConfiguration: DomainEnumConfiguration?,
    private val subpackageConfiguration: SubpackageConfiguration?,
    private val group: GroupDefinition
) : AsciidocContents {

    override fun getFormattedContent(): String {
        println("Generating diagram \"" + group.name + "\"")
        val (includedMap, excludedMap) = bepaalIncludesEnExcludes()
        val list: MutableList<String> = mutableListOf()
        list.add(".$name")
        list.add("[plantuml, ${group.filename}, svg]")
        list.add("----")
        list.add("skinparam linetype ortho")
        list.add("package \"${group.name}\" <<Rectangle>> {")
        list.addAll(getIncludedClasses(includedMap))
        list.add("}")
        list.addAll(getExcludedClasses(excludedMap))
        list.addAll(getConnections())
        list.add("----")

        return list.joinToString("\n")
    }

    private fun getIncludedClasses(includedMap: Map<String, MutableSet<IntrospectedTable>>): List<String> {
        val list: MutableList<String> = mutableListOf()
        includedMap.keys
            .sorted()
            .forEach { pakkage: String ->
                val introspectedTables: Set<IntrospectedTable> = includedMap.getValue(pakkage)
                if (NO_PACKAGE != pakkage && (subpackageConfiguration != null) && introspectedTables.isNotEmpty()) {
                    list.add("package $pakkage {")
                    list.addAll(getClassesForTables(introspectedTables))
                    list.add("}")
                } else {
                    list.addAll(getClassesForTables(introspectedTables))
                }
            }

        return list
    }

    private fun getClassesForTables(introspectedTables: Set<IntrospectedTable>): List<String> =
        introspectedTables
            .flatMap { introspectedTable ->
                if (domainEnumConfiguration?.isDomainEnumTable(introspectedTable.aliasedFullyQualifiedTableNameAtRuntime) == true) {
                    getEnumClass(introspectedTable)
                } else {
                    getNonEnumClass(introspectedTable)
                }
            }

    private fun getEnumClass(introspectedTable: IntrospectedTable): List<String> {
        val list: MutableList<String> = mutableListOf()
        list.add("enum ${introspectedTable.fullyQualifiedTableNameAtRuntime} {")
        domainEnumConfiguration?.getEnumConstants(introspectedTable)?.let {
            list.addAll(it.map { enumConstant -> "\t$enumConstant" })
        }
        list.add("}")

        return list
    }

    private fun getNonEnumClass(introspectedTable: IntrospectedTable): List<String> {
        val list: MutableList<String> = mutableListOf()
        list.add("class ${introspectedTable.fullyQualifiedTableNameAtRuntime} << (T,MediumTurquoise) >> {")
        if (introspectedTable.primaryKeyColumns.isNotEmpty() && introspectedTable.nonPrimaryKeyColumns.isNotEmpty()) {
            list.add("\t..")
            list.addAll(getPrimaryKeyColumns(introspectedTable))
            list.add("\t..")
        } else {
            list.addAll(getPrimaryKeyColumns(introspectedTable))
        }
        list.addAll(introspectedTable.nonPrimaryKeyColumns.map { "\t${it.actualColumnName}" })
        list.add("}")

        return list
    }

    private fun getPrimaryKeyColumns(introspectedTable: IntrospectedTable): List<String> =
        introspectedTable.primaryKeyColumns.map { "\t${it.actualColumnName}" }

    private fun getExcludedClasses(excludedMap: Map<String, MutableSet<IntrospectedTable>>): List<String> {
        val list: MutableList<String> = mutableListOf()
        excludedMap.keys
            .sorted()
            .forEach { excludedPackage ->
                list.add("package \"extern: $excludedPackage\" <<Rectangle>> #lightgrey {")
                for (introspectedTable: IntrospectedTable in excludedMap.getValue(excludedPackage)) {
                    list.add("\tclass ${introspectedTable.aliasedFullyQualifiedTableNameAtRuntime}")
                }
                list.add("}")
            }

        return list
    }

    private fun getConnections(): List<String> {
        val list: MutableList<String> = mutableListOf()
        for (introspectedTable: IntrospectedTable in tablesToDocument) {
            if (keys.containsKey(introspectedTable)) {
                for (tableName: String in keys.getValue(introspectedTable)) {
                    val pkIntrospectedTable: IntrospectedTable? = allTables[tableName]
                    if (pkIntrospectedTable != null) {
                        list.add("${pkIntrospectedTable.aliasedFullyQualifiedTableNameAtRuntime} *-- ${introspectedTable.aliasedFullyQualifiedTableNameAtRuntime}")
                    }
                }
            }
        }

        return list
    }

    private fun bepaalRootPackage(): String =
        if (group.rootPackage.isNullOrEmpty()) {
            targetPackage
        } else {
            "$targetPackage.${group.rootPackage}"
        }

    private fun bepaalIncludesEnExcludes(): Pair<Map<String, MutableSet<IntrospectedTable>>, Map<String, MutableSet<IntrospectedTable>>> {
        val fullRootPackage = bepaalRootPackage()
        val includedMap: MutableMap<String, MutableSet<IntrospectedTable>> = HashMap()
        val excludedMap: MutableMap<String, MutableSet<IntrospectedTable>> = HashMap()
        for (introspectedTable: IntrospectedTable in tablesToDocument) {
            val pakkage = getPackage(introspectedTable.aliasedFullyQualifiedTableNameAtRuntime)
            includedMap.computeIfAbsent(pakkage) { HashSet() }.add(introspectedTable)
            val relationsForTable = keys[introspectedTable]
            if (subpackageConfiguration == null || relationsForTable == null) {
                continue
            }

            for (tableName in relationsForTable) {
                val introspectedForeignTable = allTables.getValue(tableName)
                val keyPackage = getPackage(introspectedForeignTable.aliasedFullyQualifiedTableNameAtRuntime)
                val subpackage = getSubpackage(introspectedForeignTable.aliasedFullyQualifiedTableNameAtRuntime)
                if (shouldIncludeForeignTable(tableName, keyPackage, subpackage, fullRootPackage)) {
                    includedMap.computeIfAbsent(keyPackage) { HashSet() }.add(introspectedForeignTable)
                } else {
                    if (includedEnums.contains(tableName)) {
                        includedMap.computeIfAbsent(NO_PACKAGE) { HashSet() }.add(introspectedForeignTable)
                    } else {
                        excludedMap.computeIfAbsent(keyPackage) { HashSet() }.add(introspectedForeignTable)
                    }
                }
            }
        }

        return includedMap.toMap() to excludedMap.toMap()
    }

    private fun shouldIncludeForeignTable(tableName: String, fullPackage: String, keyPackage: String, fullRootPackage: String) =
        group.includeTables.contains(tableName) || group.includePackages.contains(keyPackage) || fullPackage.startsWith(
            fullRootPackage
        )

    private fun getPackage(table: String): String =
        if (subpackageConfiguration == null) {
            group.rootPackage!!
        } else {
            val subpackageFromConfiguration: String = subpackageConfiguration.getSubpackage(table)
            if (subpackageFromConfiguration.isEmpty()) {
                targetPackage
            } else {
                "$targetPackage.$subpackageFromConfiguration"
            }
        }

    private fun getSubpackage(table: String): String =
        subpackageConfiguration?.getSubpackage(table) ?: group.rootPackage!!
}
