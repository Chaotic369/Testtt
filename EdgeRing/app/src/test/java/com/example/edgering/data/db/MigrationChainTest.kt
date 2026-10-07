package com.example.edgering.data.db

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guard that runs on every `./gradlew test`: a schema version bump without a matching, contiguous
 * migration fails here. The real data-preserving checks live in the instrumented MigrationTest.
 */
class MigrationChainTest {
    @Test
    fun everyVersionStepHasExactlyOneMigration() {
        val sorted = EdgeRingDatabase.MIGRATIONS.sortedBy { it.startVersion }
        assertEquals(EdgeRingDatabase.VERSION - 1, sorted.size)
        sorted.forEachIndexed { i, m ->
            assertEquals(i + 1, m.startVersion)
            assertEquals(i + 2, m.endVersion)
        }
    }
}
