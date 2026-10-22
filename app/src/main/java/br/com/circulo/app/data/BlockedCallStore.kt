package br.com.circulo.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.sqlite.transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class BlockedCall(val id: Long, val number: String, val timestamp: Long)

class BlockedCallStore(context: Context) : SQLiteOpenHelper(context, "blocked_calls.db", null, 1) {
    private val mutableRevision = MutableStateFlow(0L)
    val revision = mutableRevision.asStateFlow()
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE blocked_calls (id INTEGER PRIMARY KEY AUTOINCREMENT, number TEXT NOT NULL, timestamp INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX blocked_calls_timestamp ON blocked_calls(timestamp DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    @Synchronized
    fun record(number: String, timestamp: Long) {
        writableDatabase.transaction {
            insertOrThrow("blocked_calls", null, ContentValues().apply {
                put("number", number.take(128))
                put("timestamp", timestamp)
            })
            prune(timestamp)
        }
        mutableRevision.update { it + 1 }
    }

    @Synchronized
    fun recent(nowMillis: Long): List<BlockedCall> {
        prune(nowMillis)
        return readableDatabase.query(
            "blocked_calls", arrayOf("id", "number", "timestamp"),
            null, null, null, null, "timestamp DESC, id DESC", "200",
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(BlockedCall(cursor.getLong(0), cursor.getString(1), cursor.getLong(2)))
                }
            }
        }
    }

    @Synchronized
    fun clear() {
        writableDatabase.delete("blocked_calls", null, null)
        mutableRevision.update { it + 1 }
    }

    private fun prune(nowMillis: Long) {
        writableDatabase.delete("blocked_calls", "timestamp < ?", arrayOf((nowMillis - RETENTION_MILLIS).toString()))
        writableDatabase.execSQL("DELETE FROM blocked_calls WHERE id NOT IN (SELECT id FROM blocked_calls ORDER BY timestamp DESC, id DESC LIMIT 200)")
    }

    private companion object { const val RETENTION_MILLIS = 30L * 24 * 60 * 60 * 1000 }
}
