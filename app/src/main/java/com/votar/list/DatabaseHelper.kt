package com.votar.list

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.FileOutputStream

class DatabaseHelper(private val context: Context) : SQLiteOpenHelper(context, DB_NAME, null, 1) {

    companion object {
        private const val DB_NAME = "voter_database.db"
    }

    private val dbPath: String = context.getDatabasePath(DB_NAME).path

    init {
        // অ্যাপ ওপেন হলে চেক করবে ডাটাবেস আছে কিনা, না থাকলে কপি করবে
        if (!checkDatabase()) {
            copyDatabase()
        }
    }

    private fun checkDatabase(): Boolean {
        val dbFile = context.getDatabasePath(DB_NAME)
        return dbFile.exists()
    }

    private fun copyDatabase() {
        this.readableDatabase // লোকাল স্টোরেজে ফাঁকা ডাটাবেস ফাইল তৈরি করার জন্য
        context.assets.open(DB_NAME).use { inputStream ->
            FileOutputStream(dbPath).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase?) {}
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}

    fun searchVoters(query: String, gender: String, ward: String): List<VoterResult> {
        val results = mutableListOf<VoterResult>()
        val db = this.readableDatabase

        val searchPattern = "%$query%"
        var sql = "SELECT file_name, page_num, clean_text FROM voters WHERE (clean_text LIKE ? OR raw_text LIKE ?)"
        val args = mutableListOf(searchPattern, searchPattern)

        if (gender != "all") {
            sql += " AND gender = ?"
            args.add(gender)
        }
        if (ward != "all") {
            sql += " AND ward = ?"
            args.add(ward)
        }

        val cursor = db.rawQuery(sql, args.toTypedArray())

        if (cursor.moveToFirst()) {
            do {
                results.add(
                    VoterResult(
                        fileName = cursor.getString(0),
                        pageNum = cursor.getInt(1),
                        data = cursor.getString(2)
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return results
    }
}