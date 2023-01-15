package nl.litpho.mybatis.typehandlers.postgres;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;

class PostgresUUIDTypeHandlerTest {

  final PostgresUUIDTypeHandler typeHandler = new PostgresUUIDTypeHandler();

  @Test
  void testSetNonNullParameter() throws SQLException {
    final PreparedStatement ps = mock();
    final int parameterIndex = 4;
    final UUID parameter = UUID.randomUUID();
    typeHandler.setNonNullParameter(ps, parameterIndex, parameter, JdbcType.CHAR);
    verify(ps).setObject(parameterIndex, parameter);
  }

  @Test
  void testGetNullableResultName() throws SQLException {
    final ResultSet rs = mock();
    final String columnName = "ID";
    final UUID parameter = UUID.randomUUID();
    when(rs.getObject(columnName, UUID.class)).thenReturn(parameter);
    assertThat(typeHandler.getNullableResult(rs, columnName)).isEqualTo(parameter);
  }

  @Test
  void testGetNullableResult() throws SQLException {
    final ResultSet rs = mock();
    final int columnIndex = 4;
    final UUID parameter = UUID.randomUUID();
    when(rs.getObject(columnIndex, UUID.class)).thenReturn(parameter);
    assertThat(typeHandler.getNullableResult(rs, columnIndex)).isEqualTo(parameter);
  }

  @Test
  void testGetNullableResult1() throws SQLException {
    final CallableStatement cs = mock();
    final int columnIndex = 4;
    final UUID parameter = UUID.randomUUID();
    when(cs.getObject(columnIndex, UUID.class)).thenReturn(parameter);
    assertThat(typeHandler.getNullableResult(cs, columnIndex)).isEqualTo(parameter);
  }
}
