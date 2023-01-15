package nl.litpho.mybatis.snowflake.idgenerators;

import nl.litpho.mybatis.idgenerators.IdGenerator;
import xyz.downgoon.snowflake.Snowflake;

public class LongIdGenerator implements IdGenerator<Long> {

  private static final String DATACENTER_ID = "SNOWFLAKE_DATACENTER_ID";
  private static final String WORKER_ID = "SNOWFLAKE_WORKER_ID";
  private final Snowflake snowflake;

  public LongIdGenerator() {
    final long datacenterId = Long.parseLong(System.getProperty(DATACENTER_ID, "5"));
    final long workerId = Long.parseLong(System.getProperty(WORKER_ID, "13"));
    this.snowflake = new Snowflake(datacenterId, workerId);
  }

  // Visible for testing
  LongIdGenerator(final Snowflake snowflake) {
    this.snowflake = snowflake;
  }

  @Override
  public boolean supports(final Class<?> keyType) {
    return keyType == Long.class;
  }

  @Override
  public Long nextId() {
    return snowflake.nextId();
  }
}
