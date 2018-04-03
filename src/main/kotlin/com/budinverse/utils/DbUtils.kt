package com.budinverse.utils

import java.io.FileInputStream
import java.sql.*
import java.util.*


private typealias Statement = String

class DbConfig(var databaseUser: String,
               var databasePassword: String,
               var databaseUrl: String,
               var driver: String)


fun setConfigFile(cfgFile: String = "dbConfig.properties") {
    require(noOfTimesCalled == 0) { "Config already set once!" }
    configFile = cfgFile
    noOfTimesCalled++

    val prop = Properties()
    val input = FileInputStream(configFile)

    prop.load(input)

    /* create config from input */
    dbConfig = DbConfig(databaseUser = prop.getProperty("databaseUser"),
            databasePassword = prop.getProperty("databasePassword"),
            databaseUrl = prop.getProperty("databaseUrl"),
            driver = prop.getProperty("driver"))
}

private lateinit var configFile: String
lateinit var dbConfig: DbConfig

private var noOfTimesCalled = 0

private fun getDbConnection(): Connection? {
    val jdbcDriver = dbConfig.driver
    val dbUrl = dbConfig.databaseUrl

    return try {
        Class.forName(jdbcDriver)
        DriverManager.getConnection(dbUrl, dbConfig.databaseUser, dbConfig.databasePassword)
    } catch (e: Exception) {
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
    if (this.isEmpty()) return null
    val conn = try {
        getDbConnection()
    } catch (e: SQLException) {
        e.printStackTrace(); null
    }
    return conn?.prepareStatement(this)
}

/**
 * Queries the database given params, which closes all connection after operations are done
 * @param statement     SQL Statement
 * @param blockOne      A function which takes in PreparedStatement
 * @param blockTwo      A function which takes in ResultSet
 * @return T            : Whatever stuff done to ResultSet, eg. Making a User Object from queried results
 */
fun <T> query(statement: Statement, blockOne: (PreparedStatement) -> Unit, blockTwo: (ResultSet) -> T): T? {
    val ps = statement.genPreparedStatement() ?: return null
    blockOne(ps)
    val rs = ps.executeQuery()
    return if (rs.next()) {
        val temp = blockTwo(rs)
        closeAll(ps, rs, ps.connection)
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
fun <T> queryMulti(statement: Statement, blockOne: (PreparedStatement) -> Unit, blockTwo: (ResultSet) -> T): ArrayList<T> {
    val arList = arrayListOf<T>()
    val ps = statement.genPreparedStatement() ?: return arList
    blockOne(ps)
    val rs = ps.executeQuery()

    while (rs.next()) {
        arList.add(blockTwo(rs))
    }
    closeAll(ps, rs, ps.connection)

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
private fun closeAll(ps: PreparedStatement, rs: ResultSet?, conn: Connection) {
    ps.close()
    rs?.close()
    conn.close()
}