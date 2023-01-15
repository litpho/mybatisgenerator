package nl.litpho.mybatis.typehandlers.general;

import static java.util.Objects.requireNonNull;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import nl.litpho.mybatis.enumsupport.DatabaseValueEnum;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

@MappedTypes(DatabaseValueEnum.class)
public class DatabaseValueEnumTypeHandler<E extends Enum<E> & DatabaseValueEnum>
    extends BaseTypeHandler<E> {

  private final Class<E> type;

  public DatabaseValueEnumTypeHandler(final Class<E> type) {
    this.type = requireNonNull(type, "type should not be null");
  }

  @Override
  public void setNonNullParameter(
      final PreparedStatement ps,
      final int parameterIndex,
      final E parameter,
      @Nullable final JdbcType jdbcType)
      throws SQLException {
    ps.setString(parameterIndex, parameter.getDatabaseValue());
  }

  @Override
  @CheckForNull
  public E getNullableResult(final ResultSet rs, final String columnName) throws SQLException {
    return toEnumValue(rs.getString(columnName));
  }

  @Override
  @CheckForNull
  public E getNullableResult(final ResultSet rs, final int columnIndex) throws SQLException {
    return toEnumValue(rs.getString(columnIndex));
  }

  @Override
  @CheckForNull
  public E getNullableResult(final CallableStatement cs, final int columnIndex)
      throws SQLException {
    return toEnumValue(cs.getString(columnIndex));
  }

  // Visible for testing
  @CheckForNull
  E toEnumValue(@CheckForNull final String databaseValue) {
    if (databaseValue == null) {
      return null;
    }

    return Arrays.stream(type.getEnumConstants())
        .filter(value -> value.getDatabaseValue().equals(databaseValue))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    type.getSimpleName() + "." + databaseValue + " does not exist"));
  }
}
