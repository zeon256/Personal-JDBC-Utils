package com.budinverse.utils.experimental

import com.budinverse.utils.set
import java.sql.PreparedStatement


/**
 * Experimental
 *
 * Performance issues with varargs at least according to
 * https://sites.google.com/a/athaydes.com/renato-athaydes/posts/kotlinshiddencosts-benchmarks
 *
 * @param data
 */
fun PreparedStatement.setAllValues(vararg data: Any) {
    for(i in 0 until data.size){ this[i+1] = data[i] }
}
