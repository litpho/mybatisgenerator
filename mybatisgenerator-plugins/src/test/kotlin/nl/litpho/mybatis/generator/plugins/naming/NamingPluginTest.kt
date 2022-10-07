package nl.litpho.mybatis.generator.plugins.naming

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class NamingPluginTest {

    @Test
    fun `validate should throw a RuntimeException when the configuration property is missing`() {
        shouldThrow<RuntimeException> {
            NamingPlugin().validate(mutableListOf())
        }.message shouldBe "Invalid configuration location for NamingPlugin"
    }
}
