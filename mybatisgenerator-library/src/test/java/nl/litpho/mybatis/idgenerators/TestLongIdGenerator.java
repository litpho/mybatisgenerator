package nl.litpho.mybatis.idgenerators;

public class TestLongIdGenerator implements IdGenerator<Long> {

  @Override
  public boolean supports(Class<?> keyType) {
    return keyType == Long.class;
  }

  @Override
  public Long nextId() {
    return 42L;
  }
}
