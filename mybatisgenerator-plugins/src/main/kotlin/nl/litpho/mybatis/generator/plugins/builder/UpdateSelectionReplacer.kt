package nl.litpho.mybatis.generator.plugins.builder

import nl.litpho.mybatis.generator.plugins.util.XmlUtil.findAttribute
import nl.litpho.mybatis.generator.plugins.util.XmlUtil.findElement
import nl.litpho.mybatis.generator.plugins.util.XmlUtil.findElements
import nl.litpho.mybatis.generator.plugins.util.capitalize
import org.mybatis.generator.api.IntrospectedTable
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType
import org.mybatis.generator.api.dom.java.Method
import org.mybatis.generator.api.dom.java.Parameter
import org.mybatis.generator.api.dom.xml.Attribute
import org.mybatis.generator.api.dom.xml.XmlElement
import java.util.regex.Pattern

private val UPDATE_BY_EXAMPLE_SELECTIVE_PATTERN = Pattern.compile("record\\.(.*) != null")

private val UPDATE_BY_PRIMARY_KEY_SELECTIVE_PATTERN = Pattern.compile("(.*) != null")

object UpdateSelectionReplacer {

    fun replaceMethod(method: Method, introspectedTable: IntrospectedTable) {
        val oldParameter = method.parameters[0]
        val updateSelectionType = FullyQualifiedJavaType("${introspectedTable.baseRecordType}UpdateSelection")
        val newParameter = Parameter(updateSelectionType, oldParameter.name)
        newParameter.annotations.addAll(oldParameter.annotations)
        method.parameters.remove(oldParameter)
        method.parameters.add(0, newParameter)
    }

    fun replaceUpdateByExampleSelectiveXml(rootElement: XmlElement, introspectedTable: IntrospectedTable) {
        replaceXml(rootElement, introspectedTable, UPDATE_BY_EXAMPLE_SELECTIVE_PATTERN, "record.update")
    }

    fun replaceUpdateByPrimaryKeySelectiveXml(rootElement: XmlElement, introspectedTable: IntrospectedTable) {
        val parameterTypeAttribute = findAttribute(rootElement, "parameterType")
        rootElement.attributes.remove(parameterTypeAttribute)
        rootElement.addAttribute(Attribute("parameterType", "${introspectedTable.baseRecordType}UpdateSelection"))
        replaceXml(rootElement, introspectedTable, UPDATE_BY_PRIMARY_KEY_SELECTIVE_PATTERN, "update")
    }

    private fun replaceXml(
        rootElement: XmlElement,
        introspectedTable: IntrospectedTable,
        pattern: Pattern,
        prefix: String
    ) {
        val primaryKeyProperties = getPrimaryKeyProperties(introspectedTable)
        val setElement = findElement(rootElement, "set")
        val elementsToRemove: MutableSet<XmlElement> = mutableSetOf()
        for (ifElement in findElements(setElement, "if")) {
            val testAttribute = findAttribute(ifElement, "test")
            val matcher = pattern.matcher(testAttribute.value)
            if (matcher.matches()) {
                val property = matcher.group(1)
                if (primaryKeyProperties.contains(property)) {
                    elementsToRemove.add(ifElement)
                } else {
                    ifElement.attributes.remove(testAttribute)
                    ifElement.addAttribute(Attribute("test", prefix + property.capitalize()))
                }
            }
        }
        setElement.elements.removeAll(elementsToRemove)
    }

    private fun getPrimaryKeyProperties(introspectedTable: IntrospectedTable): List<String> =
        introspectedTable.primaryKeyColumns.map { it.javaProperty }.toList()
}
