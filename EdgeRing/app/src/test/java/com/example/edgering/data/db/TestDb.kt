package com.example.edgering.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider

/** In-memory database for Robolectric tests. Main-thread queries are allowed only here. */
object TestDb {
    fun create(): EdgeRingDatabase =
        Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), EdgeRingDatabase::class.java)
            .allowMainThreadQueries()
            .build()
}
