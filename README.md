 [ ![Download](https://api.bintray.com/packages/budinverse/utils/JDBCUtils/images/download.svg) ](https://bintray.com/budinverse/utils/JDBCUtils/_latestVersion)
 [![MIT Licence](https://badges.frapsoft.com/os/mit/mit.svg?v=103)](https://opensource.org/licenses/mit-license.php)
 
# Personal-JDBC-Utils
This is my small personal jdbc utilities library to help me reduce retyping stuff mainly `(ie. Creating connection, closing connection)`.
The user is still responsible for setting the query parameters.
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
  it.setString(1,person.name)
  it.setInt(2,person.age)
})
```

3. Query Single Result.

```kotlin
/* Example */
fun getPersonByName(name:String) = 
    query("SELECT * FROM person WHERE name = ?", { it.setString(1,name) }, ::Person)
```

4. Query Multiple Results.
Multiple results returns and arraylist of said object
```kotlin
/* Example */
fun getPersons() = queryMulti("SELECT * FROM person", {}, ::Person)
```

