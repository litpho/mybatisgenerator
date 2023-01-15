package nl.litpho.mybatis.generator.plugins.addjsr305

import com.github.javaparser.ast.body.MethodDeclaration
import io.kotest.inspectors.shouldForAll
import io.kotest.inspectors.shouldForNone
import io.kotest.matchers.collections.containExactlyInAnyOrder
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.should
import nl.litpho.mybatis.generator.test.utils.SqlScriptRunner
import nl.litpho.mybatis.generator.test.utils.connectionProperties
import nl.litpho.mybatis.generator.test.utils.createAndCleanDirectory
import nl.litpho.mybatis.generator.test.utils.getType
import nl.litpho.mybatis.generator.test.utils.parseResult
import nl.litpho.mybatis.generator.test.utils.readConfigurationFromXml
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.fail
import org.mybatis.generator.api.MyBatisGenerator
import org.mybatis.generator.api.VerboseProgressCallback
import org.mybatis.generator.internal.DefaultShellCallback
import java.io.File
import java.util.stream.Stream

class AddJsr305PluginGeneratorMybatis3Test {

    companion object {

        private lateinit var tempDir: File

        private lateinit var warnings: List<String>

        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
            val generatedSourceRoot = "./build/generatedSources/addjsr305/mybatis3"
            tempDir = createAndCleanDirectory(generatedSourceRoot)
            val props = connectionProperties(
                "jdbc:h2:file:${tempDir.absolutePath}/db",
                generatedSourceRoot = generatedSourceRoot,
            )
            val resourceRoot = "/nl/litpho/mybatis/generator/plugins/addjsr305/mybatis3"
            SqlScriptRunner(props).run {
                executeScriptFromClasspath("$resourceRoot/tables.sql")
            }

            val warnings: MutableList<String> = mutableListOf()
            val configuration = readConfigurationFromXml(
                "$resourceRoot/generatorConfig.xml",
                warnings,
                props,
            )

            val generator = MyBatisGenerator(configuration, DefaultShellCallback(true), warnings)
            generator.generate(VerboseProgressCallback())

            this.warnings = warnings
        }
    }

    @Test
    fun `generate should not have warnings`() {
        if (warnings.isNotEmpty()) {
            fail(warnings.joinToString("\n"))
        }
    }

    @Test
    fun `the correct client files should be generated`() {
        tempDir.resolve("java/nl/test/client").listFiles()?.map { f -> f.name } should containExactlyInAnyOrder(
            "TestMapper.java",
        )
    }

    @Test
    fun `the correct model files should be generated`() {
        tempDir.resolve("java/nl/test/model").listFiles()?.map { f -> f.name } should containExactlyInAnyOrder(
            "Test.java",
            "TestExample.java",
        )
    }

    @Test
    fun `the correct resource files should be generated`() {
        tempDir.resolve("resources/nl/test/client").listFiles()?.map { f -> f.name } should containExactlyInAnyOrder(
            "TestMapper.xml",
        )
    }

    @TestFactory
    fun `all java code should compile`(): Stream<DynamicTest> =
        Stream.of(
            "client/TestMapper.java",
            "model/Test.java",
            "model/TestExample.java",
        ).map { fileName ->
            DynamicTest.dynamicTest("$fileName should compile") {
                tempDir.parseResult("java", "nl", "test", fileName)
            }
        }

    @Test
    fun `getters should have jsr305 annotations`() {
        val getterNames = listOf("getName", "getContent")
        val parseResult = tempDir.parseResult("java/nl/test/model/Test.java")
        val typeDeclaration = parseResult.getType(0)
        typeDeclaration.findAll(MethodDeclaration::class.java) { m -> m.nameAsString !in getterNames }
            .shouldForNone { containsNullable(it) }
        typeDeclaration.findAll(MethodDeclaration::class.java) { m -> m.nameAsString in getterNames }
            .shouldForAll { containsNullable(it) }
    }

    private fun containsNullable(m: MethodDeclaration) {
        m.annotations.map { a -> a.nameAsString }.shouldContain("Nullable")
    }
}
