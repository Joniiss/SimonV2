package com.app.simon.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.app.simon.FullscreenImageDialog
import com.app.simon.R
import com.bumptech.glide.Glide

class ImagePagerAdapter(
    private val context: Context,
    private val images: List<Uri>
) : RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivImage: ImageView = itemView.findViewById(R.id.ivItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_image_pager, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val uri = images[position]
        val width = holder.ivImage.width
        val height = holder.ivImage.height
        Glide.with(context)
            .load(uri)
            .override(width, height)
            .fitCenter()
            .into(holder.ivImage)



//        holder.ivImage.setOnClickListener {
//            val dialog = FullscreenImageDialog.newInstance(images[position].toString())
//            dialog.show((context as AppCompatActivity).supportFragmentManager, "fullscreen")
//        }
    }



    override fun getItemCount(): Int = images.size
}
