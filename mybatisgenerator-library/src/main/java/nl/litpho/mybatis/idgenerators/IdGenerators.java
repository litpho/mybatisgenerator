package nl.litpho.mybatis.idgenerators;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class IdGenerators {

  private static final List<IdGenerator<?>> ID_GENERATORS = new ArrayList<>();

  static {
    for (final IdGenerator<?> idGenerator : ServiceLoader.load(IdGenerator.class)) {
      ID_GENERATORS.add(idGenerator);
    }
    ID_GENERATORS.add(new UUIDGenerator());
  }

  private IdGenerators() {
    // no public constructor for utility class
  }

  public static boolean supports(final Class<?> keyType) {
    return ID_GENERATORS.stream().anyMatch((ig) -> ig.supports(keyType));
  }

  @SuppressWarnings("unchecked")
  public static <T> T get(final Class<T> keyType) {
    return ID_GENERATORS.stream()
        .filter((ig) -> ig.supports(keyType))
        .findFirst()
        .map((ig) -> (IdGenerator<T>) ig)
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "No IdGenerator found for class " + keyType.getCanonicalName()))
        .nextId();
  }
}
