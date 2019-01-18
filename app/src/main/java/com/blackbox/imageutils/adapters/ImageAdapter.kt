package com.blackbox.imageutils.adapters

import android.graphics.drawable.Drawable
import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blackbox.imageutils.R
import com.blackbox.imageutils.models.ImageItem
import com.blackbox.imageutils.utils.GlideApp
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import kotlinx.android.synthetic.main.list_item_image.view.*


class ImageAdapter : ListAdapter<ImageItem, ImageAdapter.ViewHolder>(ImageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(inflater.inflate(R.layout.list_item_image, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val pin = getItem(position)

        holder.apply {
            bind(pin)
            itemView.tag = pin
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(image: ImageItem) {
            val context = itemView.context

            //Load image
            GlideApp.with(context)
                .load(image.imagePath)
                .apply(
                    RequestOptions
                        .skipMemoryCacheOf(true)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                )
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                        e?.printStackTrace()
                        return false
                    }

                    override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {

                        return false
                    }
                })
                .into(itemView.imageView)
        }
    }
}