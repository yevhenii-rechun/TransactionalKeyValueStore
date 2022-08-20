package store

internal interface TransactionAction {
    fun onCommit(newState: HashMap<String, String>)
    fun onRollback()
}
