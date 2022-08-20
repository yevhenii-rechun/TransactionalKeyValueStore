package store

interface Transaction : Store, Transactional {
    val id: Long
    fun commit(): Result
    fun rollback(): Result

    class Result internal constructor(val id: Long)
}

class TransactionForbiddenModificationException() : Exception("Transaction is already committed or discarded, cannot be modified.")
