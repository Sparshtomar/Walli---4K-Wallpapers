package com.sparsh.walli_4kwallpapers.Views.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.sparsh.walli_4kwallpapers.R

class FavoritesAdapter(
    private val context: Context,
    private val imageUrls: MutableList<String>,
    private val listener3: OnItemClickListener3
) : RecyclerView.Adapter<FavoritesAdapter.ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoritesAdapter.ImageViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_favorites_frag, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageUrl = imageUrls[position]
        holder.itemView.setOnClickListener {
            listener3.onImageClick3(imageUrl)
        }
        val requestOptions = RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL)
        Glide.with(context)
            .load(imageUrl)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(holder.imageView)
            .apply { requestOptions }

    }

    override fun getItemCount(): Int {
        return imageUrls.size
    }

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
    }

    fun updateData(newImageUrls: List<String>) {
        imageUrls.clear() // Clear the existing list
        imageUrls.addAll(newImageUrls) // Add new data
        notifyDataSetChanged() // Notify adapter that dataset has changed
    }
}

interface OnItemClickListener3 {
    fun onImageClick3(imageUrls: String)
}
