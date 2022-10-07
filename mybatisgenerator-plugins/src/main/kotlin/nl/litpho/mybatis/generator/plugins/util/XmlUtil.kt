package nl.litpho.mybatis.generator.plugins.util

import org.mybatis.generator.api.dom.xml.Attribute
import org.mybatis.generator.api.dom.xml.TextElement
import org.mybatis.generator.api.dom.xml.VisitableElement
import org.mybatis.generator.api.dom.xml.XmlElement
import org.mybatis.generator.api.dom.xml.render.ElementRenderer
import java.util.stream.Collectors

object XmlUtil {

    fun findElement(rootElement: XmlElement, name: String): XmlElement =
        findElements(rootElement, name).firstOrNull() ?: throw IllegalStateException("No xmlElement found with name $name")

    fun findElements(rootElement: XmlElement, name: String): List<XmlElement> {
        return rootElement.elements
            .filterIsInstance<XmlElement>()
            .filter { it.name == name }
    }

    fun findElementsByAttribute(rootElement: XmlElement, name: String): List<XmlElement> =
        rootElement.elements
            .filterIsInstance<XmlElement>()
            .filter { e -> e.attributes.any { a -> a.name == name } }

    fun findTextElement(rootElement: XmlElement): TextElement =
        findTextElements(rootElement).firstOrNull() ?: throw IllegalStateException("No textElement found")

    fun findTextElements(rootElement: XmlElement): List<TextElement> =
        rootElement.elements.filterIsInstance<TextElement>()

    fun findAttribute(element: XmlElement, name: String): Attribute =
        element.attributes.firstOrNull { it.name == name } ?: throw IllegalStateException("No $name attribute found")

    fun replace(rootElement: XmlElement, original: VisitableElement, replacement: VisitableElement) {
        val idx = findIndexOfElement(rootElement, original)
        rootElement.elements.remove(original)
        rootElement.addElement(idx, replacement)
    }

    fun findIndexOfElement(rootElement: XmlElement, element: VisitableElement): Int {
        val idx = rootElement.elements.indexOf(element)
        if (idx > 0) {
            return idx
        }

        val elementRenderer = ElementRenderer()
        throw IllegalStateException(
            "Element ${element.accept(elementRenderer).collect(Collectors.joining())} not found on ${
            rootElement.accept(elementRenderer).collect(Collectors.joining())}"
        )
    }
}
