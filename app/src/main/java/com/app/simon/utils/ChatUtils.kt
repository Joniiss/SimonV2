package com.app.simon.utils

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.app.simon.ChatActivity
import com.app.simon.data.ChatRepository

object ChatUtils {
    fun openChat(context: Context, otherUid: String) {
        val repo = ChatRepository()
        repo.createOrGetDirectChannel(
            otherUid = otherUid,
            onSuccess = { channelId: String ->
                val intent = Intent(context, ChatActivity::class.java)
                intent.putExtra(ChatActivity.EXTRA_CHANNEL_ID, channelId)
                context.startActivity(intent)
            },
            onError = { e ->
                Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )

    }
}
