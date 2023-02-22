package nl.litpho.mybatis.generator.db

import java.sql.Connection

class H2MetadataProvider : MetadataProvider {

    override fun getConstraints(conn: Connection, tableName: String): List<ConstraintMetadata> {
        val ps = conn.prepareStatement(
            """
SELECT TCS.CONSTRAINT_TYPE, TCS.CONSTRAINT_NAME, CCE.COLUMN_NAME, TCS.REMARKS, CCE2.TABLE_NAME AS REF_TABLE, CCE2.COLUMN_NAME AS REF_COLUMN
  FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS TCS
  JOIN INFORMATION_SCHEMA.CONSTRAINT_COLUMN_USAGE CCE ON CCE.CONSTRAINT_NAME = TCS.CONSTRAINT_NAME AND TCS.TABLE_NAME = CCE.TABLE_NAME
  LEFT JOIN INFORMATION_SCHEMA.CONSTRAINT_COLUMN_USAGE CCE2 ON CCE2.CONSTRAINT_NAME = TCS.CONSTRAINT_NAME AND TCS.TABLE_NAME != CCE2.TABLE_NAME
 WHERE TCS.TABLE_NAME = ?
            """.trimIndent(),
        )
        ps.setString(1, tableName)
        val rs = ps.executeQuery()
        val result = mutableListOf<ConstraintMetadata>()
        while (rs.next()) {
            val constraintType = rs.getString("CONSTRAINT_TYPE")
            val constraintName = rs.getString("CONSTRAINT_NAME")
            val columnName = rs.getString("COLUMN_NAME")
            val refTableName = rs.getString("REF_TABLE")
            val refColumnName = rs.getString("REF_COLUMN")
            val remarks = rs.getString("REMARKS")

            result.add(ConstraintMetadata(constraintType, constraintName, columnName, remarks, refTableName, refColumnName))
        }

        return result
    }
}
