package com.crmbl.thesafe

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.util.DisplayMetrics




class ItemAdapter(_context : Context, private val dataSource : MutableList<File>) : BaseAdapter() {
    private val inflater : LayoutInflater = _context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val context : Context = _context

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val rowView = inflater.inflate(R.layout.list_item, parent, false)
        val titleView = rowView.findViewById<TextView>(R.id.textview_title)
        val extensionView = rowView.findViewById<TextView>(R.id.textview_ext)
//        val imageView : ImageView = rowView.findViewById(R.id.imageView)
//        val bottomLayout : LinearLayout = rowView.findViewById(R.id.bottom_layout)

        val file = getItem(position) as File
        val splitedName = file.originName.split('.')
        titleView.text = splitedName[0]
        extensionView.text = splitedName[1]
        file.position = position

        val imageView : ImageView = rowView.findViewById(R.id.imageView)
        val bitMap = BitmapFactory.decodeByteArray(file.decrypted, 0, file.decrypted?.size!!)
        imageView.setImageBitmap(bitMap)
        Log.d("BitmapSize", "${bitMap.width} && ${bitMap.height}")

        //rowView.viewTreeObserver.addOnGlobalLayoutListener { drawShiiiit(rowView, position, bitMap.width) }
//        val params = rowView.layoutParams
//        Log.d("INFO", "ImageView Height : ${imageView.height} | BottomLayout Height : ${bottomLayout.height}")
//        params.height = imageView.measuredHeight - bottomLayout.measuredHeight
//        rowView.layoutParams = params

        return rowView
    }

    private fun drawShiiiit(view : View, position : Int, bitmapWidth : Int) {
        if (position == 0) {
            //TODO add margin for chipgroup to be "visible"
//            val params = rowView.layoutParams as AbsListView.LayoutParams
//            params. = TypedValue.COMPLEX_UNIT_DIP * 50
//            rowView.layoutParams = params
        }

        val imageView : ImageView = view.findViewById(R.id.imageView)

        val metrics = context.resources.displayMetrics
        val delta : Float = imageView.width.toFloat() / bitmapWidth.toFloat()
        //val delta2 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, imageView.width.toFloat(), metrics) / TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, bitmapWidth.toFloat(), metrics)

        Log.d("Testing", "${imageView.width} && $delta | ${imageView.height} && ${imageView.minimumHeight}")
        if (imageView.minimumHeight == imageView.height) return
        imageView.minimumHeight = imageView.height * delta.toInt()
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