package com.crmbl.thesafe

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.*
import pl.droidsonroids.gif.GifDrawable
import pl.droidsonroids.gif.GifImageView


//TODO Gif getting slower and slower... NOT GOOD ! Might need to change for RecycleView?
//TODO Using convertView, needs to remove the marginTop in some way. Must find another way of doing it
class ItemAdapter(_context : Context, private val dataSource : MutableList<File>) : BaseAdapter() {
    private val inflater : LayoutInflater = _context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val context : Context = _context

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val rowView = convertView ?: inflater.inflate(R.layout.list_item, parent, false)
        rowView.alpha = 0f
        val titleView = rowView.findViewById<TextView>(R.id.textview_title)
        val extensionView = rowView.findViewById<TextView>(R.id.textview_ext)
        val file = getItem(position) as File
        val splitedName = file.originName.split('.')

        val mediaView : View
        if (splitedName[1].toLowerCase() == "gif" /*|| todo add other image ext */) {
            val gifDrawable = GifDrawable(file.decrypted!!)
            mediaView = rowView.findViewById<GifImageView>(R.id.imageView)
            mediaView.setImageDrawable(gifDrawable)
            gifDrawable.start()
        } else {
            mediaView = rowView.findViewById<VideoView>(R.id.videoView)
//            (mediaView as VideoView).setV
        }

        mediaView.visibility = View.VISIBLE
        titleView.text = splitedName[0]
        extensionView.text = splitedName[1]
        file.position = position

        //region height management

        if (file.height != 0) {
            if (position == 0) {
                val params = mediaView.layoutParams as RelativeLayout.LayoutParams
                val offset = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 42f, context.resources.displayMetrics).toInt()
                params.topMargin = offset
                mediaView.layoutParams = params
            }
            mediaView.minimumHeight = file.height
            rowView.minimumHeight = file.totalHeight
        }

        rowView.viewTreeObserver.addOnPreDrawListener (object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                val linearLayout : LinearLayout = rowView.findViewById(R.id.bottom_layout)
                if (rowView.minimumHeight == 0 && mediaView.minimumHeight == 0 && file.height == 0) {
                    if (position == 0) {
                        val params = mediaView.layoutParams as RelativeLayout.LayoutParams
                        val offset = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 42f, context.resources.displayMetrics).toInt()
                        params.topMargin = offset
                        mediaView.layoutParams = params
                        mediaView.minimumHeight = mediaView.height + offset
                        rowView.minimumHeight = mediaView.height + linearLayout.height + offset
                    } else {
                        rowView.minimumHeight = mediaView.height + linearLayout.height
                        mediaView.minimumHeight = mediaView.height
                    }

                    file.height = mediaView.minimumHeight
                    file.totalHeight = rowView.minimumHeight
                }

                return true
            }
        })

        rowView.animate().alpha(1f).setDuration(125).withEndAction {
            rowView.alpha = 1f
        }.start()

        //endregion height management

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