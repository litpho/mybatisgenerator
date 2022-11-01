package nl.litpho.mybatis.idgenerators;

import java.util.UUID;

public class UUIDGenerator implements IdGenerator<UUID> {
  @Override
  public boolean supports(Class<?> keyType) {
    return keyType == UUID.class;
  }

  @Override
  public UUID nextId() {
    return UUID.randomUUID();
  }
}
