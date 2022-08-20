package store

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

interface Transactional {
    fun beginTransaction(): Transaction
}

@OptIn(ExperimentalContracts::class)
inline fun Transactional.runInnerTransaction(body: Transaction.() -> Transaction.Result) {
    contract { callsInPlace(body, InvocationKind.EXACTLY_ONCE) }

    val transaction = beginTransaction()
    val result = body(transaction)

    if (transaction.id != result.id) {
        throw RuntimeException("Committing wrong transaction. Actual transaction id: ${transaction.id}, but committing: ${result.id}")
    }
}
