package com.votar.list

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.FileOutputStream

class DatabaseHelper(private val context: Context) : SQLiteOpenHelper(context, DB_NAME, null, 1) {

    companion object {
        private const val DB_NAME = "voter_database.db"
    }

    override fun onOpen(db: SQLiteDatabase?) {
        super.onOpen(db)
        db?.execSQL("CREATE TABLE IF NOT EXISTS bookmarks (id INTEGER PRIMARY KEY AUTOINCREMENT, file_name TEXT, page_num INTEGER, data TEXT, UNIQUE(file_name, page_num, data))")
    }

    // ব্যাকগ্রাউন্ড থ্রেড থেকে কল করার জন্য ফাংশন
    fun initDatabaseIfNeeded() {
        if (!checkDatabase()) {
            copyDatabase()
        }
    }

    private fun checkDatabase(): Boolean {
        val dbFile = context.getDatabasePath(DB_NAME)
        return dbFile.exists() && (dbFile.length() > 0)
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

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("CREATE TABLE IF NOT EXISTS bookmarks (id INTEGER PRIMARY KEY AUTOINCREMENT, file_name TEXT, page_num INTEGER, data TEXT, UNIQUE(file_name, page_num, data))")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}

    fun addBookmark(voter: VoterResult): Boolean {
        return try {
            val db = this.writableDatabase
            val values = android.content.ContentValues().apply {
                put("file_name", voter.fileName)
                put("page_num", voter.pageNum)
                put("data", voter.data)
            }
            val result = db.insertWithOnConflict("bookmarks", null, values, SQLiteDatabase.CONFLICT_IGNORE)
            result != -1L
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun removeBookmark(voter: VoterResult): Boolean {
        return try {
            val db = this.writableDatabase
            val rows = db.delete("bookmarks", "file_name = ? AND page_num = ? AND data = ?", arrayOf(voter.fileName, voter.pageNum.toString(), voter.data))
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getBookmarkedKeysSet(): Set<String> {
        val set = mutableSetOf<String>()
        try {
            val db = this.readableDatabase
            db.rawQuery("SELECT file_name || '_' || page_num || '_' || data FROM bookmarks", null).use { cursor ->
                if (cursor.moveToFirst()) {
                    do {
                        set.add(cursor.getString(0))
                    } while (cursor.moveToNext())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return set
    }

    fun getBookmarkedVoters(): List<VoterResult> {
        val results = mutableListOf<VoterResult>()
        try {
            val db = this.readableDatabase
            db.rawQuery("SELECT file_name, page_num, data FROM bookmarks ORDER BY id DESC", null).use { cursor ->
                if (cursor.moveToFirst()) {
                    do {
                        val fileName = cursor.getString(0)
                        val pageNum = cursor.getInt(1)
                        val data = cursor.getString(2)
                        results.add(VoterResult(fileName, pageNum, data, isBookmarked = true))
                    } while (cursor.moveToNext())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return results
    }

    fun searchVoters(
        query: String,
        father: String,
        mother: String,
        age: String,
        gender: String,
        ward: String
    ): List<VoterResult> {
        val results = mutableListOf<VoterResult>()
        val bookmarkedKeys = getBookmarkedKeysSet()

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
                val bengaliAge = convertToBengali(age)
                sql += " AND (clean_text LIKE ? OR raw_text LIKE ? OR clean_text LIKE ? OR raw_text LIKE ?)"
                args.add("%$age%")
                args.add("%$age%")
                args.add("%$bengaliAge%")
                args.add("%$bengaliAge%")
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

            db.rawQuery(sql, args.toTypedArray()).use { cursor ->
                if (cursor.moveToFirst()) {
                    do {
                        val fileName = cursor.getString(0)
                        val pageNum = cursor.getInt(1)
                        val cleanText = cursor.getString(2)

                        val voters = splitIntoIndividualVoters(cleanText)
                        for (voter in voters) {
                            if (isMatch(voter, query, father, mother, age)) {
                                val trimmed = voter.trim()
                                val key = "${fileName}_${pageNum}_${trimmed}"
                                results.add(
                                    VoterResult(
                                        fileName = fileName,
                                        pageNum = pageNum,
                                        data = trimmed,
                                        isBookmarked = bookmarkedKeys.contains(key)
                                    )
                                )
                            }
                        }
                    } while (cursor.moveToNext())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return results
    }

    fun clearOldDatabase() {
        val db = this.writableDatabase
        db.execSQL("DELETE FROM voters")
        db.close()
    }

    fun copyDatabaseFromAssets(context: Context) {
        val dbFile = context.getDatabasePath("voter_database.db")
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
        val bookmarkedKeys = getBookmarkedKeysSet()

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
                val bengaliAge = convertToBengali(age)
                sql += " AND (clean_text LIKE ? OR raw_text LIKE ? OR clean_text LIKE ? OR raw_text LIKE ?)"
                args.add("%$age%")
                args.add("%$age%")
                args.add("%$bengaliAge%")
                args.add("%$bengaliAge%")
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
                        val fileName = cursor.getString(0)
                        val pageNum = cursor.getInt(1)
                        val cleanText = cursor.getString(2)

                        val voters = splitIntoIndividualVoters(cleanText)
                        for (voter in voters) {
                            if (isMatch(voter, query, father, mother, age)) {
                                val trimmed = voter.trim()
                                val key = "${fileName}_${pageNum}_${trimmed}"
                                results.add(
                                    VoterResult(
                                        fileName = fileName,
                                        pageNum = pageNum,
                                        data = trimmed,
                                        isBookmarked = bookmarkedKeys.contains(key)
                                    )
                                )
                            }
                        }
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

        var cursor = db.rawQuery("SELECT COUNT(*) FROM voters", null)
        if (cursor.moveToFirst()) stats["total"] = cursor.getInt(0)
        cursor.close()

        cursor = db.rawQuery("SELECT COUNT(*) FROM voters WHERE gender = 'male'", null)
        if (cursor.moveToFirst()) stats["male"] = cursor.getInt(0)
        cursor.close()

        cursor = db.rawQuery("SELECT COUNT(*) FROM voters WHERE gender = 'female'", null)
        if (cursor.moveToFirst()) stats["female"] = cursor.getInt(0)
        cursor.close()

        return stats
    }

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

    private fun splitIntoIndividualVoters(text: String): List<String> {
        val replaced = text.replace(Regex("((?:[0-9০-৯]+\\s*[.|।\\-]?\\s*)?(?:নাম:|নামঃ|nam:?))", RegexOption.IGNORE_CASE), "|||\$1")
        return replaced.split("|||").filter { it.isNotBlank() && (it.contains("নাম", ignoreCase = true) || it.contains("nam", ignoreCase = true)) }
    }

    private fun isMatch(text: String, query: String, father: String, mother: String, age: String): Boolean {
        if (query.isNotEmpty() && !text.contains(query, ignoreCase = true)) return false
        if (father.isNotEmpty() && !text.contains(father, ignoreCase = true)) return false
        if (mother.isNotEmpty() && !text.contains(mother, ignoreCase = true)) return false
        
        if (age.isNotEmpty()) {
            val bengaliAge = convertToBengali(age)
            if (!text.contains(age) && !text.contains(bengaliAge)) return false
        }
        
        return true
    }

    private fun convertToBengali(input: String): String {
        val english = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
        val bengali = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        var result = input
        for (i in english.indices) {
            result = result.replace(english[i], bengali[i])
        }
        return result
    }
}
