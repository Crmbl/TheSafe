package com.crmbl.thesafe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class ItemRAdapter(private val dataSource : MutableList<File>) : RecyclerView.Adapter<ItemViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view : View = LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false)
        return ItemViewHolder(view)
    }

    override fun getItemCount(): Int {
        return dataSource.size
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val file : File = dataSource[position]
        holder.bind(file)
    }
}