import com.budinverse.utils.annotations.SameAsDB
import com.budinverse.utils.dewey.transaction
import com.budinverse.utils.setConfigFile
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import kotlin.system.measureTimeMillis

@SameAsDB("Person")
data class Person(val id: Int)

fun main(args: Array<String>) {
    setConfigFile(cfgFile = "dbconfig.properties")

//    measureTimeMillis {
//        runBlocking {
//            List(200_00) {
//                launch {
//                    transaction {
//                        insert(Person(it))
//                    }
//                    println("finished $it")
//                }
//            }.forEach {it.join()}
//        }
//    }.run(::print)

    measureTimeMillis {
        runBlocking {
            List(200_00) {
                launch {
                    transaction {
                        exec("INSERT INTO Person (id) VALUES (?)", arrayOf(it))
                    }
                    println("finished $it")
                }
            }.forEach { it.join() }
        }
    }.run(::print)
}

