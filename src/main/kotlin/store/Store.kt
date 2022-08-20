package store

interface Store : Transactional {
    fun get(key: String): String?
    fun set(key: String, value: String)
    fun delete(key: String): Boolean
    fun count(value: String): Int
}
