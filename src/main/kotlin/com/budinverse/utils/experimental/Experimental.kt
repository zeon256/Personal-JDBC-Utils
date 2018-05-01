package com.budinverse.utils.experimental

import com.budinverse.utils.get
import com.budinverse.utils.set
import java.sql.PreparedStatement
import java.sql.ResultSet
import kotlin.reflect.KClass

fun PreparedStatement.setAllValues(vararg data: Any) {
    for(i in 0 until data.size){ this[i+1] = data[i] }
}