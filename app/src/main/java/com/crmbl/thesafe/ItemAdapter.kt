package com.crmbl.thesafe

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import android.widget.ProgressBar
import com.github.ybq.android.spinkit.style.CubeGrid


class ItemAdapter(_context : Context, private val dataSource : MutableList<File>) : BaseAdapter() {
    private val inflater : LayoutInflater = _context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val rowView = inflater.inflate(R.layout.list_item, parent, false)
        val titleView = rowView.findViewById<TextView>(R.id.textview_title)
        val extensionView = rowView.findViewById<TextView>(R.id.textview_ext)

        val file = getItem(position) as File
        val splitedName = file.originName.split('.')
        titleView.text = splitedName[0]
        extensionView.text = splitedName[1]
        file.position = position
        val progressBar = rowView.findViewById(R.id.spin_kit) as ProgressBar
        progressBar.visibility = View.VISIBLE
        progressBar.indeterminateDrawable = CubeGrid()

        return rowView
    }

    override fun getItem(position: Int): Any {
        return dataSource[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return dataSource.size
    }
}