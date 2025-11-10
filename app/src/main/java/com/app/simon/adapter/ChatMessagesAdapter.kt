package com.app.simon.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.simon.R
import com.app.simon.data.ChatMessage
import com.google.firebase.Timestamp
import java.text.DateFormat

/**
 * Adapter que posiciona:
 *  - mensagens do "meu" usuário: à DIREITA
 *  - mensagens do outro: à ESQUERDA
 */
class ChatMessagesAdapter(
    private val myUidProvider: () -> String
) : ListAdapter<ChatMessage, ChatMessagesAdapter.VH>(DIFF) {

    companion object {
        private const val VIEW_OTHER = 0
        private const val VIEW_ME = 1

        private val DIFF = object : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(old: ChatMessage, new: ChatMessage) = old.id == new.id
            override fun areContentsTheSame(old: ChatMessage, new: ChatMessage) = old == new
        }
    }

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val txtMessage: TextView = v.findViewById(R.id.txtMessage)
        val txtMeta: TextView = v.findViewById(R.id.txtMeta) // hora / "Você • hora"
    }

    override fun getItemViewType(position: Int): Int {
        val m = getItem(position)
        val isMine = m.senderId == myUidProvider.invoke()
        return if (isMine) VIEW_ME else VIEW_OTHER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inflater = LayoutInflater.from(parent.context)
        val layout = when (viewType) {
            VIEW_ME -> R.layout.item_chat_message_me
            else -> R.layout.item_chat_message_other
        }
        val view = inflater.inflate(layout, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = getItem(position)

        holder.txtMessage.text = m.text ?: ""

        val ts: Timestamp? = m.createdAt
        val time = ts?.toDate()?.let { DateFormat.getTimeInstance(DateFormat.SHORT).format(it) } ?: ""

        val isMine = m.senderId == myUidProvider.invoke()
        holder.txtMeta.text = if (isMine) "Você • $time" else time
        // O alinhamento e cores já vêm do layout específico (me vs other).
    }
}
