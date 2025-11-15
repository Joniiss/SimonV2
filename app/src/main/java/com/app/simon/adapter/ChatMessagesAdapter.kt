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
import com.google.firebase.firestore.FirebaseFirestore
import java.text.DateFormat
import kotlin.math.log

class ChatMessagesAdapter(
    private val myUidProvider: () -> String
) : ListAdapter<ChatMessage, ChatMessagesAdapter.VH>(DIFF) {

    companion object {
        private const val VIEW_OTHER = 0
        private const val VIEW_ME = 1

        private val DIFF = object : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage) = oldItem == newItem
        }
    }

    private val db = FirebaseFirestore.getInstance()
    private val nomeCache = mutableMapOf<String, String>()

    private fun getUserName(uid: String, onResult: (String?) -> Unit) {
        nomeCache[uid]?.let { onResult(it); return }

        val dbAluno = db.collection("Alunos")
        val dbProfessor = db.collection("Professores")

        dbAluno.whereEqualTo("uid", uid).limit(1).get()
            .addOnSuccessListener { snap ->
                if (!snap.isEmpty) {
                    val nome = snap.documents.first().getString("nome") ?: uid
                    nomeCache[uid] = nome
                    onResult(nome)
                } else {
                    dbProfessor.whereEqualTo("uid", uid).limit(1).get()
                        .addOnSuccessListener { snap2 ->
                            val nome = if (!snap2.isEmpty) {
                                snap2.documents.first().getString("nome") ?: uid
                            } else uid
                            nomeCache[uid] = nome
                            onResult(nome)
                        }
                        .addOnFailureListener { onResult(uid) }
                }
            }
            .addOnFailureListener { onResult(uid) }
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtMessage: TextView = itemView.findViewById(R.id.txtMessage)
        val txtMeta: TextView = itemView.findViewById(R.id.txtMeta) // hora / "Você • hora"
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
        val date = ts?.toDate()?.let { DateFormat.getDateInstance(DateFormat.SHORT).format(it) } ?: ""

        println("date: $date")

        val isMine = m.senderId == myUidProvider.invoke()

        if (isMine) {
            holder.txtMeta.text = "Você • $date • $time"
        } else {
            getUserName(m.senderId) { nome ->
                holder.txtMeta.text = "${getFirstName(nome ?: "Desconhecido")} • $date • $time"
            }
        }
    }

    private fun getFirstName(nome: String): String {
        val spaceIndex = nome.indexOf(' ')
        return if (spaceIndex == -1) nome else nome.take(spaceIndex)
    }
}
