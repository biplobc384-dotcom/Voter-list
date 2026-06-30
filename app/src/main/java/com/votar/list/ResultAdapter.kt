package com.votar.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ResultAdapter(private var resultList: List<VoterResult>) : RecyclerView.Adapter<ResultAdapter.ViewHolder>() {

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
        holder.tvData.text = item.data
    }

    override fun getItemCount() = resultList.size

    fun updateData(newData: List<VoterResult>) {
        resultList = newData
        notifyDataSetChanged()
    }
    fun appendData(newItems: List<VoterResult>) {
        val startPosition = resultList.size
        resultList = resultList + newItems
        notifyItemRangeInserted(startPosition, newItems.size)
    }
}