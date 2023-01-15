package nl.litpho.mybatis.idgenerators;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class UUIDGeneratorTest {

  @ParameterizedTest
  @MethodSource("argumentsForTestSupports")
  void testSupports(final Class<?> keyType, final boolean expected) {
    assertThat(new UUIDGenerator().supports(keyType)).isEqualTo(expected);
  }

  private static Stream<Arguments> argumentsForTestSupports() {
    return Stream.of(Arguments.of(UUID.class, true), Arguments.of(String.class, false));
  }

  @Test
  void testNextId() {
    final UUIDGenerator generator = new UUIDGenerator();
    assertThat(generator.nextId()).isNotEqualTo(generator.nextId());
  }
}
