package com.votar.list

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.FileOutputStream

class DatabaseHelper(private val context: Context) : SQLiteOpenHelper(context, DB_NAME, null, 1) {

    companion object {
        private const val DB_NAME = "voter_database.db"
    }

    // ব্যাকগ্রাউন্ড থ্রেড থেকে কল করার জন্য ফাংশন
    fun initDatabaseIfNeeded() {
        if (!checkDatabase()) {
            copyDatabase()
        }
    }

    private fun checkDatabase(): Boolean {
        val dbFile = context.getDatabasePath(DB_NAME)
        return dbFile.exists() && dbFile.length() > 0
    }

    private fun copyDatabase() {
        val dbFile = context.getDatabasePath(DB_NAME)

        dbFile.parentFile?.let { parent ->
            if (!parent.exists()) {
                parent.mkdirs()
            }
        }

        context.assets.open(DB_NAME).use { inputStream ->
            FileOutputStream(dbFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase?) {}
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}

    fun searchVoters(
        query: String,
        father: String,
        mother: String,
        age: String,
        gender: String,
        ward: String
    ): List<VoterResult> {
        val results = mutableListOf<VoterResult>()

        try {
            val db = this.readableDatabase

            // ডায়নামিক SQL তৈরি (১=১ দেওয়ার ফলে যেকোনো শর্ত ইচ্ছামতো জোড়া যায়)
            var sql = "SELECT file_name, page_num, clean_text FROM voters WHERE 1=1"
            val args = mutableListOf<String>()

            if (query.isNotEmpty()) {
                sql += " AND (clean_text LIKE ? OR raw_text LIKE ?)"
                args.add("%$query%")
                args.add("%$query%")
            }

            if (father.isNotEmpty()) {
                sql += " AND (clean_text LIKE ? OR raw_text LIKE ?)"
                args.add("%$father%")
                args.add("%$father%")
            }

            if (mother.isNotEmpty()) {
                sql += " AND (clean_text LIKE ? OR raw_text LIKE ?)"
                args.add("%$mother%")
                args.add("%$mother%")
            }

            if (age.isNotEmpty()) {
                sql += " AND (clean_text LIKE ? OR raw_text LIKE ?)"
                args.add("%$age%")
                args.add("%$age%")
            }

            // জেন্ডার ফিল্টার
            if (!gender.contains("সব") && !gender.contains("উভয়") && gender != "all") {
                sql += " AND gender = ?"
                val genderValue = if (gender.contains("পুরুষ") || gender.equals("male", true)) "male" else "female"
                args.add(genderValue)
            }

            // ওয়ার্ড ফিল্টার
            if (!ward.contains("সব") && !ward.contains("All") && ward != "all") {
                sql += " AND ward = ?"
                val wardNumber = ward.replace(Regex("[^0-9]"), "")
                args.add(if (wardNumber.isNotEmpty()) wardNumber else ward)
            }

            db.rawQuery(sql, args.toTypedArray()).use { cursor ->
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
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return results
    }
    // পুরনো সব ডাটা মুছে ফেলা
    fun clearOldDatabase() {
        val db = this.writableDatabase
        db.execSQL("DELETE FROM voters")
        db.close()
    }
    fun copyDatabaseFromAssets(context: Context) {
        val dbFile = context.getDatabasePath("voter_database.db") // আপনার ডাটাবেসের নাম
        if (!dbFile.exists()) {
            try {
                dbFile.parentFile?.mkdirs()
                context.assets.open("voter_database.db").use { inputStream ->
                    FileOutputStream(dbFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    fun searchVotersWithPagination(
        query: String, father: String, mother: String, age: String,
        gender: String, ward: String, limit: Int, offset: Int
    ): List<VoterResult> {
        val results = mutableListOf<VoterResult>()
        try {
            val db = this.readableDatabase
            var sql = "SELECT file_name, page_num, clean_text FROM voters WHERE 1=1"
            val args = mutableListOf<String>()

            if (query.isNotEmpty()) {
                sql += " AND (clean_text LIKE ? OR raw_text LIKE ?)"
                args.add("%$query%")
                args.add("%$query%")
            }
            if (father.isNotEmpty()) {
                sql += " AND (clean_text LIKE ? OR raw_text LIKE ?)"
                args.add("%$father%")
                args.add("%$father%")
            }
            if (mother.isNotEmpty()) {
                sql += " AND (clean_text LIKE ? OR raw_text LIKE ?)"
                args.add("%$mother%")
                args.add("%$mother%")
            }
            if (age.isNotEmpty()) {
                sql += " AND (clean_text LIKE ? OR raw_text LIKE ?)"
                args.add("%$age%")
                args.add("%$age%")
            }
            if (!gender.contains("সব") && !gender.contains("উভয়") && gender != "all") {
                sql += " AND gender = ?"
                val genderValue = if (gender.contains("পুরুষ") || gender.equals("male", true)) "male" else "female"
                args.add(genderValue)
            }
            if (!ward.contains("সব") && !ward.contains("All") && ward != "all") {
                sql += " AND ward = ?"
                val wardNumber = ward.replace(Regex("[^0-9]"), "")
                args.add(if (wardNumber.isNotEmpty()) wardNumber else ward)
            }

            sql += " LIMIT ? OFFSET ?"
            args.add(limit.toString())
            args.add(offset.toString())

            db.rawQuery(sql, args.toTypedArray()).use { cursor ->
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
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return results
    }
    fun getVoterAnalytics(): Map<String, Int> {
        val stats = mutableMapOf<String, Int>()
        val db = this.readableDatabase

        // মোট ভোটার সংখ্যা
        var cursor = db.rawQuery("SELECT COUNT(*) FROM voters", null)
        if (cursor.moveToFirst()) stats["total"] = cursor.getInt(0)
        cursor.close()

        // মোট পুরুষ ভোটার
        cursor = db.rawQuery("SELECT COUNT(*) FROM voters WHERE gender = 'পুরুষ'", null)
        if (cursor.moveToFirst()) stats["male"] = cursor.getInt(0)
        cursor.close()

        // মোট মহিলা ভোটার
        cursor = db.rawQuery("SELECT COUNT(*) FROM voters WHERE gender = 'মহিলা'", null)
        if (cursor.moveToFirst()) stats["female"] = cursor.getInt(0)
        cursor.close()

        return stats
    }

    // পিডিএফ থেকে পাওয়া নতুন ডাটা সেভ করা
    fun insertPdfData(fileName: String, pageNum: Int, rawText: String, cleanText: String) {
        val db = this.writableDatabase
        val values = android.content.ContentValues().apply {
            put("ward", "Uploaded PDF")
            put("file_name", fileName)
            put("page_num", pageNum)
            put("raw_text", rawText)
            put("clean_text", cleanText)
            put("gender", "all")
        }
        db.insert("voters", null, values)
        db.close()
    }
}