package nl.litpho.mybatis.generator.db

import java.sql.Connection

class OracleMetadataProvider : MetadataProvider {

    override fun getConstraints(conn: Connection, tableName: String): List<ConstraintMetadata> {
        val ps = conn.prepareStatement(
            """
SELECT UCS.CONSTRAINT_TYPE, UCS.CONSTRAINT_NAME, UCC.COLUMN_NAME, NULL AS REMARKS, UCC2.TABLE_NAME AS REF_TABLE, UCC2.COLUMN_NAME AS REF_COLUMN
  FROM USER_CONSTRAINTS UCS
  JOIN USER_CONS_COLUMNS UCC ON UCC.CONSTRAINT_NAME = UCS.CONSTRAINT_NAME AND UCC.TABLE_NAME = UCS.TABLE_NAME
  LEFT JOIN USER_CONS_COLUMNS UCC2 ON UCC2.CONSTRAINT_NAME = UCS.R_CONSTRAINT_NAME
 WHERE UCS.CONSTRAINT_TYPE != 'C'
   AND UCS.TABLE_NAME = ?
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
