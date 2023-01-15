package nl.litpho.mybatis.idgenerators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class IdGeneratorsTest {

  @ParameterizedTest
  @MethodSource("argumentsForTestSupports")
  void testSupports(final Class<?> keyType, final boolean expected) {
    assertThat(IdGenerators.supports(keyType)).isEqualTo(expected);
  }

  private static Stream<Arguments> argumentsForTestSupports() {
    return Stream.of(
        Arguments.of(UUID.class, true),
        Arguments.of(Long.class, true),
        Arguments.of(String.class, false));
  }

  @ParameterizedTest
  @ValueSource(classes = {UUID.class, Long.class})
  void testGet(final Class<?> keyType) {
    assertThat(IdGenerators.get(keyType)).isNotNull();
  }

  @Test
  void testGetInvalidIdGenerator() {
    assertThatIllegalStateException()
        .isThrownBy(() -> IdGenerators.get(String.class))
        .withMessage("No IdGenerator found for class java.lang.String");
  }
}
