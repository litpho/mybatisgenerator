package nl.litpho.mybatis.typehandlers.postgres;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import javax.annotation.Nullable;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

@MappedTypes(UUID.class)
public class PostgresUUIDTypeHandler extends BaseTypeHandler<UUID> {

  @Override
  public void setNonNullParameter(
      final PreparedStatement ps,
      final int parameterIndex,
      final UUID parameter,
      @Nullable final JdbcType jdbcType)
      throws SQLException {
    ps.setObject(parameterIndex, parameter);
  }

  @Override
  public UUID getNullableResult(final ResultSet rs, final String columnName) throws SQLException {
    return rs.getObject(columnName, UUID.class);
  }

  @Override
  public UUID getNullableResult(final ResultSet rs, final int columnIndex) throws SQLException {
    return rs.getObject(columnIndex, UUID.class);
  }

  @Override
  public UUID getNullableResult(final CallableStatement cs, final int columnIndex)
      throws SQLException {
    return cs.getObject(columnIndex, UUID.class);
  }
}
