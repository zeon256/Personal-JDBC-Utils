 [ ![Download](https://api.bintray.com/packages/budinverse/utils/JDBCUtils/images/download.svg) ](https://bintray.com/budinverse/utils/JDBCUtils/_latestVersion)
 [![MIT Licence](https://badges.frapsoft.com/os/mit/mit.svg?v=103)](https://opensource.org/licenses/mit-license.php)
 
# Personal-JDBC-Utils
This is my small personal jdbc utilities library to help me reduce retyping stuff mainly `(ie. Creating connection, closing connection)`.
The user is still responsible for setting the query parameters.

## Note
Version 0.4 and 0.4.1 IS BUGGY and SHOULD NOT BE USED.

## Gradle
```groovy
repositories {
    jcenter()
}
dependencies {
  compile group: 'mysql', name: 'mysql-connector-java', version: '6.0.6' //depends on the driver you need
  compile 'com.budinverse.utils:Personal-JDBC-Utils:<latest version>'
}

```

## How to use?

Assume there is this in your codebase (all examples)
```kotlin
data class Person(val email: String, val name: String){
    constructor(resultSet: ResultSet):
            this(resultSet[1], resultSet[2])
}

```


1. Create a dbConfig.properties file with the following details inside and put it at root directory of project. You should be able to 
use the library already.
```properties
databaseUser = yourDatabaseUser
databasePassword = yourDatabasePassword
databaseUrl = yourDatbaseUrl
driver = yourDatabaseDriver
```

```kotlin
fun main(args: Array<String>) {
    setConfigFile("filename.properties") 
    //can only be set once, setting more than once will
    //result in error
}
```

2. INSERT/UPDATE/DELETE Statements
Assuming you have a Person data class and you want to insert into the person table.
```kotlin
/* Example */
fun insertPerson(person:Person) = manipulate("INSERT INTO person VALUES (?,?)",{
  it[1] = person.email
  it[2] = person.name
})
```

3. Query Single Result.

```kotlin
/* Example */
fun getPersonByName(name:String) = 
    query("SELECT * FROM person WHERE name = ?", { it[1] = name }, ::Person)
```

4. Query Multiple Results.
Multiple results returns and arraylist of said object
```kotlin
/* Example */
fun getPersons() = queryMulti("SELECT * FROM person", ::Person)
```

## Transaction Support
```kotlin
    val data = Person("budisyahiddin@gmail.com","budi")
    val data2 = Person("budisyahiddin@gmail.xd","budixd")
    val txn = transaction {
        manipulateTxn("INSERT INTO Person (email,name) VALUES (?,?)",it,{
            it[1] = data.email
            it[2] = data.name
        })
        manipulateTxn("UPDATE Person set email = ?, name = ?",it,{
            it[1] = data2.email
            it[2] = data2.name
        })
    }
    //second run of this will result in rollback
    if(txn == null)
        println("Fail")
    else
        println("!Fail")
```

## Experimental
All implementations in experimental packages have not been tested for performance

#### Set all 
```kotlin
query("SELECT * FROM person WHERE email = ? AND name = ?",{
        it.setAllValues(data.email,data.name)
    } ,::Person)
```

