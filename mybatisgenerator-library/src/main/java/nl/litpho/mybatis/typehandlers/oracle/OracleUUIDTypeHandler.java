package nl.litpho.mybatis.typehandlers.oracle;

import java.nio.ByteBuffer;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.apache.ibatis.type.TypeHandler;

@MappedTypes(UUID.class)
public class OracleUUIDTypeHandler implements TypeHandler<UUID> {

  @Override
  public void setParameter(
      PreparedStatement ps, int parameterIndex, UUID parameter, JdbcType jdbcType)
      throws SQLException {
    ps.setBytes(parameterIndex, toBytes(parameter));
  }

  @Override
  public UUID getResult(ResultSet rs, String columnName) throws SQLException {
    final byte[] bytes = rs.getBytes(columnName);
    if (bytes == null) {
      return null;
    }
    return toUUID(bytes);
  }

  @Override
  public UUID getResult(ResultSet rs, int columnIndex) throws SQLException {
    final byte[] bytes = rs.getBytes(columnIndex);
    if (bytes == null) {
      return null;
    }
    return toUUID(bytes);
  }

  @Override
  public UUID getResult(CallableStatement cs, int columnIndex) throws SQLException {
    final byte[] bytes = cs.getBytes(columnIndex);
    if (bytes == null) {
      return null;
    }
    return toUUID(bytes);
  }

  private byte[] toBytes(final UUID uuid) {
    ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
    bb.putLong(uuid.getMostSignificantBits());
    bb.putLong(uuid.getLeastSignificantBits());
    return bb.array();
  }

  private UUID toUUID(final byte[] bytes) {
    ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
    long high = byteBuffer.getLong();
    long low = byteBuffer.getLong();
    return new UUID(high, low);
  }
}
