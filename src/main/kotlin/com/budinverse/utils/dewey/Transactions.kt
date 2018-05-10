package com.budinverse.utils.dewey

import com.budinverse.utils.annotations.SameAsDB
import com.budinverse.utils.getDbConnection
import com.budinverse.utils.set
import java.sql.Connection
import java.sql.PreparedStatement
import kotlin.reflect.full.declaredMemberProperties

fun transaction(block: TransactionBuilder.() -> Unit): Boolean {
    return try {
        TransactionBuilder(block = block).apply {
            block()
            finalize()
        }

        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
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

    fun exec(statement: String, psValues: Array<Any?> = arrayOf()) {
        val ps = connection.prepareStatement(statement)
        pss += ps
        require(ps.parameterMetaData.parameterCount == psValues.size)
        for (i in 1..psValues.size) {
            ps[i] = psValues[i-1]
        }

        ps.executeUpdate()
    }

    /**
     * **WARNING: UNTESTED! USES REFLECTION!**
     */
    fun insert(obj: Any) {
        obj.javaClass.kotlin.apply {
            require(isData) {
                "$qualifiedName Must be a data class!"
            }
            val sameAsDB = annotations.firstOrNull { it is SameAsDB } as? SameAsDB ?:
                    throw IllegalStateException("Cannot call insert on an object that doesn't have the @SameAsDB annotation")

            val columns = declaredMemberProperties

            val columnsStr = columns.map {it.name}.reduce {acc, x -> "$acc, $x"}
            var questions = ""
            for (i in 0 until columns.size) {
                questions += "?,"
            }

            questions = questions.substring(0, questions.length - 1)

            val propertyValues = columns.map {it.get(obj)}.toTypedArray()

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