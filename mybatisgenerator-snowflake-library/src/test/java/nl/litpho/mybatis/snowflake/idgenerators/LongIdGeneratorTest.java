package nl.litpho.mybatis.snowflake.idgenerators;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import xyz.downgoon.snowflake.Snowflake;

class LongIdGeneratorTest {

  @ParameterizedTest
  @MethodSource("argumentsForTestSupports")
  void testSupports(final Class<?> keyType, final boolean expected) {
    assertThat(new LongIdGenerator().supports(keyType)).isEqualTo(expected);
  }

  private static Stream<Arguments> argumentsForTestSupports() {
    return Stream.of(Arguments.of(Long.class, true), Arguments.of(String.class, false));
  }

  @Test
  void testNextId() {
    final long nextId = 42L;
    final Snowflake snowflake = mock();
    final LongIdGenerator generator = new LongIdGenerator(snowflake);
    when(snowflake.nextId()).thenReturn(nextId);
    assertThat(generator.nextId()).isEqualTo(nextId);
  }
}
