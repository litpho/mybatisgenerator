package nl.litpho.mybatis.typehandlers.general;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import nl.litpho.mybatis.enumsupport.DatabaseValueEnum;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class DatabaseValueEnumTypeHandlerTest {

  private final DatabaseValueEnumTypeHandler<Truth> typeHandler =
      new DatabaseValueEnumTypeHandler<>(Truth.class);

  @ParameterizedTest
  @EnumSource(Truth.class)
  void testSetNonNullParameter(final Truth parameter) throws SQLException {
    final PreparedStatement ps = mock();
    final int parameterIndex = 4;
    typeHandler.setNonNullParameter(ps, parameterIndex, parameter, JdbcType.CHAR);
    verify(ps).setString(parameterIndex, parameter.getDatabaseValue());
  }

  @ParameterizedTest
  @EnumSource(Truth.class)
  void testGetNullableResultName(final Truth parameter) throws SQLException {
    final ResultSet rs = mock();
    final String columnName = "ID";
    when(rs.getString(columnName)).thenReturn(parameter.getDatabaseValue());
    assertThat(typeHandler.getNullableResult(rs, columnName)).isEqualTo(parameter);
  }

  @ParameterizedTest
  @EnumSource(Truth.class)
  void testGetNullableResultIndex(final Truth parameter) throws SQLException {
    final ResultSet rs = mock();
    final int columnIndex = 4;
    when(rs.getString(columnIndex)).thenReturn(parameter.getDatabaseValue());
    assertThat(typeHandler.getNullableResult(rs, columnIndex)).isEqualTo(parameter);
  }

  @ParameterizedTest
  @EnumSource(Truth.class)
  void testGetNullableResultCallable(final Truth parameter) throws SQLException {
    final CallableStatement cs = mock();
    final int columnIndex = 4;
    when(cs.getString(columnIndex)).thenReturn(parameter.getDatabaseValue());
    assertThat(typeHandler.getNullableResult(cs, columnIndex)).isEqualTo(parameter);
  }

  @Test
  void testToEnumValueNull() {
    assertThat(typeHandler.toEnumValue(null)).isNull();
  }

  @Test
  void testToEnumValueInvalid() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> typeHandler.toEnumValue("X"))
        .withMessage("Truth.X does not exist");
  }

  private enum Truth implements DatabaseValueEnum {
    TRUE("T"),
    FALSE("F");

    private final String databaseValue;

    Truth(final String databaseValue) {
      this.databaseValue = databaseValue;
    }

    @Override
    public String getDatabaseValue() {
      return databaseValue;
    }
  }
}
