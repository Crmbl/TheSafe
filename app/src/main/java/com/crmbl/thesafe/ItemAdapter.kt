package com.crmbl.thesafe

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.*


class ItemAdapter(_context : Context, private val dataSource : MutableList<File>) : BaseAdapter() {
    private val inflater : LayoutInflater = _context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val context : Context = _context

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val rowView = inflater.inflate(R.layout.list_item, parent, false)
        rowView.alpha = 0f
        val titleView = rowView.findViewById<TextView>(R.id.textview_title)
        val extensionView = rowView.findViewById<TextView>(R.id.textview_ext)
        val imageView : ImageView = rowView.findViewById(R.id.imageView)

        val file = getItem(position) as File
        val splitedName = file.originName.split('.')
        val bitMap = BitmapFactory.decodeByteArray(file.decrypted, 0, file.decrypted?.size!!)

        titleView.text = splitedName[0]
        extensionView.text = splitedName[1]
        file.position = position
        imageView.setImageBitmap(bitMap)

        if (file.height != 0) {
            if (position == 0) {
                val params = imageView.layoutParams as RelativeLayout.LayoutParams
                val offset = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 42f, context.resources.displayMetrics).toInt()
                params.topMargin = offset
                imageView.layoutParams = params
            }
            imageView.minimumHeight = file.height
            rowView.minimumHeight = file.totalHeight
        }

        rowView.viewTreeObserver.addOnPreDrawListener (object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                val linearLayout : LinearLayout = rowView.findViewById(R.id.bottom_layout)
                if (rowView.minimumHeight == 0 && imageView.minimumHeight == 0 && file.height == 0) {
                    if (position == 0) {
                        val params = imageView.layoutParams as RelativeLayout.LayoutParams
                        val offset = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 42f, context.resources.displayMetrics).toInt()
                        params.topMargin = offset
                        imageView.layoutParams = params
                        imageView.minimumHeight = imageView.height + offset
                        rowView.minimumHeight = imageView.height + linearLayout.height + offset
                    } else {
                        rowView.minimumHeight = imageView.height + linearLayout.height
                        imageView.minimumHeight = imageView.height
                    }

                    file.height = imageView.minimumHeight
                    file.totalHeight = rowView.minimumHeight
                }

                return true
            }
        })

        rowView.animate().alpha(1f).setDuration(125).withEndAction {
            rowView.alpha = 1f
        }.start()
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