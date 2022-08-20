package store

import StoreImpl
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class StoreTest {

    private lateinit var store: Store

    @BeforeEach
    fun setup() {
        store = StoreImpl()
    }

    @Test
    fun `GIVEN transaction WHEN modifying after commit THEN error`() {
        assertThrows<TransactionForbiddenModificationException> {
            store.runInnerTransaction {
                val result = commit()
                set("a", "111")
                result
            }
            assertEquals("1", store.get("a"))
        }
    }

    @Test
    fun `GIVEN inner transactions WHEN sending different result THEN error`() {
        assertThrows<RuntimeException> {
            store.set("a", "1")
            store.runInnerTransaction {
                store.set("a", "11")
                val result = store.beginTransaction().commit()
                runInnerTransaction {
                    result
                }
                result
            }
            assertEquals("1", store.get("a"))
        }
    }

    @Test
    fun `GIVEN delete element in transaction WHEN transaction is empty THEN other transactions affected`() {
        store.set("a", "1")
        store.runInnerTransaction {
            assertEquals("1", get("a"))
            runInnerTransaction {
                assertEquals("1", get("a"))
                delete("a")
                commit()
            }
            assertNull(get("a"))
            commit()
        }
        assertNull(store.get("a"))
    }

    @Test
    fun `GIVEN multiple inner transactions in one WHEN commit all THEN latest value saved`() {
        assertNull(store.get("a"))
        store.runInnerTransaction {
            runInnerTransaction {
                set("a", "1")
                assertEquals("1", get("a"))
                commit()
            }
            assertEquals("1", get("a"))
            runInnerTransaction {
                set("a", "2")
                commit()
            }
            assertEquals("2", get("a"))
            commit()
        }
        assertEquals("2", store.get("a"))
    }

    @Test
    fun `GIVEN inner transactions WHEN committing root transaction having unfinished inner transaction THEN should crash`() {
        assertThrows<IllegalStateException> {
            store.runInnerTransaction {
                beginTransaction()
                commit()
            }
        }
    }

    @Test
    fun `WHEN committing wrong transaction THEN expecting crash`() {
        assertThrows<RuntimeException> {
            store.runInnerTransaction {
                val t = this
                runInnerTransaction {
                    t.commit()
                }
                commit()
            }
        }
    }

    @Test
    fun `GIVEN multiple parallel transactions WHEN commit all THEN all data is saved`() {
        assertNull(store.get("a"))
        assertNull(store.get("b"))

        fun commitDifferentTransaction() {
            store.runInnerTransaction {
                set("a", "1")
                commit()
            }
        }

        store.runInnerTransaction {
            assertNull(get("a"))
            assertNull(get("b"))

            commitDifferentTransaction()

            assertNull(get("a"))
            assertNull(get("b"))

            set("b", "2")
            commit()
        }

        assertEquals("1", store.get("a"))
        assertEquals("2", store.get("b"))
    }

    @Test
    fun `GIVEN innerTransactions WHEN rollback root transaction THEN store not modified`() {
        assertNull(store.get("a"))
        assertNull(store.get("b"))
        assertNull(store.get("c"))
        assertNull(store.get("d"))
        assertNull(store.get("e"))
        store.runInnerTransaction {
            runInnerTransaction {
                set("a", "1")
                runInnerTransaction {
                    set("b", "2")
                    runInnerTransaction {
                        set("c", "3")
                        commit()
                    }
                    set("d", "4")
                    commit()
                }
                set("e", "5")
                commit()
            }
            rollback()
        }
        assertNull(store.get("a"))
        assertNull(store.get("b"))
        assertNull(store.get("c"))
        assertNull(store.get("d"))
        assertNull(store.get("e"))
    }

    @Test
    fun `GIVEN inner transaction WHEN setting value after inner transaction THEN the last set value is in store`() {
        assertNull(store.get("a"))
        store.runInnerTransaction {
            set("a", "1")
            runInnerTransaction {
                set("a", "11")
                assertEquals("11", get("a"))
                delete("a")
                assertNull(get("a"))
                commit()
            }
            assertNull(get("a"))
            set("a", "111")
            commit()
        }
        assertEquals("111", store.get("a"))
    }

    @Test
    fun `GIVEN inner transactions WHEN setting the same value and commit THEN only last one is saved`() {
        store.set("a", "1")

        store.runInnerTransaction {
            set("a", "1")
            runInnerTransaction {
                set("a", "11")
                runInnerTransaction {
                    set("a", "111")
                    commit()
                }
                commit()
            }
            commit()
        }

        assertEquals("111", store.get("a"))
    }

    @Test
    fun `GIVEN inner transactions WHEN delete and commit all THEN transactions impacted recursively`() {
        store.set("a", "1")

        store.runInnerTransaction {
            runInnerTransaction {
                runInnerTransaction {
                    delete("a")
                    commit()
                }
                assertNull(get("a"))
                commit()
            }
            assertNull(get("a"))
            commit()
        }

        assertNull(store.get("a"))
    }

    @Test
    fun `GIVEN single transaction WHEN reading store before commit THEN store is not updated before commit and only after`() {
        store.set("a", "1")

        store.runInnerTransaction {
            set("a", "11")
            set("b", "22")

            assertEquals("1", store.get("a"))
            assertNull(store.get("b"))

            commit()
        }

        assertEquals("11", store.get("a"))
        assertEquals("22", store.get("b"))
    }

    @Test
    fun `GIVEN multiple inner transactions WHEN commit all THEN all transactions impacted recursively`() {
        val store: Store = StoreImpl()

        store.set("a", "1")
        store.set("b", "2")
        store.set("c", "3")

        assertEquals("1", store.get("a"))
        assertEquals("2", store.get("b"))
        assertEquals("3", store.get("c"))

        store.runInnerTransaction {

            assertEquals("1", get("a"))
            assertEquals("2", get("b"))
            assertEquals("3", get("c"))

            set("a", "11")
            set("b", "22")

            assertEquals("11", get("a"))
            assertEquals("22", get("b"))
            assertEquals("3", get("c"))

            runInnerTransaction {
                set("a", "111")

                assertEquals("111", get("a"))
                assertEquals("22", get("b"))
                assertEquals("3", get("c"))

                commit()
            }

            assertEquals("111", get("a"))
            assertEquals("22", get("b"))
            assertEquals("3", get("c"))

            commit()
        }

        assertEquals("111", store.get("a"))
        assertEquals("22", store.get("b"))
        assertEquals("3", store.get("c"))
    }

    @Test
    fun `GIVEN many items set in nested transactions WHEN count THEN all transactions count upstream values`() {
        assertEquals(0, store.count("1"))

        store.runInnerTransaction {
            set("a", "1")
            assertEquals(1, count("1"))
            runInnerTransaction {
                set("b", "1")
                assertEquals(2, count("1"))
                runInnerTransaction {
                    set("c", "1")
                    assertEquals(3, count("1"))
                    commit()
                }
                assertEquals(3, count("1"))
                commit()
            }
            assertEquals(3, count("1"))
            commit()
        }
        assertEquals(3, store.count("1"))
    }

    @Test
    fun `GIVEN inner transaction WHEN rollback and commit in different order THEN transactions dont fail`() {
        store.runInnerTransaction {
            runInnerTransaction {
                runInnerTransaction {
                    runInnerTransaction {
                        runInnerTransaction {
                            runInnerTransaction {
                                runInnerTransaction {
                                    commit()
                                }
                                commit()
                            }
                            rollback()
                        }
                        rollback()
                    }
                    commit()
                }
                rollback()
            }
            commit()
        }
    }
}
