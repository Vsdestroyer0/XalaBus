package com.example.xalabus.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.xalabus.DBD.AppDatabase

actual class DriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        // v2: incluye StopEntity (CU-09)
        return AndroidSqliteDriver(AppDatabase.Schema, context, "xalabus_v2.db")
    }
}
