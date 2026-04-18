package io.github.ringwdr.novelignite.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.github.ringwdr.novelignite.db.NovelIgniteDatabase
import java.io.File

fun openDesktopDatabase(): NovelIgniteDatabase {
    val databaseDirectory = File(System.getProperty("user.home"), ".novelignite")
    databaseDirectory.mkdirs()
    val databaseFile = File(databaseDirectory, "novel-ignite.db")
    val needsSchema = !databaseFile.exists() || databaseFile.length() == 0L
    val driver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath}")
    driver.execute(null, "PRAGMA foreign_keys = ON", 0)
    if (needsSchema) {
        NovelIgniteDatabase.Schema.create(driver)
    }
    return NovelIgniteDatabase(driver)
}
