package nl.litpho.mybatis.generator.plugins.domainenum

import nl.litpho.mybatis.generator.plugins.domainenum.DomainEnumConfiguration.DomainEnumTableDefinition
import org.mybatis.generator.api.CommentGenerator
import org.mybatis.generator.api.GeneratedJavaFile
import org.mybatis.generator.api.IntrospectedColumn
import org.mybatis.generator.api.IntrospectedTable
import org.mybatis.generator.api.dom.java.CompilationUnit
import org.mybatis.generator.api.dom.java.Field
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType
import org.mybatis.generator.api.dom.java.JavaVisibility
import org.mybatis.generator.api.dom.java.Method
import org.mybatis.generator.api.dom.java.Parameter
import org.mybatis.generator.api.dom.java.TopLevelEnumeration
import org.mybatis.generator.codegen.AbstractJavaGenerator
import org.mybatis.generator.config.Context
import org.mybatis.generator.config.PropertyRegistry
import java.sql.SQLException
import java.util.Locale

private const val CHECK_FOR_NULL: String = "@CheckForNull"

private const val CODE: String = "CODE"

private const val DATABASE_VALUE: String = "DATABASE_VALUE"

private const val OMSCHRIJVING: String = "OMSCHRIJVING"

private const val VOLGORDE: String = "VOLGORDE"

class DomainEnumGenerator(
    project: String,
    introspectedTable: IntrospectedTable,
    context: Context,
    private val configuration: DomainEnumConfiguration
) : AbstractJavaGenerator(project) {

    init {
        super.introspectedTable = introspectedTable
        super.context = context
    }

    override fun getCompilationUnits(): List<CompilationUnit> {
        println("Generating Enum class for table " + introspectedTable.fullyQualifiedTable)
        val commentGenerator: CommentGenerator = context.commentGenerator
        val enumType = FullyQualifiedJavaType(getEnumType())
        val topLevelEnumeration = TopLevelEnumeration(enumType).apply {
            visibility = JavaVisibility.PUBLIC
        }
        commentGenerator.addJavaFileComment(topLevelEnumeration)
        val parameterColumns: List<ParameterColumn> = getParameterColumns()
        val order: String = if (introspectedTable.getColumn(VOLGORDE).isPresent) VOLGORDE else CODE
        addEnumValues(topLevelEnumeration, getEnumValues(order), parameterColumns)
        if (parameterColumns.isNotEmpty()) {
            for (parameterColumn: ParameterColumn in parameterColumns) {
                val field = createPrivateFinalField(parameterColumn)
                val getter = createCheckForNullGetterIfNullable(field, parameterColumn, topLevelEnumeration)
                with(topLevelEnumeration) {
                    addField(field)
                    addMethod(getter)
                    addImportedType(parameterColumn.fullyQualifiedJavaType)
                }
            }
            addConstructorToToplevelEnumeration(topLevelEnumeration, enumType, parameterColumns, commentGenerator)
        }

        return listOf<CompilationUnit>(topLevelEnumeration)
    }

    private fun addConstructorToToplevelEnumeration(
        topLevelEnumeration: TopLevelEnumeration,
        enumType: FullyQualifiedJavaType,
        parameterColumns: List<ParameterColumn>,
        commentGenerator: CommentGenerator
    ) {
        val constructor = Method(enumType.shortName).apply {
            isConstructor = true
        }
        for (parameterColumn: ParameterColumn in parameterColumns) {
            val parameter = Parameter(parameterColumn.fullyQualifiedJavaType, parameterColumn.javaProperty).apply {
                if (parameterColumn.isNullable) {
                    addAnnotation(CHECK_FOR_NULL)
                }
            }
            with(constructor) {
                addParameter(parameter)
                addBodyLine("this.${parameterColumn.javaProperty} = ${parameterColumn.javaProperty};")
            }
        }
        topLevelEnumeration.addMethod(constructor)
        commentGenerator.addGeneralMethodComment(constructor, introspectedTable)
    }

    private fun createCheckForNullGetterIfNullable(
        field: Field,
        parameterColumn: ParameterColumn,
        topLevelEnumeration: TopLevelEnumeration
    ): Method {
        val getter = getGetter(field)
        if (parameterColumn.isNullable) {
            field.addAnnotation(CHECK_FOR_NULL)
            getter.addAnnotation(CHECK_FOR_NULL)
            topLevelEnumeration.addImportedType(FullyQualifiedJavaType("javax.annotation.CheckForNull"))
        }
        return getter
    }

    private fun createPrivateFinalField(parameterColumn: ParameterColumn): Field =
        Field(parameterColumn.javaProperty, parameterColumn.fullyQualifiedJavaType).apply {
            visibility = JavaVisibility.PRIVATE
            isFinal = true
        }

    private fun getCompilationUnits(tableDefinition: DomainEnumTableDefinition): List<CompilationUnit> {
        val fullyQualifiedJavaType = FullyQualifiedJavaType(getEnumType())
        val topLevelEnumeration = getTopLevelEnumeration(tableDefinition, fullyQualifiedJavaType).apply {
            visibility = JavaVisibility.PUBLIC
        }

        return listOf<CompilationUnit>(topLevelEnumeration)
    }

    private fun getTopLevelEnumeration(tableDefinition: DomainEnumTableDefinition, enumType: FullyQualifiedJavaType): TopLevelEnumeration {
        val commentGenerator: CommentGenerator = context.commentGenerator
        val topLevelEnumeration = TopLevelEnumeration(enumType).apply {
            visibility = JavaVisibility.PUBLIC
        }
        val superInterface = FullyQualifiedJavaType("nl.litpho.mybatis.enumsupport.DatabaseValueEnum")
        val extendsInterface = FullyQualifiedJavaType("DatabaseValueEnum")
        topLevelEnumeration.addSuperInterface(extendsInterface)
        topLevelEnumeration.addImportedType(superInterface)
        if (!CODE.equals(tableDefinition.valueColumn, ignoreCase = true)) {
            tableDefinition.excludeColumns.add(tableDefinition.valueColumn)
        }
        val parameterColumns: MutableList<ParameterColumn> = getParameterColumns(tableDefinition.excludeColumns)
        if (tableDefinition.generateEnumValue || !CODE.equals(tableDefinition.valueColumn, ignoreCase = true)) {
            val dbValueColumn = IntrospectedColumn().apply {
                fullyQualifiedJavaType = FullyQualifiedJavaType.getStringInstance()
                actualColumnName = DATABASE_VALUE
                javaProperty = "databaseValue"
            }
            parameterColumns.add(ParameterColumn(dbValueColumn))
        }
        val enumDataList: List<Map<String, String>> = getEnumDataList(tableDefinition)
        addEnumValues(topLevelEnumeration, enumDataList, parameterColumns)
        for (map: Map<String, String> in enumDataList) {
            val code: String = map[CODE]!!
            val databaseValue: String = map[DATABASE_VALUE] ?: code
            configuration.addDomainEnumDatabaseValues(enumType.fullyQualifiedName, databaseValue, code)
        }
        if (parameterColumns.isNotEmpty()) {
            for (parameterColumn: ParameterColumn in parameterColumns) {
                val field = createPrivateFinalField(parameterColumn)
                val getter = createCheckForNullGetterIfNullable(field, parameterColumn, topLevelEnumeration)
                if (DATABASE_VALUE.equals(parameterColumn.actualColumnName, ignoreCase = true) ||
                    OMSCHRIJVING.equals(parameterColumn.actualColumnName, ignoreCase = true)
                ) {
                    getter.addAnnotation("@Override")
                }
                with(topLevelEnumeration) {
                    addField(field)
                    addMethod(getter)
                    addImportedType(parameterColumn.fullyQualifiedJavaType)
                }
                commentGenerator.addFieldComment(field, introspectedTable)
                commentGenerator.addGeneralMethodComment(getter, introspectedTable)
            }
            val getByDatabaseValueMethod = Method("getByDatabaseValue").apply {
                visibility = JavaVisibility.PUBLIC
                isStatic = true
                setReturnType(enumType)
                addParameter(Parameter(FullyQualifiedJavaType.getStringInstance(), "databaseValue"))
                addBodyLine("requireNonNull(databaseValue, \"databaseValue\");")
                addBodyLine("return Arrays.stream(values())")
                addBodyLine("\t.filter(value -> value.getDatabaseValue().equals(databaseValue))")
                addBodyLine("\t.findFirst()")
                addBodyLine("\t.orElseThrow(() -> new IllegalArgumentException(\"${enumType.shortName}\" + databaseValue + \" does not exist\"));")
            }
            with(topLevelEnumeration) {
                addMethod(getByDatabaseValueMethod)
                addImportedType(FullyQualifiedJavaType("java.util.Arrays"))
                addStaticImport("java.util.Objects.requireNonNull")
            }
            addConstructorToToplevelEnumeration(topLevelEnumeration, enumType, parameterColumns, commentGenerator)
        }

        return topLevelEnumeration
    }

    private fun getEnumDataList(tableDefinition: DomainEnumTableDefinition): List<Map<String, String>> {
        val enumDataList: MutableList<Map<String, String>> = mutableListOf()
        val order: String = tableDefinition.orderColumn ?: tableDefinition.valueColumn
        for (map: Map<String, String> in getEnumValues(order)) {
            val enumMap: MutableMap<String, String> = mutableMapOf<String, String>().apply { putAll(map) }
            tableDefinition.excludeColumns.forEach(enumMap::remove)
            val descriptionColumnUpperCased: String? = getDescriptionColumnUpperCased(tableDefinition)
            val description: String? = map[descriptionColumnUpperCased]
            val dbValue: String = map.getValue(tableDefinition.valueColumn.uppercase())
            if (tableDefinition.generateEnumValue) {
                val enumValue: String = description!!
                    .replace(' ', '_')
                    .replace('-', '_')
                    .replace(".", "")
                    .replace(",", "")
                    .uppercase(Locale.getDefault())
                enumMap[CODE] = enumValue
            }
            enumMap[DATABASE_VALUE] = dbValue
            if (OMSCHRIJVING != descriptionColumnUpperCased) {
                description?.let { enumMap[OMSCHRIJVING] = it }
                enumMap.remove(descriptionColumnUpperCased)
            }
            enumDataList.add(enumMap)
        }

        return enumDataList
    }

    private fun getDescriptionColumnUpperCased(tableDefinition: DomainEnumTableDefinition): String? =
        if (tableDefinition.descriptionColumn == null) {
            null
        } else {
            tableDefinition.descriptionColumn.uppercase(Locale.getDefault())
        }

    private fun addEnumValues(
        topLevelEnumeration: TopLevelEnumeration,
        enumValues: List<Map<String, String>>,
        parameterColumns: List<ParameterColumn>
    ) {
        val enumConstants: MutableList<String> = mutableListOf()
        for (enumValue: Map<String, String> in enumValues) {
            var value: String = enumValue.getValue(CODE)
            enumConstants.add(value)
            if (parameterColumns.isNotEmpty()) {
                value += "("
                value += bepaalParams(parameterColumns, enumValue).joinToString(", ")
                value += ")"
            }
            topLevelEnumeration.addEnumConstant(value)
        }

        configuration.addEnumConstants(introspectedTable, enumConstants)
    }

    private fun bepaalParams(parameterColumns: List<ParameterColumn>, enumValue: Map<String, String>): List<String> =
        parameterColumns.map { parameterColumn ->
            val columnValue = enumValue[parameterColumn.actualColumnName.uppercase()]
            when {
                columnValue == null -> "null"
                parameterColumn.isString() -> "\"" + columnValue + "\""
                parameterColumn.isBoolean() -> if ("J".equals(columnValue.toString(), ignoreCase = true)) "true" else "false"
                parameterColumn.fullyQualifiedJavaType.isPrimitive -> columnValue.toString()
                parameterColumn.fullyQualifiedJavaType.packageName.startsWith(configuration.targetPackage) ->
                    "${parameterColumn.fullyQualifiedJavaType.shortName}.$columnValue"

                else -> "new ${parameterColumn.fullyQualifiedJavaType.shortName}(\"$columnValue\")"
            }
        }

    // This SQL string concatenation is safe enough
    private fun getEnumValues(order: String): List<Map<String, String>> {
        val result: MutableList<Map<String, String>> = mutableListOf()
        val sql: String = "SELECT * FROM " + introspectedTable.fullyQualifiedTableNameAtRuntime
        try {
            context.connection.use { conn ->
                conn.prepareStatement("$sql ORDER BY $order").use { ps ->
                    ps.executeQuery().use { rs ->
                        while (rs.next()) {
                            val record = introspectedTable.allColumns
                                .associate { introspectedColumn ->
                                    val columnName: String = introspectedColumn.actualColumnName
                                    columnName.uppercase() to rs.getString(columnName)
                                }
                            result.add(record)
                        }
                    }
                }
            }
        } catch (e: SQLException) {
            throw RuntimeException(e)
        }
        return result
    }

    private fun getParameterColumns(excludeColumns: List<String> = emptyList()): MutableList<ParameterColumn> =
        introspectedTable.allColumns
            .asSequence()
            .filter { !it.actualColumnName.equals(CODE, ignoreCase = true) }
            .filter { !it.actualColumnName.equals(VOLGORDE, ignoreCase = true) }
            .filter { !excludeColumns.contains(it.actualColumnName) }
            .map {
                ParameterColumn(it)
            }
            .toMutableList()

    private fun getEnumType(): String =
        introspectedTable.baseRecordType.replace(context.javaModelGeneratorConfiguration.targetPackage, configuration.targetPackage)

    companion object {

        fun createDomeinEnumForTable(
            project: String,
            introspectedTable: IntrospectedTable,
            context: Context,
            configuration: DomainEnumConfiguration
        ): List<GeneratedJavaFile> =
            DomainEnumGenerator(project, introspectedTable, context, configuration).compilationUnits
                .map { topLevelEnumeration: CompilationUnit ->
                    GeneratedJavaFile(
                        topLevelEnumeration,
                        context.javaModelGeneratorConfiguration.targetProject,
                        context.getProperty(PropertyRegistry.CONTEXT_JAVA_FILE_ENCODING),
                        context.javaFormatter
                    )
                }
                .toList()

        fun createTypedDomeinEnumTables(
            project: String,
            introspectedTable: IntrospectedTable,
            tableDefinition: DomainEnumTableDefinition,
            context: Context,
            configuration: DomainEnumConfiguration
        ): List<GeneratedJavaFile> {
            val generator = DomainEnumGenerator(project, introspectedTable, context, configuration)
            val topLevelEnumerations: List<CompilationUnit> = generator.getCompilationUnits(tableDefinition)
            return topLevelEnumerations
                .map { compilationUnit ->
                    GeneratedJavaFile(
                        compilationUnit,
                        context.javaModelGeneratorConfiguration.targetProject,
                        context.getProperty(PropertyRegistry.CONTEXT_JAVA_FILE_ENCODING),
                        context.javaFormatter
                    )
                }
                .toList()
        }
    }
}
