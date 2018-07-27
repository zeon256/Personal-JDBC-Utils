package com.budinverse.utils.dewey

import com.budinverse.utils.annotations.SameAsDB
import com.budinverse.utils.getDbConnection
import com.budinverse.utils.set
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import kotlin.reflect.full.declaredMemberProperties

fun transaction(block: TransactionBuilder.() -> Unit): Boolean {
    TransactionBuilder(block = block).run {
        return try {
            block()
            finalize()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            connection.rollback()
            false
        }
    }
}

class TransactionBuilder
internal constructor(
        val connection: Connection = getDbConnection() ?: throw NullPointerException("hahaha"),
        val block: TransactionBuilder.() -> Unit) {

    private val pss: MutableList<PreparedStatement> = mutableListOf()

    init {
        connection.autoCommit = false
    }

    fun exec(statement: String) = connection.prepareStatement(statement).executeUpdate()

    fun exec(statement: String, psValues: Array<Any?> = arrayOf()) {
        val ps = connection.prepareStatement(statement)
        pss += ps
        require(ps.parameterMetaData.parameterCount == psValues.size)
        for (i in 1..psValues.size) {
            ps[i] = psValues[i - 1]
        }
        ps.executeUpdate()
    }

    fun execWithKeys(statement: String, psValues: Array<Any?> = arrayOf()): ResultSet? {
        val ps = connection.prepareStatement(statement, Statement.RETURN_GENERATED_KEYS)
        pss += ps
        require(ps.parameterMetaData.parameterCount == psValues.size)
        for (i in 1..psValues.size) {
            ps[i] = psValues[i - 1]
        }
        ps.executeUpdate()
        val rs = ps.generatedKeys

        return if (rs.next()) rs else null
    }

    fun <T> query(statement: String, psValues: Array<Any?> = arrayOf(), block: (ResultSet) -> T): T? {
        val ps = connection.prepareStatement(statement)
        pss += ps
        require(ps.parameterMetaData.parameterCount == psValues.size)
        for (i in 1..psValues.size) {
            ps[i] = psValues[i - 1]
        }
        val resultSet = ps.executeQuery()
        return if (resultSet.next()) {
            block(resultSet)
        } else {
            null
        }
    }

    fun <T> queryList(statement: String, psValues: Array<Any?> = arrayOf(), block: (ResultSet) -> T): List<T> {
        val list = mutableListOf<T>()
        val ps = connection.prepareStatement(statement)
        pss += ps
        require(ps.parameterMetaData.parameterCount == psValues.size)
        for (i in 1..psValues.size) {
            ps[i] = psValues[i - 1]
        }

        val resultSet = ps.executeQuery()
        while (resultSet.next())
            list.add(block(resultSet))

        return list
    }

    /**
     * **WARNING: UNTESTED! USES REFLECTION!**
     */
    fun insert(obj: Any) {
        obj.javaClass.kotlin.apply {
            require(isData) {
                "$qualifiedName Must be a data class!"
            }
            val sameAsDB = annotations.firstOrNull { it is SameAsDB } as? SameAsDB
                    ?: throw IllegalStateException("Cannot call insert on an object that doesn't have the @SameAsDB annotation")

            val columns = declaredMemberProperties

            val columnsStr = columns.map { it.name }.reduce { acc, x -> "$acc, $x" }
            var questions = ""
            for (i in 0 until columns.size) {
                questions += "?,"
            }

            questions = questions.substring(0, questions.length - 1)

            val propertyValues = columns.map { it.get(obj) }.toTypedArray()

            //language=MYSQL-SQL
            val statement = "INSERT INTO ${sameAsDB.tableNameInDB} ($columnsStr) VALUES ($questions)"

            exec(statement, propertyValues)
        }
    }

    internal fun finalize() {
        connection.commit()
        pss.map(PreparedStatement::close)
        connection.close()
    }
}