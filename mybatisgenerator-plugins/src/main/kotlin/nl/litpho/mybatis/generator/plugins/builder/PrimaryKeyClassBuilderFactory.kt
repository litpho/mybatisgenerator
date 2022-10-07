package nl.litpho.mybatis.generator.plugins.builder

import nl.donna.generiek.mybatis.generator.util.PrimitiveUtil.getPrimitive
import org.mybatis.generator.api.CommentGenerator
import org.mybatis.generator.api.IntrospectedColumn
import org.mybatis.generator.api.IntrospectedTable
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType
import org.mybatis.generator.api.dom.java.JavaVisibility
import org.mybatis.generator.api.dom.java.Method
import org.mybatis.generator.api.dom.java.Parameter
import org.mybatis.generator.api.dom.java.TopLevelClass
import org.mybatis.generator.internal.DefaultCommentGenerator

object PrimaryKeyClassBuilderFactory {

    fun create(topLevelClass: TopLevelClass, introspectedTable: IntrospectedTable, usePrimitivesWherePossible: Boolean) {
        val commentGenerator: CommentGenerator = DefaultCommentGenerator()
        createNoArgConstructor(topLevelClass, introspectedTable, commentGenerator)
        createFullConstructor(topLevelClass, introspectedTable, commentGenerator, usePrimitivesWherePossible)
        topLevelClass.addImportedType("javax.annotation.Nullable")
        topLevelClass.addImportedType("nl.donna.generiek.valideer.Valideer")
    }

    private fun createNoArgConstructor(topLevelClass: TopLevelClass, introspectedTable: IntrospectedTable, commentGenerator: CommentGenerator) {
        val baseRecordType = FullyQualifiedJavaType(introspectedTable.primaryKeyType)
        val noArgConstructor = Method(baseRecordType.shortName).apply {
            isConstructor = true
            addBodyLine("// Mybatis constructor")
            visibility = JavaVisibility.PROTECTED
        }
        commentGenerator.addGeneralMethodComment(noArgConstructor, introspectedTable)
        topLevelClass.addMethod(noArgConstructor)
    }

    private fun createFullConstructor(
        topLevelClass: TopLevelClass,
        introspectedTable: IntrospectedTable,
        commentGenerator: CommentGenerator,
        usePrimitivesWherePossible: Boolean
    ) {
        val baseRecordType = FullyQualifiedJavaType(introspectedTable.primaryKeyType)
        val constructor = Method(baseRecordType.shortName)
        commentGenerator.addGeneralMethodComment(constructor, introspectedTable)
        constructor.isConstructor = true
        for (introspectedColumn: IntrospectedColumn in introspectedTable.primaryKeyColumns) {
            val parameterType = getPrimitive(introspectedColumn, usePrimitivesWherePossible, true)
            val parameter = Parameter(parameterType, introspectedColumn.javaProperty)
            if (introspectedColumn.isNullable) {
                parameter.addAnnotation("@Nullable")
            }
            constructor.addParameter(parameter)
        }
        for (introspectedColumn: IntrospectedColumn in introspectedTable.primaryKeyColumns) {
            if (!introspectedColumn.isNullable && !getPrimitive(introspectedColumn, usePrimitivesWherePossible, false).isPrimitive) {
                constructor.addBodyLine("Valideer.notNull(\"${introspectedColumn.javaProperty}\", ${introspectedColumn.javaProperty});")
            }
        }
        for (introspectedColumn: IntrospectedColumn in introspectedTable.primaryKeyColumns) {
            constructor.addBodyLine("this.${introspectedColumn.javaProperty} = ${introspectedColumn.javaProperty};")
        }
        constructor.visibility = JavaVisibility.PUBLIC
        topLevelClass.addMethod(constructor)
    }
}
