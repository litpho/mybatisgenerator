package nl.litpho.mybatis.generator.plugins.builder

import nl.litpho.mybatis.generator.plugins.naming.NamingConfiguration
import nl.litpho.mybatis.generator.plugins.skip.SkipConfiguration
import nl.litpho.mybatis.generator.plugins.util.ConfigurationUtil
import nl.litpho.mybatis.generator.plugins.util.PrimitiveUtil.replaceGetterReturnTypeWithPrimitive
import nl.litpho.mybatis.generator.plugins.util.PrimitiveUtil.replaceSetterParameterTypeWithPrimitive
import org.mybatis.generator.api.GeneratedJavaFile
import org.mybatis.generator.api.IntrospectedColumn
import org.mybatis.generator.api.IntrospectedTable
import org.mybatis.generator.api.JavaFormatter
import org.mybatis.generator.api.PluginAdapter
import org.mybatis.generator.api.dom.java.InnerClass
import org.mybatis.generator.api.dom.java.Interface
import org.mybatis.generator.api.dom.java.Method
import org.mybatis.generator.api.dom.java.TopLevelClass
import org.mybatis.generator.api.dom.xml.XmlElement
import org.mybatis.generator.config.PropertyRegistry
import java.util.stream.IntStream

private const val MAKE_CONSTRUCTOR_PRIVATE_PARAMETER_NAME: String = "makeConstructorPrivate"
private const val USE_PRIMITIVES_WHERE_POSSIBLE_PARAMETER_NAME: String = "usePrimitivesWherePossible"
private const val REMOVE_SETTERS_PARAMETER_NAME: String = "removeSetters"
private const val METHOD_PREFIX_PARAMETER_NAME: String = "methodPrefix"
private const val USE_ID_GENERATORS_NAME: String = "useIdGenerators"
private const val DOLLAR_ENUM_DOLLAR: String = "\$enum$"

class BuilderPlugin : PluginAdapter() {

    private var makeConstructorPrivate: Boolean = true
    private var usePrimitivesWherePossible: Boolean = true
    private var removeSetters: Boolean = true
    private lateinit var methodPrefix: String
    private var useIdGenerators: Boolean = true

    private val baseRecordClasses: MutableList<TopLevelClass> = mutableListOf()

    override fun validate(warnings: List<String>): Boolean {
        this.makeConstructorPrivate = properties.getProperty(MAKE_CONSTRUCTOR_PRIVATE_PARAMETER_NAME)?.toBoolean() ?: true
        this.usePrimitivesWherePossible = properties.getProperty(USE_PRIMITIVES_WHERE_POSSIBLE_PARAMETER_NAME)?.toBoolean() ?: true
        this.removeSetters = properties.getProperty(REMOVE_SETTERS_PARAMETER_NAME)?.toBoolean() ?: true
        this.methodPrefix = properties.getProperty(METHOD_PREFIX_PARAMETER_NAME) ?: "with"
        this.useIdGenerators = properties.getProperty(USE_ID_GENERATORS_NAME)?.toBoolean() ?: true

        return context.targetRuntime in listOf("MyBatis3", "MyBatis3DynamicSql")
    }

    override fun modelBaseRecordClassGenerated(topLevelClass: TopLevelClass, introspectedTable: IntrospectedTable): Boolean {
        baseRecordClasses.add(topLevelClass)
        val namingConfiguration: NamingConfiguration? = ConfigurationUtil.getPluginConfigurationNull()
        val factory =
            BaseRecordClassBuilderFactory(topLevelClass, introspectedTable, namingConfiguration)
        val targetRuntime: String = context.targetRuntime
        factory.create(
            makeConstructorPrivate,
            usePrimitivesWherePossible,
            removeSetters,
            methodPrefix,
            useIdGenerators,
            targetRuntime,
        )
        if (usePrimitivesWherePossible) {
            for (introspectedColumn: IntrospectedColumn in introspectedTable.allColumns) {
                replaceGetterReturnTypeWithPrimitive(topLevelClass, introspectedColumn)
                replaceSetterParameterTypeWithPrimitive(topLevelClass, introspectedColumn)
            }
        }

        return super.modelBaseRecordClassGenerated(topLevelClass, introspectedTable)
    }

    override fun modelPrimaryKeyClassGenerated(topLevelClass: TopLevelClass, introspectedTable: IntrospectedTable): Boolean {
        if (context.targetRuntime == "MyBatis3") {
            PrimaryKeyClassBuilderFactory.create(topLevelClass, introspectedTable, usePrimitivesWherePossible)
            if (usePrimitivesWherePossible) {
                for (introspectedColumn: IntrospectedColumn in introspectedTable.primaryKeyColumns) {
                    replaceGetterReturnTypeWithPrimitive(topLevelClass, introspectedColumn)
                    replaceSetterParameterTypeWithPrimitive(topLevelClass, introspectedColumn)
                }
            }
        }

        return super.modelPrimaryKeyClassGenerated(topLevelClass, introspectedTable)
    }

    override fun clientInsertSelectiveMethodGenerated(method: Method, interfaze: Interface, introspectedTable: IntrospectedTable): Boolean = false

    override fun providerInsertSelectiveMethodGenerated(method: Method, topLevelClass: TopLevelClass, introspectedTable: IntrospectedTable): Boolean =
        false

    override fun sqlMapInsertSelectiveElementGenerated(element: XmlElement, introspectedTable: IntrospectedTable): Boolean = false

    override fun clientUpdateByExampleSelectiveMethodGenerated(method: Method, interfaze: Interface, introspectedTable: IntrospectedTable): Boolean =
        replaceMethod(introspectedTable, method)

    override fun clientUpdateByPrimaryKeySelectiveMethodGenerated(
        method: Method,
        interfaze: Interface,
        introspectedTable: IntrospectedTable,
    ): Boolean = replaceMethod(introspectedTable, method)

    override fun sqlMapUpdateByExampleSelectiveElementGenerated(element: XmlElement, introspectedTable: IntrospectedTable): Boolean {
        UpdateSelectionReplacer.replaceUpdateByExampleSelectiveXml(element, introspectedTable)
        return true
    }

    override fun sqlMapUpdateByPrimaryKeySelectiveElementGenerated(element: XmlElement, introspectedTable: IntrospectedTable): Boolean {
        UpdateSelectionReplacer.replaceUpdateByPrimaryKeySelectiveXml(element, introspectedTable)
        return true
    }

    override fun clientUpdateSelectiveColumnsMethodGenerated(method: Method, interfaze: Interface, introspectedTable: IntrospectedTable): Boolean =
        context.targetRuntime == "MyBatis3"

    override fun contextGenerateAdditionalJavaFiles(introspectedTable: IntrospectedTable): List<GeneratedJavaFile> {
        val earlierFiles: MutableList<GeneratedJavaFile>? = super.contextGenerateAdditionalJavaFiles(introspectedTable)
        if (context.targetRuntime != "Mybatis3") {
            return mutableListOf()
        }

        val skipConfiguration = ConfigurationUtil.getPluginConfigurationNull<SkipConfiguration>()
        if (skipConfiguration != null && skipConfiguration.isIgnored(introspectedTable)) {
            return mutableListOf()
        }

//        val domeinEnumPluginConfiguration = getPluginConfigurationNull<DomeinEnumPluginConfiguration>()
//        if (domeinEnumPluginConfiguration != null && domeinEnumPluginConfiguration.isDomeinEnumTable(introspectedTable.aliasedFullyQualifiedTableNameAtRuntime)) {
//            return mutableListOf()
//        }
        val generatedJavaFiles = if (earlierFiles == null) {
            mutableListOf()
        } else {
            mutableListOf<GeneratedJavaFile>().apply { addAll(earlierFiles) }
        }
        val targetProject: String = context.javaModelGeneratorConfiguration.targetProject
        val generator = UpdateSelectionGenerator(targetProject, introspectedTable, context)
        val fileEncoding: String? = context.getProperty(PropertyRegistry.CONTEXT_JAVA_FILE_ENCODING)
        val javaFormatter: JavaFormatter = context.javaFormatter
        generator.compilationUnits
            .map { compilationUnit -> GeneratedJavaFile(compilationUnit, targetProject, fileEncoding, javaFormatter) }
            .forEach { generatedJavaFiles.add(it) }

        return generatedJavaFiles
    }

    override fun contextGenerateAdditionalJavaFiles(): List<GeneratedJavaFile> {
//        val domeinEnumPluginConfiguration = getPluginConfigurationNull<DomeinEnumPluginConfiguration>()
//        domeinEnumPluginConfiguration?.let { enumPluginConfiguration: DomeinEnumPluginConfiguration ->
//            baseRecordClasses
//                .filter { TopLevelClassRenderer().render(it).contains(DOLLAR_ENUM_DOLLAR) }
//                .forEach { replaceEnumValue(it, enumPluginConfiguration) }
//        }
//
//        if (domeinEnumPluginConfiguration != null) {
//            for (baseRecordClass: TopLevelClass in baseRecordClasses) {
//                val builderClass = getBuilderClass(baseRecordClass) ?: continue
//                if (InnerClassRenderer().render(builderClass, baseRecordClass).any { it.contains(DOLLAR_ENUM_DOLLAR) }) {
//                    println("Enum in ${builderClass.type.fullyQualifiedName} is being replaced")
//                    replaceEnumValue(builderClass, domeinEnumPluginConfiguration)
//                }
//            }
//        }

        return super.contextGenerateAdditionalJavaFiles()
    }

    private fun replaceMethod(introspectedTable: IntrospectedTable, method: Method): Boolean =
        if (context.targetRuntime == "Mybatis3") {
            UpdateSelectionReplacer.replaceMethod(method, introspectedTable)
            true
        } else {
            false
        }

//    private fun replaceEnumValue(topLevelClass: TopLevelClass, domeinEnumPluginConfiguration: DomeinEnumPluginConfiguration) {
//        val constructor = getConstructor(topLevelClass)
//        findBodyLineIndexesToReplace(constructor.bodyLines)
//            .forEach { replaceConstructorLine(domeinEnumPluginConfiguration, constructor, it) }
//    }
//
//    private fun replaceEnumValue(builderClass: InnerClass, domeinEnumPluginConfiguration: DomeinEnumPluginConfiguration) {
//        val buildMethod: Method = getBuildMethod(builderClass)
//        findBodyLineIndexesToReplace(buildMethod.bodyLines).forEach { replaceConstructorLine(domeinEnumPluginConfiguration, buildMethod, it) }
//    }

//    private fun replaceConstructorLine(domeinEnumPluginConfiguration: DomeinEnumPluginConfiguration, constructor: Method, index: Int) {
//        with(constructor.bodyLines) {
//            val bodyLine: String = this[index]
//            removeAt(index)
//            add(index, getEnumValue(bodyLine, domeinEnumPluginConfiguration))
//        }
//    }

    private fun getBuilderClass(topLevelClass: TopLevelClass): InnerClass? =
        topLevelClass.innerClasses.firstOrNull { it.type.shortName == "Builder" }

    private fun getConstructor(topLevelClass: TopLevelClass): Method =
        topLevelClass.methods
            .filter(Method::isConstructor)
            .firstOrNull { it.parameters.isNotEmpty() } ?: throw IllegalStateException("No constructor with arguments was found")

    private fun getBuildMethod(builderClass: InnerClass): Method =
        builderClass.methods.firstOrNull { (it.name == "build") } ?: throw IllegalStateException("No build method was found")

    private fun findBodyLineIndexesToReplace(bodyLines: List<String>): IntStream =
        IntStream.range(0, bodyLines.size).filter { i: Int -> bodyLines[i].contains(DOLLAR_ENUM_DOLLAR) }

//    private fun getEnumValue(line: String, domeinEnumPluginConfiguration: DomeinEnumPluginConfiguration): String {
//        val pattern: Pattern = Pattern.compile("(.*)\\\$enum\\$(.*)\\.(.*)\\\$enum\\$;")
//        val matcher: Matcher = pattern.matcher(line)
//        if (!matcher.matches()) {
//            throw IllegalStateException("Line $line heeft niet het juiste formaat")
//        }
//        val replacementValue: String = domeinEnumPluginConfiguration.getDomeinEnumDatabaseValuePairs(matcher.group(2), matcher.group(3))
//        return "${matcher.group(1)}${FullyQualifiedJavaType(matcher.group(2)).shortName}.$replacementValue;"
//    }
}
