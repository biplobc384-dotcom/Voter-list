package com.votar.list

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter

class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var adapter: ResultAdapter
    private lateinit var bookmarksAdapter: ResultAdapter

    private var currentResults: List<VoterResult> = emptyList()
    private var currentPage = 0
    private val pageSize = 50
    private var isLoading = false
    private var isLastPage = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mainRoot = findViewById<View>(R.id.mainRoot)
        if (mainRoot != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainRoot) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        PDFBoxResourceLoader.init(applicationContext)
        dbHelper = DatabaseHelper(this)

        Thread {
            dbHelper.initDatabaseIfNeeded()
        }.start()

        val btnToggleTheme = findViewById<ImageButton>(R.id.btnToggleTheme)
        btnToggleTheme?.setOnClickListener {
            val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
            if (currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }

        val layoutSearch = findViewById<LinearLayout>(R.id.layoutSearch)
        val layoutBookmarks = findViewById<LinearLayout>(R.id.layoutBookmarks)
        val layoutUpload = findViewById<LinearLayout>(R.id.layoutUpload)
        val layoutDevInfo = findViewById<LinearLayout>(R.id.layoutDevInfo)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        val tvEmptyBookmarks = findViewById<TextView>(R.id.tvEmptyBookmarks)
        val recyclerBookmarks = findViewById<RecyclerView>(R.id.recyclerBookmarks)

        // Bookmarks Adapter Setup
        bookmarksAdapter = ResultAdapter(emptyList())
        recyclerBookmarks.layoutManager = LinearLayoutManager(this)
        recyclerBookmarks.adapter = bookmarksAdapter

        setupAdapterListeners(bookmarksAdapter, isBookmarkTab = true, tvEmptyBookmarks = tvEmptyBookmarks)

        // Search Adapter Setup
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        adapter = ResultAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        setupAdapterListeners(adapter, isBookmarkTab = false)

        // Navigation Switch with Fade Transition
        bottomNav.setOnItemSelectedListener { item ->
            fun animateVisibility(view: View, visible: Boolean) {
                if (visible) {
                    view.alpha = 0f
                    view.visibility = View.VISIBLE
                    view.animate().alpha(1f).setDuration(200).start()
                } else {
                    view.visibility = View.GONE
                }
            }

            animateVisibility(layoutSearch, item.itemId == R.id.nav_search)
            animateVisibility(layoutBookmarks, item.itemId == R.id.nav_bookmarks)
            animateVisibility(layoutUpload, item.itemId == R.id.nav_upload)
            animateVisibility(layoutDevInfo, item.itemId == R.id.nav_dev)

            if (item.itemId == R.id.nav_bookmarks) {
                loadBookmarks(tvEmptyBookmarks, recyclerBookmarks)
            }
            true
        }

        // Search Filters
        val etSearchQuery = findViewById<EditText>(R.id.etSearchQuery)
        val etFatherName = findViewById<EditText>(R.id.etFatherName)
        val etMotherName = findViewById<EditText>(R.id.etMotherName)
        val etAge = findViewById<EditText>(R.id.etAge)
        val spinnerWard = findViewById<Spinner>(R.id.spinnerWard)
        val spinnerGender = findViewById<Spinner>(R.id.spinnerGender)

        val btnSearch = findViewById<Button>(R.id.btnSearch)
        val btnExportCsv = findViewById<Button>(R.id.btnExportCsv)
        val btnUploadDb = findViewById<Button>(R.id.btnUploadDb)
        val tvTotalResults = findViewById<TextView>(R.id.tvTotalResults)

        btnExportCsv.setOnClickListener {
            exportAndShareResults(currentResults)
        }

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

            currentPage = 0
            isLastPage = false

            val searchQueries = listOf(query, fatherName, motherName, age)

            val results = dbHelper.searchVoters(query, fatherName, motherName, age, gender, ward)
            currentResults = results
            tvTotalResults.text = "পাওয়া গেছে: ${results.size} জন"

            if (results.isNotEmpty()) {
                btnExportCsv.visibility = View.VISIBLE
            } else {
                btnExportCsv.visibility = View.GONE
            }

            adapter.updateData(results, searchQueries)
            recyclerView.scheduleLayoutAnimation()
            isLastPage = true
        }

        findViewById<ImageButton>(R.id.btnShowStats).setOnClickListener {
            showStatsDialog()
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

        btnUploadDb.setOnClickListener {
            dbPicker.launch("application/octet-stream")
        }

        // PDF Upload
        val cbClearOldData = findViewById<CheckBox>(R.id.cbClearOldData)
        val uploadProgress = findViewById<ProgressBar>(R.id.uploadProgress)
        val tvProgressText = findViewById<TextView>(R.id.tvProgressText)

        val pdfPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { selectedUri ->
                uploadProgress.visibility = View.VISIBLE
                tvProgressText.visibility = View.VISIBLE
                tvProgressText.text = "পিডিএফ পড়া হচ্ছে, অপেক্ষা করুন..."

                val clearOld = cbClearOldData.isChecked

                Thread {
                    try {
                        val inputStream = contentResolver.openInputStream(selectedUri)
                        val document = PDDocument.load(inputStream)
                        val stripper = PDFTextStripper()

                        if (clearOld) {
                            dbHelper.clearOldDatabase()
                        }

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
                        loadNextPage(recyclerView)
                    }
                }
            }
        })

        // Developer links
        findViewById<Button>(R.id.btnTelegram).setOnClickListener { openLink("https://t.me/ArifurHackworld") }
        findViewById<Button>(R.id.btnDiscord).setOnClickListener { openLink("https://discord.gg/jqFHknCmN") }
        findViewById<Button>(R.id.btnGithub).setOnClickListener { openLink("https://github.com/biplobc384-dotcom/biplobc384-dotcom") }
        findViewById<Button>(R.id.btnFb).setOnClickListener { openLink("https://www.facebook.com/fary.pol") }
        findViewById<Button>(R.id.btnWhatsapp).setOnClickListener { openLink("https://wa.me/8801799517156") }
    }

    private fun setupAdapterListeners(
        targetAdapter: ResultAdapter,
        isBookmarkTab: Boolean,
        tvEmptyBookmarks: TextView? = null
    ) {
        targetAdapter.onCopyClick = { voter ->
            copyToClipboard(voter.data)
        }

        targetAdapter.onShareClick = { voter ->
            shareText(voter.data)
        }

        targetAdapter.onBookmarkClick = { voter, position ->
            if (voter.isBookmarked) {
                dbHelper.addBookmark(voter)
                Toast.makeText(this, "সংরক্ষণ করা হয়েছে!", Toast.LENGTH_SHORT).show()
            } else {
                dbHelper.removeBookmark(voter)
                Toast.makeText(this, "সংরক্ষণ থেকে সরানো হয়েছে!", Toast.LENGTH_SHORT).show()
                if (isBookmarkTab) {
                    targetAdapter.removeItemAt(position)
                    if (targetAdapter.itemCount == 0) {
                        tvEmptyBookmarks?.visibility = View.VISIBLE
                    }
                }
            }
        }

        targetAdapter.onSlipClick = { voter ->
            showVoterSlipDialog(voter)
        }
    }

    private fun loadBookmarks(tvEmptyBookmarks: TextView, recyclerBookmarks: RecyclerView) {
        val bookmarks = dbHelper.getBookmarkedVoters()
        if (bookmarks.isEmpty()) {
            tvEmptyBookmarks.visibility = View.VISIBLE
            bookmarksAdapter.updateData(emptyList())
        } else {
            tvEmptyBookmarks.visibility = View.GONE
            bookmarksAdapter.updateData(bookmarks)
            recyclerBookmarks.scheduleLayoutAnimation()
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Voter Data", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "তথ্য কপি হয়েছে!", Toast.LENGTH_SHORT).show()
    }

    private fun shareText(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, "শেয়ার করুন"))
    }

    private fun showVoterSlipDialog(voter: VoterResult) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_voter_slip, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation

        val serial = voter.getSerial()
        val name = voter.getName().ifEmpty { voter.data }
        val voterId = voter.getVoterId()
        val father = voter.getFatherName()
        val mother = voter.getMotherName()
        val professionDob = voter.getProfessionAndDob()
        val address = voter.getAddress()

        dialogView.findViewById<TextView>(R.id.tvSlipSerial).text = "ক্রমিক নম্বর: ${if (serial.isNotEmpty()) serial else "N/A"}"
        dialogView.findViewById<TextView>(R.id.tvSlipFilePage).text = "ফাইল: ${voter.fileName} | পেজ: ${voter.pageNum}"
        dialogView.findViewById<TextView>(R.id.tvSlipName).text = "নাম: $name"
        dialogView.findViewById<TextView>(R.id.tvSlipVoterId).text = "ভোটার নম্বর: ${if (voterId.isNotEmpty()) voterId else "N/A"}"
        dialogView.findViewById<TextView>(R.id.tvSlipFather).text = "পিতা: ${if (father.isNotEmpty()) father else "N/A"}"
        dialogView.findViewById<TextView>(R.id.tvSlipMother).text = "মাতা: ${if (mother.isNotEmpty()) mother else "N/A"}"
        dialogView.findViewById<TextView>(R.id.tvSlipProfessionDob).text = if (professionDob.isNotEmpty()) professionDob else "পেশা/জন্ম তারিখ: N/A"
        dialogView.findViewById<TextView>(R.id.tvSlipAddress).text = "ঠিকানা: ${if (address.isNotEmpty()) address else "N/A"}"

        val fullSlipText = """
            🗳️ ভোটার স্লিপ
            ক্রমিক নম্বর: ${if (serial.isNotEmpty()) serial else "N/A"}
            নাম: $name
            ভোটার নম্বর: ${if (voterId.isNotEmpty()) voterId else "N/A"}
            পিতা: ${if (father.isNotEmpty()) father else "N/A"}
            মাতা: ${if (mother.isNotEmpty()) mother else "N/A"}
            ${if (professionDob.isNotEmpty()) professionDob else "পেশা/জন্ম তারিখ: N/A"}
            ঠিকানা: ${if (address.isNotEmpty()) address else "N/A"}
            ফাইল: ${voter.fileName} | পেজ: ${voter.pageNum}
        """.trimIndent()

        dialogView.findViewById<Button>(R.id.btnSlipCopy).setOnClickListener {
            copyToClipboard(fullSlipText)
        }

        dialogView.findViewById<Button>(R.id.btnSlipShare).setOnClickListener {
            shareText(fullSlipText)
        }

        dialogView.findViewById<Button>(R.id.btnSlipClose).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun openLink(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun showStatsDialog() {
        val stats = dbHelper.getVoterAnalytics()
        val total = stats["total"] ?: 0
        if (total == 0) {
            Toast.makeText(this, "পর্যাপ্ত ডাটা নেই", Toast.LENGTH_SHORT).show()
            return
        }

        val maleCount = stats["male"] ?: 0
        val femaleCount = stats["female"] ?: 0

        val dialogView: View = layoutInflater.inflate(R.layout.dialog_stats, null)
        val pieChart = dialogView.findViewById<PieChart>(R.id.pieChart)

        val entries = mutableListOf<PieEntry>()
        entries.add(PieEntry(maleCount.toFloat(), "পুরুষ"))
        entries.add(PieEntry(femaleCount.toFloat(), "মহিলা"))

        val dataSet = PieDataSet(entries, "ভোটার পরিসংখ্যান")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextSize = 16f

        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.description.isEnabled = false
        pieChart.centerText = "মোট: $total"
        pieChart.animateY(1000)

        val dialog = AlertDialog.Builder(this)
            .setTitle("ভোটার পরিসংখ্যান")
            .setView(dialogView)
            .setPositiveButton("ঠিক আছে", null)
            .create()

        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialog.show()
    }

    private fun loadNextPage(recyclerView: RecyclerView) {
        isLoading = true
        val offset = currentPage * pageSize

        val query = findViewById<EditText>(R.id.etSearchQuery).text.toString().trim()
        val father = findViewById<EditText>(R.id.etFatherName).text.toString().trim()
        val mother = findViewById<EditText>(R.id.etMotherName).text.toString().trim()
        val age = findViewById<EditText>(R.id.etAge).text.toString().trim()
        val ward = findViewById<Spinner>(R.id.spinnerWard).selectedItem?.toString() ?: "all"
        val gender = findViewById<Spinner>(R.id.spinnerGender).selectedItem?.toString() ?: "all"

        Thread {
            val nextResults = dbHelper.searchVotersWithPagination(query, father, mother, age, gender, ward, pageSize, offset)
            runOnUiThread {
                if (nextResults.isEmpty()) {
                    isLastPage = true
                } else {
                    if (currentPage == 0) {
                        val searchQueries = listOf(query, father, mother, age)
                        adapter.updateData(nextResults, searchQueries)
                        recyclerView.scheduleLayoutAnimation()
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
