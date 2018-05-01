package com.budinverse.utils

import java.io.FileInputStream
import java.sql.*
import java.util.*

private typealias Statement = String

private class DbConfig(val databaseUser: String,
                        val databasePassword: String,
                        val databaseUrl: String,
                        val driver: String)

/**
 * Sets the config file to import which will then be
 * used to make all the connections to the database.
 * This should be called only once
 * @param cfgFile       a properties file
 */
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
private lateinit var dbConfig: DbConfig

private var noOfTimesCalled = 0

/**
 * Gets the database connection based on the config file provided
 * @return Connection?
 */
fun getDbConnection(): Connection? {
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
fun Statement.genPreparedStatementFromStatement(): PreparedStatement? {
    if (this.isEmpty()) return null
    val conn = try {
        getDbConnection()
    } catch (e: SQLException) {
        e.printStackTrace(); null
    }
    return conn?.prepareStatement(this)
}

inline fun transaction(block: () -> PreparedStatement): Unit? {
    val dbConnection = getDbConnection() ?: return null
    return try {
        dbConnection.autoCommit = false
        val ps = block()
        dbConnection.commit()
        closeAll(ps, null, dbConnection)
    } catch (e: SQLException) {
        dbConnection.rollback()
        null
    }
}

inline fun manipulateTxn(statement: Statement, block: (PreparedStatement) -> Unit): PreparedStatement? {
    val ps = statement.genPreparedStatementFromStatement() ?: return null
    block(ps)
    ps.executeUpdate()
    return ps
}

/**
 * Queries the database given params, which closes all connection after operations are done
 * @param statement     SQL Statement
 * @param blockOne      A function which takes in PreparedStatement
 * @param blockTwo      A function which takes in ResultSet
 * @return T            : Whatever stuff done to ResultSet, eg. Making a User Object from queried results
 */
inline fun <T> query(statement: Statement, blockOne: (PreparedStatement) -> Unit, blockTwo: (ResultSet) -> T): T? {
    val ps = statement.genPreparedStatementFromStatement() ?: return null
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
inline fun <T> queryMulti(statement: Statement, blockOne: (PreparedStatement) -> Unit, blockTwo: (ResultSet) -> T): ArrayList<T> {
    val arList = arrayListOf<T>()
    val ps = statement.genPreparedStatementFromStatement() ?: return arList
    blockOne(ps)
    val rs = ps.executeQuery()

    while (rs.next()) {
        arList.add(blockTwo(rs))
    }
    closeAll(ps, rs, ps.connection)

    return arList
}

/**
 * Queries the database given params, which closes all connection after operations are done
 * @param statement     SQL Statement
 * @param blockOne      A function which takes in ResultSet
 * @return T            : Whatever stuff done to ResultSet, eg. Making a User Object from queried results
 *                      and returns arrayListOf<T>
 */
inline fun <T> queryMulti(statement: Statement, blockOne: (ResultSet) -> T): ArrayList<T> {
    val arList = arrayListOf<T>()
    val ps = statement.genPreparedStatementFromStatement() ?: return arList
    val rs = ps.executeQuery()

    while (rs.next()) {
        arList.add(blockOne(rs))
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
inline fun manipulate(statement: Statement, block: (PreparedStatement) -> Unit): Int {
    val ps = statement.genPreparedStatementFromStatement() ?: return 0
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
fun closeAll(ps: PreparedStatement, rs: ResultSet?, conn: Connection) {
    ps.close()
    rs?.close()
    conn.close()
}

operator fun <T> ResultSet.get(index: Int): T = this.getObject(index) as T
operator fun <T> PreparedStatement.set(index: Int, data: T): Unit = this.setObject(index, data)
