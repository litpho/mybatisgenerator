package nl.litpho.mybatis.typehandlers.oracle;

import java.nio.ByteBuffer;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

@MappedTypes(UUID.class)
public class OracleUUIDTypeHandler extends BaseTypeHandler<UUID> {

  @Override
  public void setNonNullParameter(
      final PreparedStatement ps,
      final int parameterIndex,
      final UUID parameter,
      @Nullable final JdbcType jdbcType)
      throws SQLException {
    ps.setBytes(parameterIndex, toBytes(parameter));
  }

  @Override
  public UUID getNullableResult(final ResultSet rs, final String columnName) throws SQLException {
    return toUUID(rs.getBytes(columnName));
  }

  @Override
  public UUID getNullableResult(final ResultSet rs, final int columnIndex) throws SQLException {
    return toUUID(rs.getBytes(columnIndex));
  }

  @Override
  public UUID getNullableResult(final CallableStatement cs, final int columnIndex)
      throws SQLException {
    return toUUID(cs.getBytes(columnIndex));
  }

  // Visible for testing
  byte[] toBytes(final UUID uuid) {
    final ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
    bb.putLong(uuid.getMostSignificantBits());
    bb.putLong(uuid.getLeastSignificantBits());
    return bb.array();
  }

  // Visible for testing
  @CheckForNull
  UUID toUUID(@CheckForNull final byte[] bytes) {
    if (bytes == null) {
      return null;
    }

    final ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
    final long high = byteBuffer.getLong();
    final long low = byteBuffer.getLong();
    return new UUID(high, low);
  }
}
