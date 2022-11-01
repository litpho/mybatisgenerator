package nl.litpho.mybatis.typehandlers.postgres;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.apache.ibatis.type.TypeHandler;

@MappedTypes(UUID.class)
public class PostgresUUIDTypeHandler implements TypeHandler<UUID> {

  @Override
  public void setParameter(
      PreparedStatement ps, int parameterIndex, UUID parameter, JdbcType jdbcType)
      throws SQLException {
    ps.setObject(parameterIndex, parameter);
  }

  @Override
  public UUID getResult(ResultSet rs, String columnName) throws SQLException {
    return rs.getObject(columnName, UUID.class);
  }

  @Override
  public UUID getResult(ResultSet rs, int columnIndex) throws SQLException {
    return rs.getObject(columnIndex, UUID.class);
  }

  @Override
  public UUID getResult(CallableStatement cs, int columnIndex) throws SQLException {
    return cs.getObject(columnIndex, UUID.class);
  }
}
