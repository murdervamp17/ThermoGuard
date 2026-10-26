package com.thermoguard.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Lightweight on-device history. Nothing ever leaves the phone. */
class ReadingStore private constructor(context: Context) :
    SQLiteOpenHelper(context, "thermoguard.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE readings (ts INTEGER PRIMARY KEY, battery REAL, cpu REAL, ambient REAL, status INTEGER)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS readings")
        onCreate(db)
    }

    fun insert(r: TempReading) {
        val values = ContentValues().apply {
            put("ts", r.timestamp)
            put("battery", r.batteryC)
            put("cpu", r.cpuC)
            put("ambient", r.ambientC)
            put("status", r.thermalStatus)
        }
        writableDatabase.insertWithOnConflict("readings", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun since(fromMillis: Long): List<TempReading> {
        val out = mutableListOf<TempReading>()
        readableDatabase.query(
            "readings", arrayOf("ts", "battery", "cpu", "ambient", "status"),
            "ts >= ?", arrayOf(fromMillis.toString()), null, null, "ts ASC"
        ).use { c ->
            while (c.moveToNext()) {
                out += TempReading(
                    timestamp = c.getLong(0),
                    batteryC = c.floatOrNull(1),
                    cpuC = c.floatOrNull(2),
                    ambientC = c.floatOrNull(3),
                    thermalStatus = if (c.isNull(4)) null else c.getInt(4),
                )
            }
        }
        return out
    }

    fun pruneOlderThan(cutoffMillis: Long) {
        writableDatabase.delete("readings", "ts < ?", arrayOf(cutoffMillis.toString()))
    }

    fun clear() {
        writableDatabase.delete("readings", null, null)
    }

    private fun Cursor.floatOrNull(i: Int): Float? = if (isNull(i)) null else getFloat(i)

    companion object {
        @Volatile private var instance: ReadingStore? = null
        fun get(context: Context): ReadingStore =
            instance ?: synchronized(this) {
                instance ?: ReadingStore(context.applicationContext).also { instance = it }
            }
    }
}
