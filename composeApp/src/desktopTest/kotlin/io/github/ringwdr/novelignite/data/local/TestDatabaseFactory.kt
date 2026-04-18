package io.github.ringwdr.novelignite.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.github.ringwdr.novelignite.db.NovelIgniteDatabase

object TestDatabaseFactory {
    fun create(): NovelIgniteDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, "PRAGMA foreign_keys = ON", 0)
        NovelIgniteDatabase.Schema.create(driver)
        return NovelIgniteDatabase(driver)
    }
}
