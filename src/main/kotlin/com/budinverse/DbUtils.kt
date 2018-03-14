package com.budinverse

import com.google.gson.Gson
import java.io.FileReader
import java.sql.*


typealias Statement = String

private class DbConfig(var databaseName: String = "",
             var databaseUser: String = "",
             var databasePassword: String = "")

fun setConfigFile(cfgFile:String = "Config.json"){ configFile = cfgFile }

private var configFile:String = "Config.json"
private val dbConfig: DbConfig =
        Gson().fromJson<DbConfig>(FileReader(configFile), DbConfig::class.java)

private fun getDbConnection(): Connection? {
    val jdbcDriver = "com.mysql.cj.jdbc.Driver"
    val dbUrl= "jdbc:mysql://localhost/" +
            "${dbConfig.databaseName}?" +
            "useLegacyDatetimeCode=false&serverTimezone=UTC" +
            "&useSSL=false"

    return try {
        Class.forName(jdbcDriver)
        DriverManager.getConnection(dbUrl, dbConfig.databaseUser, dbConfig.databasePassword)
    }catch (e:Exception){
        e.printStackTrace()
        null
    }
}

/**
 * Generates PreparedStatement and get database connection
 * given a String.
 * @return null     if empty String or error getting database connection
 */
private fun Statement.genPreparedStatement(): PreparedStatement? {
    if(this.isEmpty()) return null
    val conn = try { getDbConnection() } catch (e: SQLException) { e.printStackTrace(); null}
    return conn?.prepareStatement(this)
}

/**
 * Queries the database given params, which closes all connection after operations are done
 * @param statement     SQL Statement
 * @param blockOne      A function which takes in PreparedStatement
 * @param blockTwo      A function which takes in ResultSet
 * @return T            : Whatever stuff done to ResultSet, eg. Making a User Object from queried results
 */
fun <T> query(statement: Statement, blockOne:(PreparedStatement) -> Unit, blockTwo: (ResultSet) -> T):T? {
    val ps = statement.genPreparedStatement() ?: return null
    blockOne(ps)
    val rs = ps.executeQuery()
    return if(rs.next()) {
        val temp = blockTwo(rs)
        closeAll(ps,rs,ps.connection)
        temp
    } else null
}

/**
 * Queries the database given params, which closes all connection after operations are done
 * @param statement     SQL Statement
 * @param blockOne      A function which takes in PreparedStatement
 * @param blockTwo      A function which takes in ResultSet
 * @return T            : Whatever stuff done to ResultSet, eg. Making a User Object from queried results
 *                      and returns arrayListOf<T>
 */
fun <T> queryMulti(statement: Statement, blockOne:(PreparedStatement) -> Unit, blockTwo: (ResultSet) -> T):ArrayList<T> {
    val arList = arrayListOf<T>()
    val ps = statement.genPreparedStatement() ?: return arList
    blockOne(ps)
    val rs = ps.executeQuery()

    while(rs.next()) { arList.add(blockTwo(rs)) }
    closeAll(ps,rs,ps.connection)

    return arList
}

/**
 * Inserts into the database given params, which closes all connection after operations are done
 * Statement can be CREATE,UPDATE,DELETE
 * @param statement     SQL Statement
 * @param block         A function which takes in PreparedStatement
 * @return Int          Number of rows added
 */
fun manipulate(statement: Statement, block: (PreparedStatement) -> Unit): Int {
    val ps = statement.genPreparedStatement() ?: return 0
    block(ps)
    return try {
        val rs = ps.executeUpdate()
        closeAll(ps, null, ps.connection)

        rs
    } catch (e: Exception) {
        e.printStackTrace()
        0
    }
}

/**
 * Closes all database connection
 * Should only be called after operations done
 * @param ps        PreparedStatement
 * @param rs        ResultSet
 * @param conn      Connection
 */
private fun closeAll(ps: PreparedStatement, rs: ResultSet?, conn: Connection){
    ps.close()
    rs?.close()
    conn.close()
}