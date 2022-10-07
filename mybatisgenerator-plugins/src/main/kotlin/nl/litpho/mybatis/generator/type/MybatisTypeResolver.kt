package nl.litpho.mybatis.generator.type

import org.mybatis.generator.api.IntrospectedColumn
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType
import org.mybatis.generator.internal.types.JavaTypeResolverDefaultImpl
import java.sql.Types

private val bigIntegerInstance = FullyQualifiedJavaType("java.math.BigInteger")
private val doubleInstance = FullyQualifiedJavaType("double")
private val floatInstance = FullyQualifiedJavaType("float")
private val integerInstance = FullyQualifiedJavaType("java.lang.Integer")
private val longWrapperInstance = FullyQualifiedJavaType("java.lang.Long")
private val longInstance = FullyQualifiedJavaType("long")
private val uuidInstance = FullyQualifiedJavaType("java.util.UUID")

class MybatisTypeResolver : JavaTypeResolverDefaultImpl() {

    init {
        // Oracle RowId
        typeMap[Types.ROWID] = JdbcTypeInformation("ROWID", FullyQualifiedJavaType.getStringInstance())
    }

    override fun overrideDefaultType(
        column: IntrospectedColumn,
        defaultType: FullyQualifiedJavaType
    ): FullyQualifiedJavaType {
        if (column.jdbcType == Types.BINARY) {
            return uuidInstance
        }

        // Oracle
        if (column.jdbcType == Types.VARBINARY && column.length == 16) {
            return uuidInstance
        }

        // Types.REAL = java.lang.Float, Types.FLOAT = java.lang.Double
        if (!column.isNullable) {
            if (column.jdbcType == Types.FLOAT || column.jdbcType == Types.DOUBLE) {
                return doubleInstance
            }
            if (column.jdbcType == Types.REAL) {
                return floatInstance
            }
        }

        return super.overrideDefaultType(column, defaultType)
    }

    override fun calculateBigDecimalReplacement(
        column: IntrospectedColumn,
        defaultType: FullyQualifiedJavaType
    ): FullyQualifiedJavaType =
        when (column.scale) {
            0 -> when {
                column.length > 9 -> when {
                    column.length > 19 -> bigIntegerInstance
                    else -> if (column.isNullable) longWrapperInstance else longInstance
                }

                else -> if (column.isNullable) integerInstance else FullyQualifiedJavaType.getIntInstance()
            }

            else -> super.calculateBigDecimalReplacement(column, defaultType)
        }
}
