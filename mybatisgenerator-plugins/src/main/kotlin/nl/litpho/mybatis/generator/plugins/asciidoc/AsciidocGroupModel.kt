package nl.litpho.mybatis.generator.plugins.asciidoc

import nl.litpho.mybatis.generator.db.MetadataProvider
import nl.litpho.mybatis.generator.plugins.asciidoc.AsciidocConfiguration.GroupDefinition
import nl.litpho.mybatis.generator.plugins.domainenum.DomainEnumConfiguration
import nl.litpho.mybatis.generator.plugins.skip.SkipConfiguration
import nl.litpho.mybatis.generator.plugins.subpackage.SubpackageConfiguration
import org.mybatis.generator.api.IntrospectedTable
import org.mybatis.generator.config.Context
import java.sql.Connection
import java.sql.SQLException
import java.util.SortedSet
import java.util.TreeSet

data class AsciidocGroupModel(
    val group: GroupDefinition,
    val context: Context,
    val allTables: Map<String, IntrospectedTable>,
    val domainEnumConfiguration: DomainEnumConfiguration?,
    val skipConfiguration: SkipConfiguration?,
    val subpackageConfiguration: SubpackageConfiguration?,
) {

    val tablesToDocument: SortedSet<IntrospectedTable> =
        TreeSet(Comparator.comparing { it.aliasedFullyQualifiedTableNameAtRuntime })

    val importedKeys: MutableMap<IntrospectedTable, SortedSet<String>> = mutableMapOf()

    val includedEnums: SortedSet<String> = TreeSet()

    val keyInfoMap: MutableMap<IntrospectedTable, MutableMap<KeyInfo, ColumnData>> = mutableMapOf()

    private val exportedKeys: MutableMap<IntrospectedTable, SortedSet<String>> = mutableMapOf()

    private val enumsUsedFromPackage: MutableMap<String, MutableSet<String>> = HashMap()

    fun calculateTablesToDocument() {
        if (!group.rootPackage.isNullOrEmpty()) {
            group.includePackages.add(group.rootPackage)
        }
        tablesToDocument.addAll(filterAllTables())

        if (skipConfiguration != null) {
            tablesToDocument.removeIf { skipConfiguration.isIgnored(it) }
        }

        calculateKeys()
    }

    fun calculateTablesLeftToDocument(groupModels: Collection<AsciidocGroupModel>) {
        tablesToDocument.addAll(allTables.values)
        groupModels.forEach { tablesToDocument.removeAll(it.tablesToDocument) }
        groupModels.forEach { groupModel -> groupModel.includedEnums.forEach { tablesToDocument.remove(allTables[it]) } }
        if (skipConfiguration != null) {
            tablesToDocument.removeIf { skipConfiguration.isIgnored(it) }
        }
        group.includeTables.forEach { tablesToDocument.add(allTables[it]) }
        calculateKeys()
    }

    private fun filterAllTables(): Collection<IntrospectedTable> =
        if (subpackageConfiguration == null) {
            allTables.values
        } else {
            allTables.values
                .filter { introspectedTable ->
                    val subpackage =
                        subpackageConfiguration.getSubpackage(introspectedTable.aliasedFullyQualifiedTableNameAtRuntime)
                    (group.includeRecursive && group.rootPackage == null) ||
                        group.includePackages.contains(subpackage) ||
                        (group.includeRecursive && subpackage.startsWith("${group.rootPackage}.")) ||
                        group.includeTables.contains(introspectedTable.aliasedFullyQualifiedTableNameAtRuntime)
                }
                .filter { !group.excludeTables.contains(it.aliasedFullyQualifiedTableNameAtRuntime) }
        }

    private fun calculateKeys() {
        try {
            context.connection.use { conn ->
                for (introspectedTable in tablesToDocument) {
                    parseImportedKeys(conn, introspectedTable, subpackageConfiguration)
                    parseExportedKeys(conn, introspectedTable)
                    parseKeyInfo(conn, introspectedTable)
                }
            }
        } catch (e: SQLException) {
            throw RuntimeException(e)
        }
    }

    private fun parseImportedKeys(
        conn: Connection,
        introspectedTable: IntrospectedTable,
        subPackagePluginConfiguration: SubpackageConfiguration?,
    ) {
        val importedKeysResultSet =
            conn.metaData.getImportedKeys(conn.catalog, conn.schema, introspectedTable.fullyQualifiedTableNameAtRuntime)
        while (importedKeysResultSet.next()) {
            val pktableName = importedKeysResultSet.getString("PKTABLE_NAME")
            importedKeys.computeIfAbsent(introspectedTable) { TreeSet() }.add(pktableName)
            if (domainEnumConfiguration?.isDomainEnumTable(pktableName) == true) {
                includedEnums.add(pktableName)
                if (subPackagePluginConfiguration != null) {
                    enumsUsedFromPackage
                        .computeIfAbsent(pktableName) { HashSet() }
                        .add(subPackagePluginConfiguration.getSubpackage(introspectedTable.aliasedFullyQualifiedTableNameAtRuntime))
                }
            }
        }
    }

    private fun parseExportedKeys(conn: Connection, introspectedTable: IntrospectedTable) {
        val exportedKeysResultSet =
            conn.metaData.getExportedKeys(conn.catalog, conn.schema, introspectedTable.fullyQualifiedTableNameAtRuntime)
        while (exportedKeysResultSet.next()) {
            val pktableName = exportedKeysResultSet.getString("PKTABLE_NAME")
            exportedKeys.computeIfAbsent(introspectedTable) { TreeSet() }.add(pktableName)
        }
    }

    private fun parseKeyInfo(conn: Connection, introspectedTable: IntrospectedTable) {
        MetadataProvider.getMetadataProvider(conn)
            ?.getConstraints(conn, introspectedTable.fullyQualifiedTableNameAtRuntime)
            ?.forEach {
                keyInfoMap
                    .computeIfAbsent(introspectedTable) { mutableMapOf() }
                    .computeIfAbsent(
                        KeyInfo(
                            it.constraintType,
                            it.constraintName,
                            it.remarks,
                            it.refTableName,
                        ),
                    ) { ColumnData() }
                    .add(it.columnName, it.refColumnName)
            }

        val map = keyInfoMap[introspectedTable]
        map?.keys?.filter { it.type == "PRIMARY KEY" }?.forEach { it.label = "PK" }
        val foreignKeys: List<KeyInfo> = map?.keys?.filter { it.type == "FOREIGN KEY" } ?: emptyList()
        if (foreignKeys.size == 1) {
            foreignKeys[0].label = "FK"
        } else {
            foreignKeys.sortedBy { it.name }.forEachIndexed { i, info -> info.label = "FK${i + 1}" }
        }
        val uniqueKeys: List<KeyInfo> = map?.keys?.filter { it.type == "UNIQUE" } ?: emptyList()
        if (uniqueKeys.size == 1) {
            uniqueKeys[0].label = "UK"
        } else {
            uniqueKeys.sortedBy { it.name }.forEachIndexed { i, info -> info.label = "UK${i + 1}" }
        }
    }

    data class KeyInfo(
        val type: String,
        val name: String,
        val remarks: String?,
        val refTable: String?,
        var label: String? = null,
    )

    data class ColumnData(
        val columns: MutableSet<String> = linkedSetOf(),
        val refColumns: MutableSet<String> = linkedSetOf(),
    ) {
        fun add(column: String, refColumn: String?) {
            columns.add(column)
            refColumn?.let { refColumns.add(it) }
        }
    }
}
