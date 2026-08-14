package com.votar.list

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class ResultAdapter(private var resultList: List<VoterResult>) : RecyclerView.Adapter<ResultAdapter.ViewHolder>() {

    private var highlights: List<String> = emptyList()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvBadge: TextView = view.findViewById(R.id.tvBadge)
        val tvData: TextView = view.findViewById(R.id.tvData)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = resultList[position]
        holder.tvBadge.text = "ফাইল: ${item.fileName} | পেজ: ${item.pageNum}"
        
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
}