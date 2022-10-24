package nl.litpho.mybatis.generator.plugins.asciidoc

import nl.litpho.mybatis.generator.file.GeneratedFlatFile
import nl.litpho.mybatis.generator.plugins.asciidoc.AsciidocConfiguration.GroupDefinition
import org.mybatis.generator.api.IntrospectedColumn
import java.sql.Types

class GeneratedAsciidocTableDescriptionFile(
    group: GroupDefinition,
    targetProject: String,
    private val groupModel: AsciidocGroupModel
) : GeneratedFlatFile(group.filename + "-table-description.adoc", "", targetProject) {

    override fun getFormattedContent(): String {
        val sb = StringBuilder()
        for (table in groupModel.tablesToDocument) {
            if (table.fullyQualifiedTableNameAtRuntime.startsWith("DOM_")) {
                continue
            }
            sb.append("== ").append(table.fullyQualifiedTableNameAtRuntime).append("\n")
            table.remarks?.let { sb.append(table.remarks).append("\n\n") }
            sb.append("=== Columns\n")
            sb.append("[%header, cols=5*]\n")
            sb.append("|===\n")
            sb.append("|Name|Type|Nullable|Primary Key|Description\n")
            for (column in table.primaryKeyColumns) {
                sb.append("|")
                    .append(column.actualColumnName)
                    .append("|")
                    .append(getJdbcTypeString(column))
                    .append("|")
                    .append(getNullableString(column))
                    .append("|Y|")
                    .append(column.remarks ?: "")
                    .append("\n")
            }
            for (column in table.nonPrimaryKeyColumns) {
                sb.append("|")
                    .append(column.actualColumnName)
                    .append("|")
                    .append(getJdbcTypeString(column))
                    .append("|")
                    .append(getNullableString(column))
                    .append("|N|")
                    .append(column.remarks ?: "")
                    .append("\n")
            }
            sb.append("|===\n")
            sb.append("\n")

            sb.append("=== Primary/Unique Keys\n")
            sb.append("[%header]\n")
            sb.append("|===\n")
            sb.append("|Label|Name|Type|Columns|Comments\n")
            val keyInfo = groupModel.keyInfoMap.getOrDefault(table, emptyMap())
            keyInfo.entries.filter { e -> e.key.type == "PRIMARY KEY" }
            for ((info, columnData) in keyInfo.entries.filter { e -> e.key.type == "PRIMARY KEY" }.sortedBy { it.key.name }) {
                sb.append("|${info.label}|${info.name}|${info.type}|${columnData.columns.joinToString(",")}|${info.remarks ?: ""}\n")
            }
            for ((info, columnData) in keyInfo.entries.filter { e -> e.key.type == "UNIQUE" }.sortedBy { it.key.name }) {
                sb.append("|${info.label}|${info.name}|${info.type}|${columnData.columns.joinToString(",")}|${info.remarks ?: ""}\n")
            }
            sb.append("|===\n")
            sb.append("\n")
            sb.append("=== Foreign Keys\n")
            sb.append("[%header]\n")
            sb.append("|===\n")
            sb.append("|Label|Name|Columns|Ref.Table|Ref.Columns|Comments\n")
            for ((info, columnData) in keyInfo.entries.filter { e -> e.key.type == "FOREIGN KEY" }.sortedBy { it.key.name }) {
                sb.append("|${info.label}|${info.name}|${columnData.columns.joinToString(",")}|${info.refTable}|${columnData.refColumns.joinToString(",")}|${info.remarks ?: ""}\n")
            }
            sb.append("|===\n")
            sb.append("\n")
        }
        return sb.toString()
    }

    private fun getNullableString(introspectedColumn: IntrospectedColumn): String =
        if (introspectedColumn.isNullable) "Y" else "N"
}

fun getJdbcTypeString(introspectedColumn: IntrospectedColumn): String =
    if (introspectedColumn.length == 65535 && introspectedColumn.scale == 32767) {
        "NUMBER"
    } else {
        if (introspectedColumn.jdbcType == Types.BINARY && introspectedColumn.length == 16) {
            "UUID"
        } else {
            when (introspectedColumn.jdbcType) {
                Types.CHAR -> "CHAR(${introspectedColumn.length})"
                Types.VARCHAR -> "VARCHAR(${introspectedColumn.length})"
                Types.FLOAT, Types.REAL -> "FLOAT"
                Types.DECIMAL, Types.INTEGER, Types.NUMERIC, Types.VARBINARY -> "NUMBER(${introspectedColumn.length},${introspectedColumn.scale})"
                Types.DATE -> "DATE"
                Types.TIME -> "TIME"
                Types.TIMESTAMP -> "TIMESTAMP"
                Types.BLOB -> "BLOB"
                Types.BOOLEAN -> "BOOLEAN"
                else -> "${introspectedColumn.jdbcTypeName}(${introspectedColumn.length},${introspectedColumn.scale})"
            }
        }
    }
