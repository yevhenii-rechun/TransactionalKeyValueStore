package store

import StoreImpl

object StoreApi {

    fun provideConcurrentUnsafeStore(): Store {
        return StoreImpl()
    }
}
