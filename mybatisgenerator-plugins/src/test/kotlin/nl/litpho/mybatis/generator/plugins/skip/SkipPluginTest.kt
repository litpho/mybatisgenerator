package nl.litpho.mybatis.generator.plugins.skip

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SkipPluginTest {

    @Test
    fun `validate should throw a RuntimeException when the configuration property is missing`() {
        shouldThrow<RuntimeException> {
            SkipPlugin().validate(mutableListOf())
        }.message shouldBe "Invalid configuration location for SkipPlugin"
    }
}
