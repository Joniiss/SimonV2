package com.app.simon.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.simon.R
import com.app.simon.data.ChatChannelItem
import java.text.DateFormat

class ChatsAdapter(
    private val onClick: (ChatChannelItem) -> Unit
) : ListAdapter<ChatChannelItem, ChatsAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ChatChannelItem>() {
            override fun areItemsTheSame(old: ChatChannelItem, new: ChatChannelItem) =
                old.channelId == new.channelId
            override fun areContentsTheSame(old: ChatChannelItem, new: ChatChannelItem) =
                old == new
        }
    }

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val ivAvatar: ImageView = v.findViewById(R.id.ivAvatar)
        val tvTitle: TextView = v.findViewById(R.id.tvTitle)
        val tvSubtitle: TextView = v.findViewById(R.id.tvSubtitle)
        val tvTime: TextView = v.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_channel, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.tvTitle.text = item.title
        holder.tvSubtitle.text = item.lastMessage

        val time = item.lastMessageAt?.toDate()?.let {
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(it)
        } ?: ""
        holder.tvTime.text = time

        // Se quiser carregar foto com Glide/Picasso:
        // Glide.with(holder.itemView).load(item.photoUrl).placeholder(R.drawable.ic_user).into(holder.ivAvatar)

        holder.itemView.setOnClickListener { onClick(item) }
    }
}
