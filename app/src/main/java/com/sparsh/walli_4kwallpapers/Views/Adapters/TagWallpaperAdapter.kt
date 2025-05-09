package com.sparsh.walli_4kwallpapers.Views.Adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.sparsh.walli_4kwallpapers.R
import com.sparsh.walli_4kwallpapers.Data.Wallpaper
import com.sparsh.walli_4kwallpapers.Views.Activities.wallpaperSetActivity

class TagWallpaperAdapter(private var tagwallpaperList: MutableList<Wallpaper>) :
    RecyclerView.Adapter<TagWallpaperAdapter.WallpaperViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WallpaperViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image, parent, false)
        return WallpaperViewHolder(view)
    }

    override fun onBindViewHolder(holder: WallpaperViewHolder, position: Int) {
        val wallpaper = tagwallpaperList[position]

        Glide.with(holder.itemView.context)
            .load(wallpaper.url)
            .centerCrop()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(holder.wallpaperImageView)


        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, wallpaperSetActivity::class.java).apply {
                putExtra("imageUrl", wallpaper.url)
            }
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return tagwallpaperList.size
    }


    inner class WallpaperViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val wallpaperImageView: ImageView = itemView.findViewById(R.id.imageView)
    }
}
