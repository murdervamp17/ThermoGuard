package com.thermoguard.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Lightweight on-device history. Nothing ever leaves the phone. */
class ReadingStore private constructor(context: Context) :
    SQLiteOpenHelper(context, "thermoguard.db", null, 2) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE readings (ts INTEGER PRIMARY KEY, battery REAL, cpu REAL, ambient REAL, status INTEGER, " +
                "battery_source TEXT NOT NULL DEFAULT 'UNAVAILABLE', cpu_source TEXT NOT NULL DEFAULT 'UNAVAILABLE', " +
                "ambient_source TEXT NOT NULL DEFAULT 'UNAVAILABLE')"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE readings ADD COLUMN battery_source TEXT NOT NULL DEFAULT 'UNAVAILABLE'")
            db.execSQL("ALTER TABLE readings ADD COLUMN cpu_source TEXT NOT NULL DEFAULT 'UNAVAILABLE'")
            db.execSQL("ALTER TABLE readings ADD COLUMN ambient_source TEXT NOT NULL DEFAULT 'UNAVAILABLE'")
            db.execSQL("UPDATE readings SET battery_source = 'AVAILABLE' WHERE battery IS NOT NULL")
            db.execSQL("UPDATE readings SET cpu_source = 'AVAILABLE' WHERE cpu IS NOT NULL")
            db.execSQL("UPDATE readings SET ambient_source = 'AVAILABLE' WHERE ambient IS NOT NULL")
        }
    }

    fun insert(r: TempReading) {
        val values = ContentValues().apply {
            put("ts", r.timestamp)
            put("battery", r.batteryC)
            put("cpu", r.cpuC)
            put("ambient", r.ambientC)
            put("status", r.thermalStatus)
            put("battery_source", r.batterySource.name)
            put("cpu_source", r.cpuSource.name)
            put("ambient_source", r.ambientSource.name)
        }
        writableDatabase.insertWithOnConflict("readings", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun since(fromMillis: Long): List<TempReading> {
        val out = mutableListOf<TempReading>()
        readableDatabase.query(
            "readings", arrayOf(
                "ts", "battery", "cpu", "ambient", "status",
                "battery_source", "cpu_source", "ambient_source",
            ),
            "ts >= ?", arrayOf(fromMillis.toString()), null, null, "ts ASC"
        ).use { c ->
            while (c.moveToNext()) {
                out += c.toTempReading()
            }
        }
        return out
    }

    fun latest(): TempReading? {
        val columns = arrayOf(
            "ts", "battery", "cpu", "ambient", "status",
            "battery_source", "cpu_source", "ambient_source",
        )
        return readableDatabase.query(
            "readings", columns, null, null, null, null, "ts DESC", "1"
        ).use { c ->
            if (c.moveToFirst()) c.toTempReading() else null
        }
    }

    fun pruneOlderThan(cutoffMillis: Long) {
        writableDatabase.delete("readings", "ts < ?", arrayOf(cutoffMillis.toString()))
    }

    fun clear() {
        writableDatabase.delete("readings", null, null)
    }

    private fun Cursor.floatOrNull(i: Int): Float? = if (isNull(i)) null else getFloat(i)

    private fun Cursor.toTempReading() = TempReading(
        timestamp = getLong(0),
        batteryC = floatOrNull(1),
        cpuC = floatOrNull(2),
        ambientC = floatOrNull(3),
        thermalStatus = if (isNull(4)) null else getInt(4),
        batterySource = SensorAvailability.fromStorage(getString(5)),
        cpuSource = SensorAvailability.fromStorage(getString(6)),
        ambientSource = SensorAvailability.fromStorage(getString(7)),
    )

    companion object {
        @Volatile private var instance: ReadingStore? = null
        fun get(context: Context): ReadingStore =
            instance ?: synchronized(this) {
                instance ?: ReadingStore(context.applicationContext).also { instance = it }
            }
    }
}
