 [ ![Download](https://api.bintray.com/packages/budinverse/utils/JDBCUtils/images/download.svg) ](https://bintray.com/budinverse/utils/JDBCUtils/_latestVersion)
 [![MIT Licence](https://badges.frapsoft.com/os/mit/mit.svg?v=103)](https://opensource.org/licenses/mit-license.php)
 
# Personal-JDBC-Utils
This is my personal jdbc utilities library to help me reduce retyping stuff mainly `(ie. Creating connection, closing connection)`.
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

1. Create a dbConfig.properties file with the following details inside and put it at root directory of project. You should be able to 
use the library already.
```properties
databaseUser = yourDatabaseUser
databasePassword = yourDatabasePassword
databaseUrl = yourDatbaseUrl
driver = yourDatabaseDriver
```

2. INSERT/UPDATE/DELETE Statements
Assuming you have a Person class and you want to insert into the person table.
```kotlin
data class Person(val name:String, val age: Int)
.
.
.

/* Example */
fun insertPerson(person:Person) = manipulate("INSERT INTO person VALUES (?,?)",{
  it.setString(1,person.name)
  it.setInt(2,person.age)
})
```
Java Example
```java
private static int insertPerson(Person person) {
   return DbUtilsKt.manipulate("INSERT INTO person VALUES (?,?)",
        (PreparedStatement x) -> {
             try {
                 x.setString(1, person.getName());
                 x.setInt(2, person.getAge());
              } catch (SQLException e) {
                  e.printStackTrace();
              }
              return Unit.INSTANCE;
        });
    }
```


3. Query Single Result.
Assume that there is an extension function that converts ResultSet to to Person object
```kotlin

/* Example */
fun getPersonByName(name:String) = 
    query("SELECT * FROM person WHERE name = ?", { it.setString(1,name) }, { it.toPerson() })

```
Java Example
```java
private static Person getPersonByName(String name){
    return DbUtilsKt.query("SELECT * FROM person WHERE name = ?",
          (PreparedStatement x) -> {
               try {
                   x.setString(1, name);
               } catch (SQLException e) {
                  e.printStackTrace();
               }
                  return Unit.INSTANCE;
             },PersonKt::toPerson);
    }
```

4. Query Multiple Results.
Multiple results returns and arraylist of said object
```kotlin
/* Example */
fun getPersons() = queryMulti("SELECT * FROM person", {}, { it.toPerson() })
```

Java Example
```java
private static ArrayList<Person> getPersons(){
    return DbUtilsKt.queryMulti("SELECT * FROM person", (PreparedStatement x) -> Unit.INSTANCE,PersonKt::toPerson);
}
```


## Others
Changing `Config.json`
```kotlin
fun main(args: Array<String>) {
    setConfigFile("filename.properties") 
    //can only be set once, setting more than once will
    //result in error
}
```
