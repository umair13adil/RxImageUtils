package com.blackbox.imageutils.adapters


import android.support.v7.util.DiffUtil
import com.blackbox.imageutils.models.ImageItem

class ImageDiffCallback : DiffUtil.ItemCallback<ImageItem>() {

    override fun areItemsTheSame(oldItem: ImageItem, newItem: ImageItem): Boolean {
        return oldItem.imagePath == newItem.imagePath

    }

    override fun areContentsTheSame(oldItem: ImageItem, newItem: ImageItem): Boolean {
        return oldItem.imagePath == newItem.imagePath
    }
}