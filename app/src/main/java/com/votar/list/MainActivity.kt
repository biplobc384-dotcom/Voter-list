package com.votar.list

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import java.io.FileOutputStream
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileWriter


class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var adapter: ResultAdapter

    private var currentPage = 0
    private val pageSize = 50
    private var isLoading = false
    private var isLastPage = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        PDFBoxResourceLoader.init(applicationContext)
        dbHelper = DatabaseHelper(this)

        val layoutSearch = findViewById<LinearLayout>(R.id.layoutSearch)
        val layoutUpload = findViewById<LinearLayout>(R.id.layoutUpload)
        val layoutDevInfo = findViewById<LinearLayout>(R.id.layoutDevInfo)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        // নিচের মেনু কন্ট্রোল
        bottomNav.setOnItemSelectedListener { item ->
            layoutSearch.visibility = View.GONE
            layoutUpload.visibility = View.GONE
            layoutDevInfo.visibility = View.GONE

            when (item.itemId) {
                R.id.nav_search -> layoutSearch.visibility = View.VISIBLE
                R.id.nav_upload -> layoutUpload.visibility = View.VISIBLE
                R.id.nav_dev -> layoutDevInfo.visibility = View.VISIBLE
            }
            true
        }

        // --- সার্চ অপশন লজিক (পুনরুদ্ধার করা ফিল্টার সহ) ---
        val etSearchQuery = findViewById<EditText>(R.id.etSearchQuery)
        val etFatherName = findViewById<EditText>(R.id.etFatherName)
        val etMotherName = findViewById<EditText>(R.id.etMotherName)
        val etAge = findViewById<EditText>(R.id.etAge)
        val spinnerWard = findViewById<Spinner>(R.id.spinnerWard)
        val spinnerGender = findViewById<Spinner>(R.id.spinnerGender)

        val btnSearch = findViewById<Button>(R.id.btnSearch)
        val btnUploadDb = findViewById<Button>(R.id.btnUploadDb)
        val tvTotalResults = findViewById<TextView>(R.id.tvTotalResults)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        adapter = ResultAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnSearch.setOnClickListener {
            val query = etSearchQuery.text.toString().trim()
            val fatherName = etFatherName.text.toString().trim()
            val motherName = etMotherName.text.toString().trim()
            val age = etAge.text.toString().trim()

            val ward = spinnerWard.selectedItem?.toString() ?: "all"
            val gender = spinnerGender.selectedItem?.toString() ?: "all"

            if (query.isEmpty() && fatherName.isEmpty() && motherName.isEmpty() && age.isEmpty()) {
                Toast.makeText(this, "অনুসন্ধানের জন্য অন্তত একটি তথ্য দিন", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            //  নতুন ফিল্টারগুলো ডাটবেস হেল্পারে পাঠানো হচ্ছে
            val results = dbHelper.searchVoters(query, fatherName, motherName, age, ward, gender)
            tvTotalResults.text = "পাওয়া গেছে: ${results.size} জন"
            adapter.updateData(results)
        }
        val dbPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { selectedUri ->
                val currentDBPath = getDatabasePath("voter_database.db")
                contentResolver.openInputStream(selectedUri)?.use { input ->
                    FileOutputStream(currentDBPath).use { output ->
                        input.copyTo(output)
                    }
                }
                Toast.makeText(this, "ডাটাবেস সফলভাবে ইম্পোর্ট হয়েছে!", Toast.LENGTH_SHORT).show()
            }
        }

        // বাটন ক্লিক করলে:
        btnUploadDb.setOnClickListener {
            dbPicker.launch("application/octet-stream") // অথবা "*/*"
        }
        // --- পিডিএফ আপলোড লজিক ---
        val uploadProgress = findViewById<ProgressBar>(R.id.uploadProgress)
        val tvProgressText = findViewById<TextView>(R.id.tvProgressText)

        val pdfPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { selectedUri ->
                uploadProgress.visibility = View.VISIBLE
                tvProgressText.visibility = View.VISIBLE
                tvProgressText.text = "পিডিএফ পড়া হচ্ছে, অপেক্ষা করুন..."

                Thread {
                    try {
                        val inputStream = contentResolver.openInputStream(selectedUri)
                        val document = PDDocument.load(inputStream)
                        val stripper = PDFTextStripper()

                        dbHelper.clearOldDatabase() // পুরনো ডাটা ডিলিট

                        for (i in 1..document.numberOfPages) {
                            stripper.startPage = i
                            stripper.endPage = i
                            val rawText = stripper.getText(document)
                            val cleanText = BengaliCleaner.clean(rawText)

                            dbHelper.insertPdfData("Uploaded_File.pdf", i, rawText, cleanText)

                            runOnUiThread {
                                tvProgressText.text = "প্রসেস হচ্ছে: পৃষ্ঠা $i / ${document.numberOfPages}"
                            }
                        }
                        document.close()

                        runOnUiThread {
                            uploadProgress.visibility = View.GONE
                            tvProgressText.text = "✅ সফলভাবে নতুন পিডিএফ ডাটাবেসে সেভ হয়েছে!"
                            Toast.makeText(this, "আপডেট সম্পন্ন!", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            uploadProgress.visibility = View.GONE
                            tvProgressText.text = "❌ সমস্যা হয়েছে: ${e.message}"
                        }
                    }
                }.start()
            }
        }

        findViewById<Button>(R.id.btnSelectPdf).setOnClickListener {
            pdfPicker.launch("application/pdf")
        }

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (!isLoading && !isLastPage) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                        && firstVisibleItemPosition >= 0) {
                        loadNextPage()
                    }
                }
            }
        })

        // --- ডেভেলপার সোশ্যাল লিংক লজিক ---
        findViewById<Button>(R.id.btnTelegram).setOnClickListener { openLink("https://t.me/ArifurHackworld") }
        findViewById<Button>(R.id.btnDiscord).setOnClickListener { openLink("https://discord.gg/jqFHknCmN") }
        findViewById<Button>(R.id.btnGithub).setOnClickListener { openLink("https://github.com/biplobc384-dotcom/biplobc384-dotcom") }
        findViewById<Button>(R.id.btnFb).setOnClickListener { openLink("https://www.facebook.com/fary.pol") }
        findViewById<Button>(R.id.btnWhatsapp).setOnClickListener { openLink("https://wa.me/8801799517156") }
    }

    private fun openLink(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun loadNextPage() {
        isLoading = true
        val offset = currentPage * pageSize

        Thread {
            val query = ""
            val father = ""
            val mother = ""
            val age = ""
            val ward = "all"
            val gender = "all"
            val nextResults = dbHelper.searchVotersWithPagination(query, father, mother, age, ward, gender, pageSize, offset)
            runOnUiThread {
                if (nextResults.isEmpty()) {
                    isLastPage = true
                } else {
                    if (currentPage == 0) {
                        adapter.updateData(nextResults)
                    } else {
                        adapter.appendData(nextResults)
                    }
                    currentPage++
                }
                isLoading = false
            }
        }.start()
    }

    private fun exportAndShareResults(results: List<VoterResult>) {
        if (results.isEmpty()) {
            Toast.makeText(this, "শেয়ার করার মতো কোনো তথ্য নেই", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val exportDir = File(cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val file = File(exportDir, "voter_search_results.csv")
            val writer = FileWriter(file)

            writer.append("ফাইল,পেজ,তথ্য\n")

            for (voter in results) {
                writer.append("${voter.fileName},${voter.pageNum},${voter.data.replace("\n", " ")}\n")
            }
            writer.flush()
            writer.close()

            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "ভোটার অনুসন্ধানের ফলাফল")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "ফলাফলটি শেয়ার করুন"))

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "এক্সপোর্ট করতে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show()
        }
    }
}
