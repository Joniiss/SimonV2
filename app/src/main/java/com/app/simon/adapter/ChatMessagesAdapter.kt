package com.app.simon.adapter

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.simon.R
import com.app.simon.data.ChatMessage
import com.google.firebase.Timestamp
import java.text.DateFormat

class ChatMessagesAdapter(
    private val myUidProvider: () -> String
) : ListAdapter<ChatMessage, ChatMessagesAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean =
                oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean =
                oldItem == newItem
        }
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtMessage: TextView = itemView.findViewById(R.id.tvMessage)
        val txtMeta: TextView = itemView.findViewById(R.id.tvMeta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return VH(v)
    }
    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = getItem(position)
        holder.txtMessage.text = m.text ?: ""

        val ts: Timestamp? = m.createdAt
        val time = ts?.toDate()?.let { DateFormat.getTimeInstance(DateFormat.SHORT).format(it) } ?: ""
        val isMine = m.senderId == myUidProvider.invoke()

        // Define o texto do rodapé (hora + identificação)
        holder.txtMeta.text = if (isMine) "Você • $time" else "$time"

        // === BLOCO DE ESTILIZAÇÃO DAS MENSAGENS ===
        val wrapper = holder.itemView.findViewById<LinearLayout>(R.id.messageWrapper)
        val params = wrapper.layoutParams as LinearLayout.LayoutParams
        params.gravity = if (isMine) Gravity.END else Gravity.START
        wrapper.layoutParams = params

        val bubble = holder.itemView.findViewById<LinearLayout>(R.id.messageBubble)
        val bgRes = if (isMine) R.drawable.bg_chat_bubble_right else R.drawable.bg_chat_bubble_left
        bubble.setBackgroundResource(bgRes)
    }

}
