package store

import java.util.concurrent.atomic.AtomicLong

internal class TransactionImpl(
    private val storeState: HashMap<String, String>,
    private val transactionAction: TransactionAction,
) : Store, Transaction {

    private val transactionResult = Transaction.Result(nextId())
    private val resultState = HashMap(storeState)

    private var isAlive = true
    private var hasUnfinishedInnerTransaction = false

    override val id: Long = transactionResult.id

    override fun commit(): Transaction.Result = whenValid {
        requireNoInnerTransactions()
        isAlive = false
        transactionAction.onCommit(resultState)
        transactionResult
    }

    override fun rollback(): Transaction.Result = whenValid {
        requireNoInnerTransactions()
        isAlive = false
        transactionAction.onRollback()
        transactionResult
    }

    override fun get(key: String): String? = whenValid {
        resultState[key]
    }

    override fun set(key: String, value: String) = whenValid {
        resultState[key] = value
    }

    override fun delete(key: String): Boolean = whenValid {
        return resultState.remove(key) != null
    }

    override fun count(value: String): Int = whenValid {
        return resultState.count { it.value == value }
    }

    override fun beginTransaction(): TransactionImpl = whenValid {
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

    private inline fun <T> whenValid(block: () -> T): T {
        return if (isAlive) block() else throw TransactionForbiddenModificationException()
    }

    companion object IdGenerator {
        private val count = AtomicLong()
        private fun nextId(): Long = count.incrementAndGet()
    }
}
