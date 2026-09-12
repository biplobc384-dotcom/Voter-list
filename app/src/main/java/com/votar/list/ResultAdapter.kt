package com.votar.list

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class ResultAdapter(private var resultList: List<VoterResult>) : RecyclerView.Adapter<ResultAdapter.ViewHolder>() {

    private var highlights: List<String> = emptyList()

    var onCopyClick: ((VoterResult) -> Unit)? = null
    var onShareClick: ((VoterResult) -> Unit)? = null
    var onBookmarkClick: ((VoterResult, Int) -> Unit)? = null
    var onSlipClick: ((VoterResult) -> Unit)? = null

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvBadge: TextView = view.findViewById(R.id.tvBadge)
        val tvData: TextView = view.findViewById(R.id.tvData)
        val btnBookmark: ImageButton = view.findViewById(R.id.btnBookmark)
        val btnQuickCopy: TextView = view.findViewById(R.id.btnQuickCopy)
        val btnQuickShare: TextView = view.findViewById(R.id.btnQuickShare)
        val btnViewSlip: TextView = view.findViewById(R.id.btnViewSlip)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = resultList[position]
        
        val serial = item.getSerial()
        val voterId = item.getVoterId()
        val badgeParts = mutableListOf<String>()
        
        if (serial.isNotEmpty()) {
            badgeParts.add("ক্রমিক: $serial")
        }
        if (voterId.isNotEmpty()) {
            badgeParts.add("ভোটার নং: $voterId")
        }
        badgeParts.add("ফাইল: ${item.fileName}")
        badgeParts.add("পেজ: ${item.pageNum}")

        holder.tvBadge.text = badgeParts.joinToString(" | ")
        
        val fullText = item.data
        val spannable = SpannableString(fullText)

        for (query in highlights) {
            if (query.isEmpty()) continue
            
            var startPos = fullText.lowercase(Locale.getDefault()).indexOf(query.lowercase(Locale.getDefault()))
            while (startPos != -1) {
                val endPos = startPos + query.length
                spannable.setSpan(
                    BackgroundColorSpan(Color.YELLOW),
                    startPos,
                    endPos,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                startPos = fullText.lowercase(Locale.getDefault()).indexOf(query.lowercase(Locale.getDefault()), endPos)
            }
        }
        
        holder.tvData.text = spannable

        if (item.isBookmarked) {
            holder.btnBookmark.setImageResource(android.R.drawable.btn_star_big_on)
        } else {
            holder.btnBookmark.setImageResource(android.R.drawable.btn_star_big_off)
        }

        holder.btnBookmark.setOnClickListener {
            item.isBookmarked = !item.isBookmarked
            notifyItemChanged(position)
            onBookmarkClick?.invoke(item, position)
        }

        holder.btnQuickCopy.setOnClickListener {
            onCopyClick?.invoke(item)
        }

        holder.btnQuickShare.setOnClickListener {
            onShareClick?.invoke(item)
        }

        holder.btnViewSlip.setOnClickListener {
            onSlipClick?.invoke(item)
        }
    }

    override fun getItemCount() = resultList.size

    fun updateData(newData: List<VoterResult>, searchQueries: List<String> = emptyList()) {
        resultList = newData
        highlights = searchQueries.filter { it.isNotEmpty() }
        notifyDataSetChanged()
    }

    fun appendData(newItems: List<VoterResult>) {
        val startPosition = resultList.size
        resultList = resultList + newItems
        notifyItemRangeInserted(startPosition, newItems.size)
    }

    fun removeItemAt(position: Int) {
        if (position in resultList.indices) {
            val mutableList = resultList.toMutableList()
            mutableList.removeAt(position)
            resultList = mutableList
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, resultList.size - position)
        }
    }
}
