package nl.litpho.mybatis.generator.plugins.builder

import nl.litpho.mybatis.generator.plugins.util.capitalize
import org.mybatis.generator.api.CommentGenerator
import org.mybatis.generator.api.IntrospectedColumn
import org.mybatis.generator.api.IntrospectedTable
import org.mybatis.generator.api.dom.java.CompilationUnit
import org.mybatis.generator.api.dom.java.Field
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType
import org.mybatis.generator.api.dom.java.JavaVisibility
import org.mybatis.generator.api.dom.java.Method
import org.mybatis.generator.api.dom.java.Parameter
import org.mybatis.generator.api.dom.java.TopLevelClass
import org.mybatis.generator.codegen.AbstractJavaGenerator
import org.mybatis.generator.config.Context
import org.mybatis.generator.internal.util.JavaBeansUtil

class UpdateSelectionGenerator(
    project: String,
    introspectedTable: IntrospectedTable,
    context: Context
) : AbstractJavaGenerator(project) {

    init {
        super.introspectedTable = introspectedTable
        super.context = context
    }

    override fun getCompilationUnits(): List<CompilationUnit> {
        val commentGenerator: CommentGenerator = context.commentGenerator
        val updateSelectionType = FullyQualifiedJavaType("${introspectedTable.baseRecordType}UpdateSelection")
        val baseRecordTypeFQJT = FullyQualifiedJavaType(introspectedTable.baseRecordType)
        val superInterface = FullyQualifiedJavaType("UpdateSelection<${baseRecordTypeFQJT.shortName}>")
        val topLevelClass = TopLevelClass(updateSelectionType).apply {
            visibility = JavaVisibility.PUBLIC
            addSuperInterface(superInterface)
            addImportedType("nl.litpho.mybatis.dto.UpdateSelection")
            addImportedType("javax.annotation.CheckForNull")
        }
        commentGenerator.addJavaFileComment(topLevelClass)

        for (column: IntrospectedColumn in introspectedTable.allColumns) {
            val field = column.createField()
            topLevelClass.addField(field)
            topLevelClass.addImportedType(field.type)
            commentGenerator.addFieldComment(field, introspectedTable, column)
        }

        for (field: Field in createUpdateFields()) {
            topLevelClass.addField(field)
            commentGenerator.addFieldComment(field, introspectedTable)
        }

        createEmptyConstructor(updateSelectionType).also {
            topLevelClass.addMethod(it)
            commentGenerator.addGeneralMethodComment(it, introspectedTable)
        }

        createPrimaryKeyConstructor(updateSelectionType)?.let {
            topLevelClass.addMethod(it)
            commentGenerator.addGeneralMethodComment(it, introspectedTable)
        }

        createKeyTypeConstructor(updateSelectionType)?.let {
            topLevelClass.addImportedType(introspectedTable.primaryKeyType)
            topLevelClass.addMethod(it)
            commentGenerator.addGeneralMethodComment(it, introspectedTable)
        }

        createEmptyStaticFactoryMethod(updateSelectionType).let {
            topLevelClass.addMethod(it)
            commentGenerator.addGeneralMethodComment(it, introspectedTable)
        }

        createPrimaryKeyStaticFactoryMethod(updateSelectionType)?.let {
            topLevelClass.addMethod(it)
            commentGenerator.addGeneralMethodComment(it, introspectedTable)
        }

        createPrimaryKeyClassStaticFactoryMethod(updateSelectionType)?.let {
            topLevelClass.addMethod(it)
            commentGenerator.addGeneralMethodComment(it, introspectedTable)
        }

        for (column: IntrospectedColumn in introspectedTable.nonPrimaryKeyColumns) {
            val setter = createSetter(column, updateSelectionType)
            topLevelClass.addMethod(setter)
            commentGenerator.addSetterComment(setter, introspectedTable, column)
        }

        for (field: Field in topLevelClass.fields) {
            val column = findColumn(field)
            val getter = createGetter(column, field)

            topLevelClass.addMethod(getter)
            if (column == null) {
                commentGenerator.addGeneralMethodComment(getter, introspectedTable)
            } else {
                commentGenerator.addGetterComment(getter, introspectedTable, column)
            }
        }

        return listOf<CompilationUnit>(topLevelClass)
    }

    private fun IntrospectedColumn.createField(): Field =
        Field(javaProperty, primitiveColumn()).apply {
            visibility = JavaVisibility.PRIVATE
            addAnnotation("@CheckForNull")
        }

    private fun createUpdateFields(): List<Field> =
        introspectedTable.nonPrimaryKeyColumns
            .map { column ->
                val fieldName = "update${column.javaProperty.capitalize()}"
                Field(fieldName, FullyQualifiedJavaType.getBooleanPrimitiveInstance()).apply {
                    visibility = JavaVisibility.PRIVATE
                }
            }

    private fun createEmptyConstructor(updateSelectionType: FullyQualifiedJavaType): Method =
        Method(updateSelectionType.shortName).apply {
            isConstructor = true
            visibility = JavaVisibility.PUBLIC
            addBodyLine("// empty constructor")
        }

    private fun createPrimaryKeyConstructor(updateSelectionType: FullyQualifiedJavaType): Method? =
        if (introspectedTable.primaryKeyColumns.isNotEmpty()) {
            val primaryKeyConstructor = Method(updateSelectionType.shortName).apply {
                isConstructor = true
                visibility = JavaVisibility.PUBLIC
            }
            for (primaryKeyColumn: IntrospectedColumn in introspectedTable.primaryKeyColumns) {
                val columnJavaProperty = primaryKeyColumn.javaProperty
                val parameter = Parameter(primaryKeyColumn.fullyQualifiedJavaType, columnJavaProperty)
                with(primaryKeyConstructor) {
                    addParameter(parameter)
                    addBodyLine("this.$columnJavaProperty = $columnJavaProperty;")
                }
            }
            primaryKeyConstructor
        } else {
            null
        }

    private fun createKeyTypeConstructor(updateSelectionType: FullyQualifiedJavaType): Method? =
        if (introspectedTable.primaryKeyColumns.size > 1 && (context.targetRuntime == "MyBatis3")) {
            Method(updateSelectionType.shortName).apply {
                isConstructor = true
                visibility = JavaVisibility.PUBLIC
                addParameter(Parameter(FullyQualifiedJavaType(introspectedTable.primaryKeyType), "key"))

                for (primaryKeyColumn: IntrospectedColumn in introspectedTable.primaryKeyColumns) {
                    val columnJavaProperty = primaryKeyColumn.javaProperty
                    val getterMethodName = JavaBeansUtil.getGetterMethodName(columnJavaProperty, primaryKeyColumn.fullyQualifiedJavaType)
                    addBodyLine("this.$columnJavaProperty = key.$getterMethodName();")
                }
            }
        } else {
            null
        }

    private fun createEmptyStaticFactoryMethod(updateSelectionType: FullyQualifiedJavaType): Method =
        Method("set").apply {
            visibility = JavaVisibility.PUBLIC
            isStatic = true
            setReturnType(updateSelectionType)
            addBodyLine("return new ${updateSelectionType.shortName}();")
        }

    private fun createPrimaryKeyStaticFactoryMethod(updateSelectionType: FullyQualifiedJavaType): Method? =
        if (introspectedTable.primaryKeyColumns.isNotEmpty()) {
            Method("set").apply {
                visibility = JavaVisibility.PUBLIC
                isStatic = true
                setReturnType(updateSelectionType)

                for (primaryKeyColumn: IntrospectedColumn in introspectedTable.primaryKeyColumns) {
                    val columnJavaProperty: String = primaryKeyColumn.javaProperty
                    val parameter = Parameter(primaryKeyColumn.fullyQualifiedJavaType, columnJavaProperty)
                    addParameter(parameter)
                }
                val joinedParameters: String = parameters.joinToString(", ") { it.name }
                addBodyLine("return new ${updateSelectionType.shortName}($joinedParameters);")
            }
        } else {
            null
        }

    private fun createPrimaryKeyClassStaticFactoryMethod(updateSelectionType: FullyQualifiedJavaType): Method? =
        if (introspectedTable.primaryKeyColumns.size > 1 && ("MyBatis3" == context.targetRuntime)) {
            Method("set").apply {
                visibility = JavaVisibility.PUBLIC
                isStatic = true
                setReturnType(updateSelectionType)
                addParameter(Parameter(FullyQualifiedJavaType(introspectedTable.primaryKeyType), "key"))
                addBodyLine("return new ${updateSelectionType.shortName}(key);")
            }
        } else {
            null
        }

    private fun createSetter(column: IntrospectedColumn, updateSelectionType: FullyQualifiedJavaType): Method =
        Method(JavaBeansUtil.getSetterMethodName(column.javaProperty)).apply {
            addParameter(Parameter(column.fullyQualifiedJavaType, column.javaProperty))
            visibility = JavaVisibility.PUBLIC
            setReturnType(updateSelectionType)
            addBodyLine("this.${column.javaProperty} = ${column.javaProperty};")
            addBodyLine("this.update${column.javaProperty.capitalize()} = true;")
            addBodyLine("return this;")
        }

    private fun createGetter(column: IntrospectedColumn?, field: Field): Method =
        Method(bepaalGetterMethodName(column, field)).apply {
            visibility = JavaVisibility.PUBLIC
            setReturnType(field.type)
            addBodyLine("return ${field.name};")

            if (FullyQualifiedJavaType.getBooleanPrimitiveInstance() != field.type) {
                addAnnotation("@CheckForNull")
            }
        }

    private fun bepaalGetterMethodName(column: IntrospectedColumn?, field: Field): String =
        when (column?.fullyQualifiedJavaType?.fullyQualifiedName) {
            "boolean" -> JavaBeansUtil.getGetterMethodName(field.name, FullyQualifiedJavaType.getBooleanPrimitiveInstance())
            else -> JavaBeansUtil.getGetterMethodName(field.name, field.type)
        }

    private fun IntrospectedColumn.primitiveColumn() =
        fullyQualifiedJavaType.run {
            if (isPrimitive) {
                primitiveTypeWrapper
            } else {
                this
            }
        }

    private fun findColumn(field: Field): IntrospectedColumn? = introspectedTable.allColumns.firstOrNull { (field.name == it.javaProperty) }
}
