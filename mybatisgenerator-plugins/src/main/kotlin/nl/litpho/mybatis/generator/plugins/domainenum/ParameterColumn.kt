package nl.litpho.mybatis.generator.plugins.domainenum

import org.mybatis.generator.api.IntrospectedColumn
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType

private val BOOLEAN_INSTANCE: FullyQualifiedJavaType = FullyQualifiedJavaType("java.lang.Boolean")

class ParameterColumn(introspectedColumn: IntrospectedColumn) {

    val actualColumnName: String = introspectedColumn.actualColumnName

    val javaProperty: String = introspectedColumn.javaProperty

    val isNullable: Boolean = introspectedColumn.isNullable

    val fullyQualifiedJavaType: FullyQualifiedJavaType =
        if (introspectedColumn.isNullable) introspectedColumn.fullyQualifiedJavaType else getPrimitiveIfPossible(introspectedColumn)

    fun isString(): Boolean = fullyQualifiedJavaType == FullyQualifiedJavaType.getStringInstance()

    fun isBoolean(): Boolean =
        fullyQualifiedJavaType in listOf(FullyQualifiedJavaType.getBooleanPrimitiveInstance(), BOOLEAN_INSTANCE)

    private fun getPrimitiveIfPossible(introspectedColumn: IntrospectedColumn): FullyQualifiedJavaType =
        when (introspectedColumn.fullyQualifiedJavaType.fullyQualifiedName) {
            "java.lang.Boolean" -> FullyQualifiedJavaType.getBooleanPrimitiveInstance()
            "java.lang.Integer" -> FullyQualifiedJavaType.getIntInstance()
            "java.lang.Long" -> FullyQualifiedJavaType("long")
            "java.lang.Short" -> FullyQualifiedJavaType("short")
            else -> introspectedColumn.fullyQualifiedJavaType
        }
}
