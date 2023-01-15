package nl.litpho.mybatis.generator.plugins.builder

import nl.litpho.mybatis.generator.plugins.naming.NamingConfiguration
import nl.litpho.mybatis.generator.plugins.naming.NamingConfigurationEntry
import nl.litpho.mybatis.generator.plugins.util.PrimitiveUtil
import nl.litpho.mybatis.generator.plugins.util.capitalize
import org.apache.ibatis.type.JdbcType
import org.mybatis.generator.api.CommentGenerator
import org.mybatis.generator.api.IntrospectedColumn
import org.mybatis.generator.api.IntrospectedTable
import org.mybatis.generator.api.dom.java.Field
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType
import org.mybatis.generator.api.dom.java.InnerClass
import org.mybatis.generator.api.dom.java.JavaVisibility
import org.mybatis.generator.api.dom.java.Method
import org.mybatis.generator.api.dom.java.Parameter
import org.mybatis.generator.api.dom.java.TopLevelClass
import org.mybatis.generator.internal.DefaultCommentGenerator

class BaseRecordClassBuilderFactory(
    private val topLevelClass: TopLevelClass,
    private val introspectedTable: IntrospectedTable,
    private val namingConfiguration: NamingConfiguration?,
) {

    private val commentGenerator: CommentGenerator = DefaultCommentGenerator()

    fun create(
        makeConstructorPrivate: Boolean,
        usePrimitivesWherePossible: Boolean,
        removeSetters: Boolean,
        methodPrefix: String,
        useIdGenerators: Boolean,
        targetRuntime: String,
    ) {
        createOrChangeNoArgConstructor(makeConstructorPrivate)
        createFullConstructor(targetRuntime)
        createBuilderInstanceMethod()
        createBuilderMethod()
        createBuilderFromDtoMethod()
        createBuilderClass(usePrimitivesWherePossible, methodPrefix, useIdGenerators)
        if (removeSetters) {
            removeSetters()
        }
    }

    private fun createOrChangeNoArgConstructor(makeConstructorPrivate: Boolean) {
        val baseRecordType = FullyQualifiedJavaType(introspectedTable.baseRecordType)
        val noArgConstructor = Method(baseRecordType.shortName).apply {
            isConstructor = true
            addBodyLine("// Mybatis constructor")
            visibility = if (makeConstructorPrivate) JavaVisibility.PRIVATE else JavaVisibility.PUBLIC
        }
        commentGenerator.addGeneralMethodComment(noArgConstructor, introspectedTable)
        topLevelClass.addMethod(noArgConstructor)
    }

    private fun createFullConstructor(targetRuntime: String) {
        val baseRecordType = FullyQualifiedJavaType(introspectedTable.baseRecordType)
        val constructor = Method(baseRecordType.shortName).apply {
            isConstructor = true
        }
        commentGenerator.addGeneralMethodComment(constructor, introspectedTable)
        for (introspectedColumn: IntrospectedColumn in introspectedTable.allColumns) {
            val parameterType = getParameterType(introspectedColumn, targetRuntime)
            val parameter = Parameter(parameterType, introspectedColumn.javaProperty)
            if (introspectedColumn.isNullable) {
                parameter.addAnnotation("@Nullable")
            }
            if (topLevelClass.superClass.isPresent) {
                topLevelClass.addImportedType(parameterType)
            }
            constructor.addParameter(parameter)
        }
        if (topLevelClass.superClass.isPresent) {
            val primaryKeyParameters: List<String> = introspectedTable.primaryKeyColumns
                .map { it.javaProperty }
                .toList()
            constructor.addBodyLine("super(${java.lang.String.join(", ", primaryKeyParameters)});")
        } else {
            getConstructorBodyLines(introspectedTable.primaryKeyColumns).forEach(constructor::addBodyLine)
        }
        getConstructorBodyLines(introspectedTable.nonPrimaryKeyColumns).forEach(constructor::addBodyLine)
        constructor.visibility = JavaVisibility.PRIVATE
        topLevelClass.addMethod(constructor)
    }

    private fun getParameterType(introspectedColumn: IntrospectedColumn, targetRuntime: String): FullyQualifiedJavaType =
        introspectedColumn.fullyQualifiedJavaType.run {
            if (isPrimitive && targetRuntime == "MyBatis3") {
                primitiveTypeWrapper
            } else {
                this
            }
        }

    private fun getConstructorBodyLines(columns: List<IntrospectedColumn>): List<String> = columns.flatMap { getConstructorBodyLine(it) }

    private fun getConstructorBodyLine(introspectedColumn: IntrospectedColumn): List<String> {
        val prefix = "this." + introspectedColumn.javaProperty + " = "
        val initialization = if (introspectedColumn.isNullable || introspectedColumn.fullyQualifiedJavaType.isPrimitive) {
            prefix + introspectedColumn.javaProperty + ";"
        } else {
            ("${prefix}requireNonNull(${introspectedColumn.javaProperty}, \"${introspectedColumn.javaProperty} should not be null\");")
        }
        return listOf(initialization)
    }

    private fun determineDefaultValue(introspectedColumn: IntrospectedColumn): String {
        val defaultValue = introspectedColumn.defaultValue.removeQuotes()
        val fullyQualifiedName = introspectedColumn.fullyQualifiedJavaType.fullyQualifiedName
        return when (introspectedColumn.fullyQualifiedJavaType.fullyQualifiedName) {
            "java.lang.String" -> "\"" + defaultValue + "\""
            "java.lang.Boolean", "boolean" -> determineDefaultBooleanValue(introspectedColumn, defaultValue)
            "java.lang.Integer", "int" -> defaultValue
            "java.lang.Long", "long" -> "${defaultValue}L"
            "java.math.BigInteger" ->
                when (defaultValue) {
                    "0" -> "BigInteger.ZERO"
                    else -> "BigInteger.valueOf(${defaultValue}L)"
                }

            "java.math.BigDecimal" ->
                when (defaultValue) {
                    "0" -> "BigDecimal.ZERO"
                    else -> "BigDecimal.valueOf(${defaultValue}L)"
                }

            else -> determineDefaultValueNamingOverride(fullyQualifiedName, introspectedColumn, defaultValue)
        }
    }

    private fun determineDefaultBooleanValue(introspectedColumn: IntrospectedColumn, defaultValue: String) =
        when (introspectedColumn.jdbcType) {
            JdbcType.BOOLEAN.TYPE_CODE -> defaultValue.lowercase()
            JdbcType.CHAR.TYPE_CODE, JdbcType.VARCHAR.TYPE_CODE ->
                when (defaultValue) {
                    "Y" -> "true"
                    "N" -> "false"
                    else -> throw IllegalStateException("Invalid default value for yes/no boolean '$defaultValue' - ${introspectedColumn.actualColumnName}")
                }

            else ->
                when (defaultValue) {
                    "1" -> "true"
                    "0" -> "false"
                    else -> throw IllegalStateException("Invalid default value for 0/1 boolean '$defaultValue' - ${introspectedColumn.actualColumnName}")
                }
        }

    private fun determineDefaultValueNamingOverride(fullyQualifiedName: String, introspectedColumn: IntrospectedColumn, defaultValue: String) =
        if (namingConfiguration != null) {
            val namingConfigurationEntry =
                namingConfiguration.getParseResultForType(introspectedColumn.fullyQualifiedJavaType.shortName)
            if (namingConfigurationEntry?.type == introspectedColumn.fullyQualifiedJavaType.shortName) {
                // Vervangen met nabewerking
                "\$enum$$fullyQualifiedName.$defaultValue\$enum$"
            } else {
                "$fullyQualifiedName.$defaultValue"
            }
        } else {
            defaultValue
        }

    private fun String.removeQuotes(): String =
        if (this.startsWith("'")) {
            this.substring(1, this.length - 1)
        } else {
            this
        }

    private fun createBuilderInstanceMethod() {
        val method = Method("toBuilder").apply {
            setReturnType(FullyQualifiedJavaType("Builder"))
            addBodyLine("return builder(this);")
            visibility = JavaVisibility.PUBLIC
        }
        commentGenerator.addGeneralMethodComment(method, introspectedTable)
        topLevelClass.addMethod(method)
    }

    private fun createBuilderMethod() {
        val method = Method("builder").apply {
            setReturnType(FullyQualifiedJavaType("Builder"))
            addBodyLine("return new Builder();")
            visibility = JavaVisibility.PUBLIC
            isStatic = true
        }
        commentGenerator.addGeneralMethodComment(method, introspectedTable)
        topLevelClass.addMethod(method)
    }

    private fun createBuilderFromDtoMethod() {
        val method = Method("builder").apply {
            addParameter(Parameter(FullyQualifiedJavaType(introspectedTable.baseRecordType), "dto"))
            setReturnType(FullyQualifiedJavaType("Builder"))
            addBodyLine("requireNonNull(dto, \"dto should not be null\");")
            addBodyLine("return new Builder(dto);")
            visibility = JavaVisibility.PUBLIC
            isStatic = true
        }
        commentGenerator.addGeneralMethodComment(method, introspectedTable)
        topLevelClass.addMethod(method)
    }

    private fun createBuilderClass(usePrimitivesWherePossible: Boolean, methodPrefix: String, useIdGenerators: Boolean) {
        val baseRecordType = FullyQualifiedJavaType(introspectedTable.baseRecordType)
        val builderType = FullyQualifiedJavaType("${introspectedTable.baseRecordType}.Builder")
        val tableConfiguration: NamingConfigurationEntry? =
            namingConfiguration?.getTableConfiguration(introspectedTable.aliasedFullyQualifiedTableNameAtRuntime)

        val innerClass = InnerClass("Builder").apply {
            visibility = JavaVisibility.PUBLIC
            isStatic = true
        }
        commentGenerator.addClassComment(innerClass, introspectedTable)

        for (field in createBuilderClassFields(useIdGenerators, tableConfiguration)) {
            commentGenerator.addFieldComment(field, introspectedTable)
            innerClass.addField(field)
        }

        val noArgConstructor = Method(builderType.shortName).apply {
            isConstructor = true
            addBodyLine("// No-arg constructor")
            visibility = JavaVisibility.PUBLIC
        }
        commentGenerator.addGeneralMethodComment(noArgConstructor, introspectedTable)
        innerClass.addMethod(noArgConstructor)

        val fromDtoConstructor = Method(builderType.shortName).apply {
            isConstructor = true
            visibility = JavaVisibility.PUBLIC
            addParameter(Parameter(baseRecordType, "dto"))
        }
        commentGenerator.addGeneralMethodComment(fromDtoConstructor, introspectedTable)
        for (introspectedColumn: IntrospectedColumn in introspectedTable.allColumns) {
            fromDtoConstructor.addBodyLine("this.${introspectedColumn.javaProperty} = dto.${getGetterMethod(introspectedColumn)}();")
        }
        innerClass.addMethod(fromDtoConstructor)

        for (introspectedColumn: IntrospectedColumn in introspectedTable.allColumns) {
            val method = Method(methodPrefix + introspectedColumn.javaProperty.capitalize()).apply {
                setReturnType(FullyQualifiedJavaType("Builder"))
                addParameter(createParameter(introspectedColumn, usePrimitivesWherePossible))
                addBodyLine("this.${introspectedColumn.javaProperty} = ${introspectedColumn.javaProperty};")
                addBodyLine("return this;")
                visibility = JavaVisibility.PUBLIC
            }
            commentGenerator.addGeneralMethodComment(method, introspectedTable)
            innerClass.addMethod(method)
        }

        val javaProperties: List<String> = introspectedTable.allColumns.map { it.javaProperty }.toList()
        val fieldsAsJoinedString: String = javaProperties.joinToString(", ")
        val method = Method("build").apply {
            setReturnType(baseRecordType)
            visibility = JavaVisibility.PUBLIC
        }
        commentGenerator.addGeneralMethodComment(method, introspectedTable)
        if (useIdGenerators && introspectedTable.primaryKeyColumns.size == 1) {
            val pkColumn = introspectedTable.primaryKeyColumns[0]
            topLevelClass.addImportedType(FullyQualifiedJavaType("nl.litpho.mybatis.idgenerators.IdGenerators"))
            val pkType = pkColumn.determineColumnType()
            method.addBodyLine("if (this.${pkColumn.javaProperty} == null && IdGenerators.supports(${pkType.shortName}.class)) {")
            method.addBodyLine("this.${pkColumn.javaProperty} = IdGenerators.get(${pkType.shortName}.class);")
            method.addBodyLine("}")
        }

        for (introspectedColumn: IntrospectedColumn in introspectedTable.allColumns) {
            if (doesColumnNeedsNonNullCheck(introspectedColumn)) {
                method.addBodyLine("requireNonNull(${introspectedColumn.javaProperty}, \"${introspectedColumn.javaProperty} should not be null\");")
            }
        }
        for (introspectedColumn: IntrospectedColumn in introspectedTable.nonPrimaryKeyColumns) {
            if (introspectedColumn.defaultValue != null) {
                method.addBodyLine("if (${introspectedColumn.javaProperty} == null) {")
                method.addBodyLine("this.${introspectedColumn.javaProperty} = ${determineDefaultValue(introspectedColumn)};")
                method.addBodyLine("}")
            }
        }
        method.addBodyLine("return new ${baseRecordType.shortName}($fieldsAsJoinedString);")
        innerClass.addMethod(method)
        topLevelClass.addInnerClass(innerClass)
    }

    private fun createBuilderClassFields(useIdGenerators: Boolean, tableConfiguration: NamingConfigurationEntry?): List<Field> =
        introspectedTable.allColumns
            .map { introspectedColumn ->
                val columnType = introspectedColumn.determineColumnType(useIdGenerators)
                Field(introspectedColumn.javaProperty, columnType).apply {
                    visibility = JavaVisibility.PRIVATE
                    if (tableConfiguration?.columnDefaultValues?.containsKey(introspectedColumn.actualColumnName) == true) {
                        setInitializationString(determineInitializationString(tableConfiguration, introspectedColumn, columnType))
                    }
                }
            }

    private fun createParameter(introspectedColumn: IntrospectedColumn, usePrimitivesWherePossible: Boolean): Parameter =
        Parameter(
            PrimitiveUtil.getPrimitive(introspectedColumn, usePrimitivesWherePossible, false),
            introspectedColumn.javaProperty,
        ).apply {
            if (introspectedColumn.isNullable) {
                addAnnotation("@Nullable")
            }
        }

    private fun IntrospectedColumn.determineColumnType(useIdGenerators: Boolean = true): FullyQualifiedJavaType =
        fullyQualifiedJavaType.run {
            if (useIdGenerators && isPrimitive) {
                primitiveTypeWrapper
            } else {
                this
            }
        }

    private fun determineInitializationString(
        tableConfiguration: NamingConfigurationEntry,
        introspectedColumn: IntrospectedColumn,
        columnType: FullyQualifiedJavaType,
    ): String? =
        tableConfiguration.columnDefaultValues[introspectedColumn.actualColumnName].run {
            if (columnType == FullyQualifiedJavaType.getStringInstance()) {
                "\"$this\""
            } else {
                this
            }
        }

    private fun doesColumnNeedsNonNullCheck(introspectedColumn: IntrospectedColumn): Boolean =
        !introspectedColumn.isNullable && introspectedColumn.fullyQualifiedJavaType.isPrimitive && (introspectedColumn.defaultValue?.isBlank() ?: false)

    private fun getGetterMethod(introspectedColumn: IntrospectedColumn): String {
        val name = introspectedColumn.javaProperty.capitalize()
        return if (introspectedColumn.fullyQualifiedJavaType == FullyQualifiedJavaType.getBooleanPrimitiveInstance()) {
            "is$name"
        } else {
            "get$name"
        }
    }

    private fun removeSetters() {
        val setters: Set<Method> = topLevelClass.methods.filter { it.name.startsWith("set") }.toSet()
        topLevelClass.methods.removeAll(setters)
    }
}
