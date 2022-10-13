package nl.litpho.mybatis.typehandlers.general;

import nl.litpho.mybatis.enumsupport.DatabaseValueEnum;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.apache.ibatis.type.TypeHandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import static java.util.Objects.requireNonNull;

@MappedTypes(DatabaseValueEnum.class)
public class DatabaseValueEnumTypeHandler<E extends Enum<E> & DatabaseValueEnum> implements TypeHandler<E> {

    private final Class<E> type;

    public DatabaseValueEnumTypeHandler(final Class<E> type) {
        this.type = requireNonNull(type, "type should not be null");
    }

    @Override
    public void setParameter(final PreparedStatement ps, final int parameterIndex, final E parameter, final JdbcType jdbcType) throws SQLException {
        ps.setString(parameterIndex, parameter.getDatabaseValue());
    }

    @Override
    public E getResult(final ResultSet rs, final String columnName) throws SQLException {
        return toEnumValue(rs.getString(columnName));
    }

    @Override
    public E getResult(final ResultSet rs, final int columnIndex) throws SQLException {
        return toEnumValue(rs.getString(columnIndex));
    }

    @Override
    public E getResult(final CallableStatement cs, final int columnIndex) throws SQLException {
        return toEnumValue(cs.getString(columnIndex));
    }

    private E toEnumValue(final String databaseValue) {
        return Arrays.stream(type.getEnumConstants())
                .filter(value -> value.getDatabaseValue().equals(databaseValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(type.getSimpleName() + "." + databaseValue + " does not exist"));
    }
}
