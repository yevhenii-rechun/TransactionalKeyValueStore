package cmd

import store.Store
import store.Transaction
import store.Transactional
import java.util.LinkedList

class CmdStoreWrapper(private val store: Store) {

    private val transactions = LinkedList<Transaction>()

    fun get(key: String) {
        val store = transactions.lastOrNull() ?: store
        val value = store.get(key)
        if (value == null) println("key not set") else println(value)
    }

    fun set(key: String, value: String) {
        val store = transactions.lastOrNull() ?: store
        store.set(key, value)
    }

    fun delete(key: String) {
        val store = transactions.lastOrNull() ?: store
        if (!store.delete(key)) println("key not set")
    }

    fun count(value: String) {
        val store = transactions.lastOrNull() ?: store
        val count = store.count(value)
        println(count)
    }

    fun beginTransaction() {
        val transactional: Transactional = transactions.lastOrNull() ?: store
        transactions.add(transactional.beginTransaction())
    }

    fun commitTransaction() {
        val transaction: Transaction? = transactions.pollLast()
        if (transaction == null) println("no transaction") else transaction.commit()
    }

    fun rollbackTransaction() {
        val transaction: Transaction? = transactions.pollLast()
        if (transaction == null) println("no transaction") else transaction.rollback()
    }
}
