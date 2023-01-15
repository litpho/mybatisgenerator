package nl.litpho.mybatis.typehandlers.oracle;

import static org.assertj.core.api.Assertions.assertThat;
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

class OracleUUIDTypeHandlerTest {

  private final OracleUUIDTypeHandler typeHandler = new OracleUUIDTypeHandler();

  @Test
  void testSetNonNullParameter() throws SQLException {
    final PreparedStatement ps = mock();
    final int parameterIndex = 4;
    final UUID parameter = UUID.randomUUID();
    typeHandler.setNonNullParameter(ps, parameterIndex, parameter, JdbcType.CHAR);
    verify(ps).setBytes(parameterIndex, typeHandler.toBytes(parameter));
  }

  @Test
  void testGetNullableResultName() throws SQLException {
    final ResultSet rs = mock();
    final String columnName = "ID";
    final UUID parameter = UUID.randomUUID();
    when(rs.getBytes(columnName)).thenReturn(typeHandler.toBytes(parameter));
    assertThat(typeHandler.getNullableResult(rs, columnName)).isEqualTo(parameter);
  }

  @Test
  void testGetNullableResultIndex() throws SQLException {
    final ResultSet rs = mock();
    final int columnIndex = 4;
    final UUID parameter = UUID.randomUUID();
    when(rs.getBytes(columnIndex)).thenReturn(typeHandler.toBytes(parameter));
    assertThat(typeHandler.getNullableResult(rs, columnIndex)).isEqualTo(parameter);
  }

  @Test
  void testGetNullableResultCallable() throws SQLException {
    final CallableStatement rs = mock();
    final int columnIndex = 4;
    final UUID parameter = UUID.randomUUID();
    when(rs.getBytes(columnIndex)).thenReturn(typeHandler.toBytes(parameter));
    assertThat(typeHandler.getNullableResult(rs, columnIndex)).isEqualTo(parameter);
  }

  @Test
  void testToUUID() {
    assertThat(typeHandler.toUUID(null)).isNull();
  }
}
