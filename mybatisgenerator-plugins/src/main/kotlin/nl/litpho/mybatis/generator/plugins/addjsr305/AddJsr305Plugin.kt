package nl.litpho.mybatis.generator.plugins.addjsr305

import nl.litpho.mybatis.generator.plugins.util.isGetter
import nl.litpho.mybatis.generator.plugins.util.isSetter
import org.mybatis.generator.api.IntrospectedColumn
import org.mybatis.generator.api.IntrospectedTable
import org.mybatis.generator.api.PluginAdapter
import org.mybatis.generator.api.dom.java.Method
import org.mybatis.generator.api.dom.java.TopLevelClass
import java.util.Locale

class AddJsr305Plugin : PluginAdapter() {

    override fun validate(warnings: MutableList<String>?): Boolean =
        context.targetRuntime in listOf("MyBatis3", "MyBatis3DynamicSql")

    override fun modelBaseRecordClassGenerated(topLevelClass: TopLevelClass, introspectedTable: IntrospectedTable): Boolean {
        with(topLevelClass) {
            addStaticImport("java.util.Objects.requireNonNull")
            addImportedType("javax.annotation.Nullable")
        }
        for (method in topLevelClass.methods) {
            if (!method.isGetter() && !method.isSetter()) {
                continue
            }
            val column = getColumn(introspectedTable, method)
            if (method.isGetter() && column.isNullable) {
                method.addAnnotation("@Nullable")
            }
            if (method.isSetter()) {
                if (column.isNullable) {
                    method.parameters[0].addAnnotation("@Nullable")
                } else {
                    val parameterName = method.parameters[0].name
                    method.addBodyLine(0, "requireNonNull($parameterName, \"$parameterName should not be null\");")
                }
            }
        }

        return super.modelBaseRecordClassGenerated(topLevelClass, introspectedTable)
    }

    private fun getColumn(introspectedTable: IntrospectedTable, method: Method): IntrospectedColumn {
        val javaProperty = getJavaProperty(method)
        return introspectedTable.allColumns
            .firstOrNull { it.javaProperty == javaProperty } ?: throw IllegalStateException("Column not found for method ${method.name}")
    }

    private fun getJavaProperty(method: Method): String {
        val strippedMethodName = method.name.replace("^(get|set|is)".toRegex(), "")
        return strippedMethodName.substring(0, 1).lowercase(Locale.getDefault()) + strippedMethodName.substring(1)
    }
}
