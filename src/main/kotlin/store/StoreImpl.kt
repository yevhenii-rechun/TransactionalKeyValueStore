import store.Store
import store.Transaction
import store.TransactionAction
import store.TransactionImpl

internal class StoreImpl : Store {

    private val store = HashMap<String, String>()

    override fun get(key: String): String? {
        return store[key]
    }

    override fun set(key: String, value: String) {
        store[key] = value
    }

    override fun delete(key: String): Boolean {
        return store.remove(key) != null
    }

    override fun count(value: String): Int {
        return store.count { it.value == value }
    }

    override fun beginTransaction(): Transaction {
        val storeSnapshot = HashMap(store)
        return TransactionImpl(HashMap(storeSnapshot), object : TransactionAction {
            override fun onCommit(newState: HashMap<String, String>) {
                store.putAll(newState)
                storeSnapshot.forEach { (key) ->
                    if (!newState.contains(key)) store.remove(key)
                }
            }

            override fun onRollback() = Unit
        })
    }
}
