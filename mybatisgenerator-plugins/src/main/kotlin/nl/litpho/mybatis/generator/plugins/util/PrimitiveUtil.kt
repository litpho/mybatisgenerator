package nl.litpho.mybatis.generator.plugins.util

import org.mybatis.generator.api.IntrospectedColumn
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType
import org.mybatis.generator.api.dom.java.Method
import org.mybatis.generator.api.dom.java.Parameter
import org.mybatis.generator.api.dom.java.TopLevelClass
import org.mybatis.generator.internal.util.JavaBeansUtil

object PrimitiveUtil {

    private val primitives: Map<FullyQualifiedJavaType, FullyQualifiedJavaType> = mapOf(
        FullyQualifiedJavaType("java.lang.Boolean") to FullyQualifiedJavaType.getBooleanPrimitiveInstance(),
        FullyQualifiedJavaType("java.lang.Byte") to FullyQualifiedJavaType("byte"),
        FullyQualifiedJavaType("java.lang.Character") to FullyQualifiedJavaType("character"),
        FullyQualifiedJavaType("java.lang.Double") to FullyQualifiedJavaType("double"),
        FullyQualifiedJavaType("java.lang.Float") to FullyQualifiedJavaType("float"),
        FullyQualifiedJavaType("java.lang.Integer") to FullyQualifiedJavaType.getIntInstance(),
        FullyQualifiedJavaType("java.lang.Long") to FullyQualifiedJavaType("long"),
        FullyQualifiedJavaType("java.lang.Short") to FullyQualifiedJavaType("short"),
    )

    fun getPrimitive(introspectedColumn: IntrospectedColumn, usePrimitivesWherePossible: Boolean, builder: Boolean): FullyQualifiedJavaType =
        if (!usePrimitivesWherePossible || introspectedColumn.isNullable || introspectedColumn.introspectedTable.primaryKeyColumns.contains(
                introspectedColumn,
            )
        ) {
            if (isPrimitivePrimaryKeyColumnForBuilder(introspectedColumn, builder)) {
                introspectedColumn.fullyQualifiedJavaType.primitiveTypeWrapper
            } else {
                introspectedColumn.fullyQualifiedJavaType
            }
        } else {
            primitives.getOrDefault(introspectedColumn.fullyQualifiedJavaType, introspectedColumn.fullyQualifiedJavaType)
        }

    // package private voor tests
    fun isPrimitivePrimaryKeyColumnForBuilder(introspectedColumn: IntrospectedColumn, builder: Boolean): Boolean =
        builder &&
            introspectedColumn.introspectedTable.primaryKeyColumns.contains(introspectedColumn) &&
            introspectedColumn.fullyQualifiedJavaType.isPrimitive

    fun replaceGetterReturnTypeWithPrimitive(topLevelClass: TopLevelClass, introspectedColumn: IntrospectedColumn) {
        if (keepCurrentType(introspectedColumn)) {
            return
        }

        val methodName = JavaBeansUtil.getGetterMethodName(introspectedColumn.javaProperty, introspectedColumn.fullyQualifiedJavaType)
        val method = findMethod(topLevelClass, methodName) ?: return
        method.setReturnType(primitives[introspectedColumn.fullyQualifiedJavaType])
    }

    fun replaceSetterParameterTypeWithPrimitive(topLevelClass: TopLevelClass, introspectedColumn: IntrospectedColumn) {
        if (keepCurrentType(introspectedColumn)) {
            return
        }

        val methodName = JavaBeansUtil.getSetterMethodName(introspectedColumn.javaProperty)
        val method = findMethod(topLevelClass, methodName) ?: return
        val oldParameter = method.parameters[0]
        method.parameters.clear()
        method.addParameter(Parameter(primitives[introspectedColumn.fullyQualifiedJavaType], oldParameter.name))
    }

    // package private voor tests
    fun keepCurrentType(introspectedColumn: IntrospectedColumn): Boolean =
        introspectedColumn.isNullable ||
            !primitives.containsKey(introspectedColumn.fullyQualifiedJavaType) ||
            (introspectedColumn.introspectedTable.primaryKeyColumns.contains(introspectedColumn) && introspectedColumn.isAutoIncrement)

    private fun findMethod(topLevelClass: TopLevelClass, methodName: String): Method? =
        topLevelClass.methods.firstOrNull { it.name == methodName }
}
