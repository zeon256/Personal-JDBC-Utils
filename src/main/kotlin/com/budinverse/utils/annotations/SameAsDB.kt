package com.budinverse.utils.annotations

@Target(AnnotationTarget.CLASS)
@MustBeDocumented
annotation class SameAsDB(val tableNameInDB: String)