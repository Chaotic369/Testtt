package com.example.edgering.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented migration harness (run with `./gradlew connectedAndroidTest` on a device/emulator).
 * Requires the exported schema files in app/schemas (committed after the first CI build).
 *
 * When adding version N+1: add the Migration to EdgeRingDatabase.MIGRATIONS, then add a test that
 * calls `helper.createDatabase(TEST_DB, N)`, inserts representative rows with raw SQL, closes it,
 * and `runMigrationsAndValidate(TEST_DB, N + 1, true, <migration>)`, then asserts the rows survived.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        EdgeRingDatabase::class.java,
    )

    @Test
    fun oldestSchemaOpensAndValidatesAgainstTheLatestWithAllMigrations() {
        helper.createDatabase(TEST_DB, 1).close()
        helper.runMigrationsAndValidate(TEST_DB, EdgeRingDatabase.VERSION, true, *EdgeRingDatabase.MIGRATIONS).close()
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
