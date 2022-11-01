package nl.litpho.mybatis.idgenerators;

import xyz.downgoon.snowflake.Snowflake;

public class LongIdGenerator implements IdGenerator<Long> {

  private final Snowflake SNOWFLAKE = new Snowflake(5, 13);

  @Override
  public boolean supports(Class<?> keyType) {
    return keyType == Long.class;
  }

  @Override
  public Long nextId() {
    return SNOWFLAKE.nextId();
  }
}
