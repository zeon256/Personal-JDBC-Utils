import com.budinverse.utils.annotations.SameAsDB
import com.budinverse.utils.dewey.transaction
import com.budinverse.utils.setConfigFile
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import kotlin.concurrent.thread

@SameAsDB("niggas")
data class nigga(val niggaID: Int)

fun main(args: Array<String>) {
    setConfigFile("dbconfig.properties")
    runBlocking {
        List(100000) {
            launch {
                transaction {
                    exec("INSERT INTO niggas (niggaID) values (?)", arrayOf(it))
                }
                println("finished $it")
            }
        }.forEach {it.join()}
    }

}