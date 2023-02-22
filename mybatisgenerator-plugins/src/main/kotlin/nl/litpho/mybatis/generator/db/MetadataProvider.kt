package nl.litpho.mybatis.generator.db

import java.sql.Connection

interface MetadataProvider {
    fun getConstraints(conn: Connection, tableName: String): List<ConstraintMetadata>

    companion object {
        fun getMetadataProvider(conn: Connection): MetadataProvider? =
            when (conn.metaData.databaseProductName) {
                "H2" -> H2MetadataProvider()
                "Oracle" -> OracleMetadataProvider()
                else -> null
            }
    }
}

data class ConstraintMetadata(
    val constraintType: String,
    val constraintName: String,
    val columnName: String,
    val remarks: String?,
    val refTableName: String?,
    val refColumnName: String?,
)
