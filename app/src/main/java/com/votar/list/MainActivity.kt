package com.votar.list

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var adapter: ResultAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DatabaseHelper(this)

        val loadingLayout = findViewById<LinearLayout>(R.id.loadingLayout)
        val mainLayout = findViewById<LinearLayout>(R.id.mainLayout)

        val etSearchQuery = findViewById<EditText>(R.id.etSearchQuery)
        val etFatherName = findViewById<EditText>(R.id.etFatherName)
        val etMotherName = findViewById<EditText>(R.id.etMotherName)
        val etAge = findViewById<EditText>(R.id.etAge)

        val spinnerWard = findViewById<Spinner>(R.id.spinnerWard)
        val spinnerGender = findViewById<Spinner>(R.id.spinnerGender)
        val btnSearch = findViewById<Button>(R.id.btnSearch)
        val tvTotalResults = findViewById<TextView>(R.id.tvTotalResults)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        adapter = ResultAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // অ্যাপ ওপেন হলে শুরুতে লোডিং অ্যানিমেশন দেখাবে
        loadingLayout.visibility = View.VISIBLE
        mainLayout.visibility = View.GONE

        // ব্যাকগ্রাউন্ড থ্রেডে ডাটাবেস কপি করা হচ্ছে (UI ক্র্যাশ করবে না)
        Thread {
            dbHelper.initDatabaseIfNeeded()

            // কপি শেষ হলে মেইন স্ক্রিন দৃশ্যমান হবে
            runOnUiThread {
                loadingLayout.visibility = View.GONE
                mainLayout.visibility = View.VISIBLE
            }
        }.start()

        btnSearch.setOnClickListener {
            val query = etSearchQuery.text.toString().trim()
            val father = etFatherName.text.toString().trim()
            val mother = etMotherName.text.toString().trim()
            val age = etAge.text.toString().trim()

            val ward = spinnerWard.selectedItem?.toString() ?: ""
            val gender = spinnerGender.selectedItem?.toString() ?: ""

            // সবগুলো বক্স খালি রেখে সার্চ দিলে সতর্ক করবে
            if (query.isEmpty() && father.isEmpty() && mother.isEmpty() && age.isEmpty()) {
                Toast.makeText(this, "খোঁজার জন্য অন্তত একটি তথ্য (নাম, পিতা, মাতা বা বয়স) লিখুন", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // সার্চ করার সময় ইউজারকে ওয়েট করানো হচ্ছে
            tvTotalResults.text = "অনুসন্ধান করা হচ্ছে..."
            btnSearch.isEnabled = false

            Thread {
                val results = dbHelper.searchVoters(query, father, mother, age, gender, ward)

                runOnUiThread {
                    btnSearch.isEnabled = true
                    if (results.isEmpty()) {
                        tvTotalResults.text = "কোনো তথ্য পাওয়া যায়নি।"
                        adapter.updateData(emptyList())
                    } else {
                        tvTotalResults.text = "সর্বমোট পাওয়া গেছে: ${results.size} জনের তথ্য"
                        adapter.updateData(results)
                    }
                }
            }.start()
        }
    }
}