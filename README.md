# Transactional Key-Value store

Store supports CRUD operations for string values. As well all operations can be packed into transactions and performed
within it without impacting original store before committing transaction.

## Features

- set(key, value) // sets value by specified key
- get(key) // gets value by key if set or null if value is missing
- count(value) // counts amount of values
- delete(key) // deletes value by specified key
- beginTransaction() // begins a new transaction and returns its reference
- transaction.commit() // commits receiver transaction and save all its actions to either upstream transaction or
  directly to the store, depedning in which scope it was created
- transaction.rollback() // rollbacks transaction and all its actions

## How to use command line sample

As a part of the library there is included `.jar` sample which can run in cmdline.

##### Command line app features

```
SET <key> <value> // store the value for key
GET <key>         // return the current value for key
DELETE <key>      // remove the entry for key
COUNT <value>     // return the number of keys that have the given value
BEGIN             // start a new transaction
COMMIT            // complete the current transaction
ROLLBACK          // revert to state prior to BEGIN call
```

##### How To run cmd line app

Run `java -jar jar/KeyValueStore-cmdline-tool.jar` in your terminal and follow the instructions.
List of the supported commands is provided above.

## How to use kotlin library

Kotlin library operates on 2 main types: `Store` and `Transaction` both of the are `Transactional` what means that from
both Store and Transaction you can begin a new transaction which in case of commit will affect the data of the upstream
transactional object it was created from.
For instance if you create transactionA from store and then transactionB from transactionA by commiting transactionB
store will not be impacted but only transactionA. To apply all the changes to the store you need to commit store owned
transactionA.
Transaction has the same functionality as store has.

Transactions should be finilized in the order they were created.

```kotlin
interface Store : Transactional {
    fun get(key: String): String?
    fun set(key: String, value: String)
    fun delete(key: String): Boolean
    fun count(value: String): Int
}
```

```kotlin
interface Transaction : Store, Transactional {
    val id: Long
    fun commit(): Result
    fun rollback(): Result

    class Result internal constructor(val id: Long)
}
```

```kotlin
interface Transactional {
    fun beginTransaction(): Transaction
}
```

There is an extension function `runInnerTransaction(body: Transaction.() -> Transaction.Result)` for `Store` which can
be used to run transaction withing the action block:

```kotlin
val store: Store
store.runInnerTransaction {
    commit() // commits all the changes in transaction to owner object (Store in this case)
}
store.runInnerTransaction {
    rollback() // discards all the changes done within a transaction
}

store.runInnerTransaction { } // will not compile as at the last point in the block we need to either commit or rollback a transaction
```

Example usage:

```kotlin
val store: Store
store.set("a", "1") // set "1" by key "a"
store.runInnerTransaction transaction1@{
    set("b", "2") // set "2" by key "b"
    runInnerTransaction transaction2@{
        set("c", "3") // set "3" by key "c"
        delete("a") // delete all values associated with a
        commit() // commit all changes to transaction1
    }
    commit() // commit all changes to store
}
store.get("a") // return "1"
store.get("b") // return "2"
store.get("c") // return "3"
```