package nl.litpho.mybatis.generator.test.utils

import java.sql.Connection
import java.sql.DriverManager
import java.util.Properties

class SqlScriptRunner(
    private val driverClass: String,
    private val jdbcUrl: String,
    private val username: String,
    private val password: String,
) {

    constructor(props: Properties) : this(
        props.getProperty("driverClassName"),
        props.getProperty("jdbcUrl"),
        props.getProperty("username"),
        props.getProperty("password"),
    )

    fun executeScriptFromClasspath(location: String) {
        val script =
            SqlScriptRunner::class.java.getResourceAsStream(location)?.bufferedReader()?.readText() ?: "Resource $location could not be found"
        executeScript(script)
    }

    private fun executeScript(script: String) {
        connection().use { conn ->
            for (sql in script.toSqlStatements()) {
                conn.createStatement().use { statement ->
                    statement.execute(sql)
                }
            }
        }
    }

    private fun connection(): Connection {
        Class.forName(driverClass)
        return DriverManager.getConnection(jdbcUrl, username, password)
    }

    private fun String.toSqlStatements(): List<String> {
        val list: MutableList<String> = mutableListOf()
        val sb = StringBuilder()
        lines()
            .filter { it.isNotBlank() }
            .filter { !it.startsWith("--") }
            .forEach {
                if (it.endsWith(";")) {
                    sb.append(" ${it.substring(0, it.length - 1)}")
                    list.add(sb.toString())
                    sb.clear()
                } else {
                    sb.append(" $it")
                }
            }

        return list.toList()
    }
}
