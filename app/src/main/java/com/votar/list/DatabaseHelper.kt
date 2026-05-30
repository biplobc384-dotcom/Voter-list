package com.votar.list

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.FileOutputStream

class DatabaseHelper(private val context: Context) : SQLiteOpenHelper(context, DB_NAME, null, 1) {

    companion object {
        private const val DB_NAME = "voter_database.db"
    }

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
        val dbFile = context.getDatabasePath(DB_NAME)
        
        // বাগ ফিক্স ১: ফাঁকা ডাটাবেস তৈরি না করে ফোল্ডার তৈরি করা হচ্ছে যাতে ক্র্যাশ না করে
        if (dbFile.parentFile?.exists() == false) {
            dbFile.parentFile?.mkdirs()
        }

        context.assets.open(DB_NAME).use { inputStream ->
            FileOutputStream(dbFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase?) {}
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}

    fun searchVoters(query: String, gender: String, ward: String): List<VoterResult> {
        val results = mutableListOf<VoterResult>()
        
        // বাগ ফিক্স ২: Try-Catch ব্লক দেওয়া হলো যাতে যেকোনো কারণে অ্যাপ ক্র্যাশ না করে
        try {
            val db = this.readableDatabase
            val searchPattern = "%$query%"
            var sql = "SELECT file_name, page_num, clean_text FROM voters WHERE (clean_text LIKE ? OR raw_text LIKE ?)"
            val args = mutableListOf(searchPattern, searchPattern)

            // জেন্ডার ফিল্টার ফিক্স (বাংলা টেক্সট থেকে ডাটাবেসের male/female এ কনভার্ট)
            if (!gender.contains("সব") && !gender.contains("উভয়") && gender != "all") {
                sql += " AND gender = ?"
                val genderValue = if (gender.contains("পুরুষ") || gender.equals("male", true)) "male" else "female"
                args.add(genderValue)
            }
            
            // ওয়ার্ড ফিল্টার ফিক্স (ওয়ার্ডের বাংলা নাম থেকে শুধু নম্বরটি বের করা)
            if (!ward.contains("সব") && !ward.contains("All") && ward != "all") {
                sql += " AND ward = ?"
                // যদি স্পিনারে "ওয়ার্ড ৩" থাকে, তবে এটি শুধু "3" বের করে নেবে
                val wardNumber = ward.replace(Regex("[^0-9]"), "")
                args.add(if (wardNumber.isNotEmpty()) wardNumber else ward)
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return results
    }
}
