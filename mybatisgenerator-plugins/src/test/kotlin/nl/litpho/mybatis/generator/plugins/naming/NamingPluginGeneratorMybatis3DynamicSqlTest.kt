package nl.litpho.mybatis.generator.plugins.naming

import io.kotest.matchers.collections.containExactlyInAnyOrder
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.should
import nl.litpho.mybatis.generator.test.utils.SqlScriptRunner
import nl.litpho.mybatis.generator.test.utils.connectionProperties
import nl.litpho.mybatis.generator.test.utils.createAndCleanDirectory
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

class NamingPluginGeneratorMybatis3DynamicSqlTest {

    companion object {

        private lateinit var tempDir: File

        private lateinit var warnings: List<String>

        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
            val generatedSourceRoot = "./build/generatedSources/naming/mybatis3dynamicsql"
            tempDir = createAndCleanDirectory(generatedSourceRoot)
            val props = connectionProperties(
                "jdbc:h2:file:${tempDir.absolutePath}/db",
                generatedSourceRoot = generatedSourceRoot
            )
            val resourceRoot = "/nl/litpho/mybatis/generator/plugins/naming/mybatis3dynamicsql"
            SqlScriptRunner(props).run {
                executeScriptFromClasspath("$resourceRoot/tables.sql")
            }

            val warnings: MutableList<String> = mutableListOf()
            val configuration = readConfigurationFromXml(
                "$resourceRoot/generatorConfig.xml",
                warnings,
                props
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
            "ABTest2DynamicSqlSupport.java",
            "ABTest2Mapper.java",
            "DBMyTestDynamicSqlSupport.java",
            "DBMyTestMapper.java",
            "UnrenamedDynamicSqlSupport.java",
            "UnrenamedMapper.java"
        )
    }

    @Test
    fun `the correct model files should be generated`() {
        tempDir.resolve("java/nl/test/model").listFiles()?.map { f -> f.name } should containExactlyInAnyOrder(
            "ABTest2.java",
            "DBMyTest.java",
            "Unrenamed.java"
        )
    }

    @Test
    fun `no resource files should be generated`() {
        tempDir.resolve("resources/nl/test/client").listFiles() should beNull()
    }

    @TestFactory
    fun `all java code should compile`(): Stream<DynamicTest> =
        Stream.of(
            "client/ABTest2DynamicSqlSupport.java",
            "client/ABTest2Mapper.java",
            "client/DBMyTestDynamicSqlSupport.java",
            "client/DBMyTestMapper.java",
            "client/UnrenamedDynamicSqlSupport.java",
            "client/UnrenamedMapper.java",
            "model/ABTest2.java",
            "model/DBMyTest.java",
            "model/Unrenamed.java"
        ).map { fileName ->
            DynamicTest.dynamicTest("$fileName should compile") {
                tempDir.parseResult("java", "nl", "test", fileName)
            }
        }
}
