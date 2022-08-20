package store

import java.util.concurrent.atomic.AtomicLong

internal class TransactionImpl(
    private val storeState: HashMap<String, String>,
    private val transactionAction: TransactionAction,
) : Store, Transaction {

    private val transactionResult = Transaction.Result(nextId())
    private val resultState = HashMap(storeState)

    private var hasUnfinishedInnerTransaction = false

    override val id: Long = transactionResult.id

    override fun commit(): Transaction.Result {
        requireNoInnerTransactions()
        transactionAction.onCommit(resultState)
        return transactionResult
    }

    override fun rollback(): Transaction.Result {
        requireNoInnerTransactions()
        transactionAction.onRollback()
        return transactionResult
    }

    override fun get(key: String): String? {
        return resultState[key]
    }

    override fun set(key: String, value: String) {
        resultState[key] = value
    }

    override fun delete(key: String): Boolean {
        return resultState.remove(key) != null
    }

    override fun count(value: String): Int {
        return resultState.count { it.value == value }
    }

    override fun beginTransaction(): TransactionImpl {
        hasUnfinishedInnerTransaction = true
        return TransactionImpl(HashMap(resultState), object : TransactionAction {
            override fun onCommit(newState: HashMap<String, String>) {
                hasUnfinishedInnerTransaction = false
                applyNewStateFromInnerTransaction(newState)
            }

            override fun onRollback() {
                hasUnfinishedInnerTransaction = false
            }
        })
    }

    private fun applyNewStateFromInnerTransaction(newState: HashMap<String, String>) {
        storeState.clear()
        storeState.putAll(newState)
        resultState.clear()
        resultState.putAll(newState)
    }

    private fun requireNoInnerTransactions() {
        if (hasUnfinishedInnerTransaction) throw IllegalStateException("Finalize all inner transactions first")
    }

    companion object IdGenerator {
        private val count = AtomicLong()
        private fun nextId(): Long = count.incrementAndGet()
    }
}
