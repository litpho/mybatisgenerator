package nl.litpho.mybatis.generator.plugins.asciidoc

import nl.litpho.mybatis.generator.plugins.domainenum.DomainEnumConfiguration
import nl.litpho.mybatis.generator.plugins.subpackage.SubpackageConfiguration
import org.mybatis.generator.api.IntrospectedColumn
import org.mybatis.generator.api.IntrospectedTable
import kotlin.math.ceil

private const val NO_PACKAGE = "<no-package>"

class PlantUMLDiagram(
    private val groupModel: AsciidocGroupModel,
    private val targetPackage: String,
    private val allTables: Map<String, IntrospectedTable>,
    private val domainEnumConfiguration: DomainEnumConfiguration?,
    private val subpackageConfiguration: SubpackageConfiguration?
) : AsciidocContents {

    override fun getFormattedContent(): String {
        val group = groupModel.group
        println("Generating diagram \"" + group.name + "\"")
        val (includedMap, excludedMap) = determineIncludesAndExcludes()
        val list: MutableList<String> = mutableListOf()
        list.add(".${group.name}")
        list.add("[plantuml, ${group.filename}, svg]")
        list.add("----")
        list.add("skinparam classAttributeFontName Courier")
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
        if (introspectedTable.allColumns.isNotEmpty()) {
            val prefixes = calculatePrefixes(introspectedTable, introspectedTable.allColumns)
            val maxPrefixLength = prefixes.maxOf { it.length }
            val maxColumnLength = introspectedTable.allColumns.maxOf { it.actualColumnName.length }
            introspectedTable.allColumns.forEachIndexed { i, column ->
                list.add(renderColumn(prefixes[i], column, maxPrefixLength, maxColumnLength))
            }
        }
        list.add("}")

        return list
    }

    private fun renderColumn(
        prefix: String,
        introspectedColumn: IntrospectedColumn,
        maxPrefixLength: Int,
        maxColumnLength: Int
    ): String {
        val numPrefixTabs: Int = calculateNumTabs(maxPrefixLength, prefix)
        val numColumnTabs: Int = calculateNumTabs(maxColumnLength, introspectedColumn.actualColumnName)
        val nullableString: String = if (introspectedColumn.isNullable) "" else "* "
        val prefixString = "$prefix${"\\t".repeat(numPrefixTabs)}"
        val columnName = introspectedColumn.actualColumnName
        val columnTabs = "\\t".repeat(numColumnTabs)
        return "\t{field}$nullableString$prefixString$columnName$columnTabs${getJdbcTypeString(introspectedColumn)}"
    }

    private fun calculateNumTabs(maxLength: Int, value: String): Int {
        val newMaxLength = ((maxLength / 8) + 1) * 8
        return ceil((newMaxLength - value.length) / 8.0).toInt()
    }

    private fun calculatePrefixes(introspectedTable: IntrospectedTable, columns: List<IntrospectedColumn>): List<String> =
        columns.map { column -> calculatePrefix(introspectedTable, column) }

    private fun calculatePrefix(introspectedTable: IntrospectedTable, column: IntrospectedColumn): String {
        val list: MutableList<String> = mutableListOf()
        list.addAll(getLabels(introspectedTable, column, "PK"))
        list.addAll(getLabels(introspectedTable, column, "UK"))
        list.addAll(getLabels(introspectedTable, column, "FK"))
//        groupModel.keyInfoMap[introspectedTable]?.filter { it.value.contains(column.actualColumnName) }?.keys?.map { it.label }?.filter { it?.startsWith("PK") ?: false }?.sortedBy { it }?.forEach { list.add(it!!) }
//        groupModel.keyInfoMap[introspectedTable]?.filter { it.value.contains(column.actualColumnName) }?.keys?.map { it.label }?.filter { it?.startsWith("UK") ?: false }?.sortedBy { it }?.forEach { list.add(it!!) }
//        groupModel.keyInfoMap[introspectedTable]?.filter { it.value.contains(column.actualColumnName) }?.keys?.map { it.label }?.filter { it?.startsWith("FK") ?: false }?.sortedBy { it }?.forEach { list.add(it!!) }

        return list.joinToString(",")
    }

    private fun getLabels(introspectedTable: IntrospectedTable, introspectedColumn: IntrospectedColumn, prefix: String): List<String> {
        return groupModel.keyInfoMap[introspectedTable]
            ?.filter { it.value.columns.contains(introspectedColumn.actualColumnName) }
            ?.keys
            ?.map { it.label }
            ?.filter { it?.startsWith(prefix) ?: false }
            ?.map { it!! }
            ?.sortedBy { it } ?: emptyList()
    }

    private fun getExcludedClasses(excludedMap: Map<String, MutableSet<IntrospectedTable>>): List<String> {
        val list: MutableList<String> = mutableListOf()
        excludedMap.keys
            .sorted()
            .forEach { excludedPackage ->
                list.add("package \"external: $excludedPackage\" <<Rectangle>> #lightgrey {")
                for (introspectedTable: IntrospectedTable in excludedMap.getValue(excludedPackage)) {
                    list.add("\tclass ${introspectedTable.aliasedFullyQualifiedTableNameAtRuntime}")
                }
                list.add("}")
            }

        return list
    }

    private fun getConnections(): List<String> {
        val list: MutableList<String> = mutableListOf()
        for (introspectedTable: IntrospectedTable in groupModel.tablesToDocument) {
            for (tableName: String in groupModel.importedKeys.getOrDefault(introspectedTable, emptySet())) {
                val pkIntrospectedTable: IntrospectedTable? = allTables[tableName]
                if (pkIntrospectedTable != null) {
                    list.add("${pkIntrospectedTable.aliasedFullyQualifiedTableNameAtRuntime} *-- ${introspectedTable.aliasedFullyQualifiedTableNameAtRuntime}")
                }
            }
        }

        return list
    }

    private fun bepaalRootPackage(): String =
        if (groupModel.group.rootPackage.isNullOrEmpty()) {
            targetPackage
        } else {
            "$targetPackage.${groupModel.group.rootPackage}"
        }

    private fun determineIncludesAndExcludes(): Pair<Map<String, MutableSet<IntrospectedTable>>, Map<String, MutableSet<IntrospectedTable>>> {
        val fullRootPackage = bepaalRootPackage()
        val includedMap: MutableMap<String, MutableSet<IntrospectedTable>> = HashMap()
        val excludedMap: MutableMap<String, MutableSet<IntrospectedTable>> = HashMap()
        for (introspectedTable: IntrospectedTable in groupModel.tablesToDocument) {
            val pakkage = getPackage(introspectedTable.aliasedFullyQualifiedTableNameAtRuntime)
            includedMap.computeIfAbsent(pakkage) { HashSet() }.add(introspectedTable)
            val relationsForTable = groupModel.importedKeys[introspectedTable]
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
                    if (groupModel.includedEnums.contains(tableName)) {
                        includedMap.computeIfAbsent(NO_PACKAGE) { HashSet() }.add(introspectedForeignTable)
                    } else {
                        excludedMap.computeIfAbsent(keyPackage) { HashSet() }.add(introspectedForeignTable)
                    }
                }
            }
        }

        return includedMap.toMap() to excludedMap.toMap()
    }

    private fun shouldIncludeForeignTable(
        tableName: String,
        fullPackage: String,
        keyPackage: String,
        fullRootPackage: String
    ) =
        groupModel.group.includeTables.contains(tableName) ||
            groupModel.group.includePackages.contains(keyPackage) ||
            fullPackage.startsWith(fullRootPackage)

    private fun getPackage(table: String): String =
        if (subpackageConfiguration == null) {
            groupModel.group.rootPackage!!
        } else {
            val subpackageFromConfiguration: String = subpackageConfiguration.getSubpackage(table)
            if (subpackageFromConfiguration.isEmpty()) {
                targetPackage
            } else {
                "$targetPackage.$subpackageFromConfiguration"
            }
        }

    private fun getSubpackage(table: String): String =
        subpackageConfiguration?.getSubpackage(table) ?: groupModel.group.rootPackage!!
}
