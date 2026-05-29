package com.votar.list

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
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

        // ডাটাবেস ইনিশিয়ালাইজ (প্রথমবার ওপেন হলে কপি হবে)
        dbHelper = DatabaseHelper(this)

        val etSearchQuery = findViewById<EditText>(R.id.etSearchQuery)
        val spinnerWard = findViewById<Spinner>(R.id.spinnerWard)
        val spinnerGender = findViewById<Spinner>(R.id.spinnerGender)
        val btnSearch = findViewById<Button>(R.id.btnSearch)
        val tvTotalResults = findViewById<TextView>(R.id.tvTotalResults)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        // রিসাইক্লার ভিউ সেটআপ
        adapter = ResultAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnSearch.setOnClickListener {
            val query = etSearchQuery.text.toString().trim()
            val ward = spinnerWard.selectedItem.toString()
            val gender = spinnerGender.selectedItem.toString()

            if (query.isEmpty()) {
                Toast.makeText(this, "দয়া করে খোঁজার জন্য কিছু লিখুন", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // সার্চ করা
            val results = dbHelper.searchVoters(query, gender, ward)

            if (results.isEmpty()) {
                tvTotalResults.text = "কোনো তথ্য পাওয়া যায়নি।"
                adapter.updateData(emptyList())
            } else {
                tvTotalResults.text = "সর্বমোট পাওয়া গেছে: ${results.size} জনের তথ্য"
                adapter.updateData(results)
            }
        }
    }
}