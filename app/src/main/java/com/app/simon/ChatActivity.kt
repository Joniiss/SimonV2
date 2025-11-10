package com.app.simon

import android.os.Bundle
import android.text.TextUtils
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.simon.adapter.ChatMessagesAdapter
import com.app.simon.data.ChatMessage
import com.app.simon.data.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot

class ChatActivity : AppCompatActivity() {

    companion object {
        // Passe esse channelId via Intent extra ou calcule determinístico p/ 1:1
        const val EXTRA_CHANNEL_ID = "channel_id"
        private const val PAGE_SIZE = 20L
    }

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val repo = ChatRepository(auth = auth)

    private lateinit var recycler: RecyclerView
    private lateinit var input: EditText
    private lateinit var btnBack: ImageButton
    private lateinit var sendBtn: ImageButton

    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var adapter: ChatMessagesAdapter

    private var liveReg: ListenerRegistration? = null
    private var lastVisible: DocumentSnapshot? = null
    private var isLoadingMore = false
    private var reachedEnd = false
    private val userCache = HashMap<String, Pair<String, String?>>() // uid -> (nome, fotoUrl)

    private val currentChannelId: String by lazy {
        intent.getStringExtra(EXTRA_CHANNEL_ID)
            ?: throw IllegalStateException("EXTRA_CHANNEL_ID ausente")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat) // teu layout atual

        // IDs que devem existir no teu layout:
        btnBack = findViewById(R.id.btnBack)
        recycler = findViewById(R.id.recyclerChat)
        input = findViewById(R.id.edtMessage)
        sendBtn = findViewById(R.id.btnSend)

        btnBack.setOnClickListener { finish() }

        layoutManager = LinearLayoutManager(this).apply {
            reverseLayout = true   // mensagens mais novas no topo
            stackFromEnd = false
        }
        adapter = ChatMessagesAdapter { auth.currentUser?.uid ?: "" }
        recycler.layoutManager = layoutManager
        recycler.adapter = adapter

        setupPaginationScroll()
        setupSendActions()

        startListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopListening()
    }

    private fun setupSendActions() {
        sendBtn.setOnClickListener {
            val text = input.text?.toString()?.trim() ?: ""
            if (text.isNotEmpty()) sendText(text)
        }
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val text = input.text?.toString()?.trim() ?: ""
                if (text.isNotEmpty()) sendText(text)
                true
            } else false
        }
    }

    private fun sendText(text: String) {
        disableInput()
        repo.sendText(
            channelId = currentChannelId,
            text = text,
            onOk = {
                input.text?.clear()
                enableInput()
            },
            onError = {
                // exibe erro conforme padrão do teu app
                enableInput()
            }
        )
    }

    private fun disableInput() {
        sendBtn.isEnabled = false
        input.isEnabled = false
    }
    private fun enableInput() {
        sendBtn.isEnabled = true
        input.isEnabled = true
    }

    /**
     * Primeira página em tempo real
     */
    private fun startListening() {
        stopListening()
        reachedEnd = false
        liveReg = repo.watchFirstPage(
            channelId = currentChannelId,
            pageSize = PAGE_SIZE,
            onUpdate = { snap -> handleFirstPage(snap) },
            onError = { /* mostra erro */ }
        )
    }

    private fun stopListening() {
        liveReg?.remove()
        liveReg = null
    }

    private fun handleFirstPage(snap: QuerySnapshot) {
        // docs vêm desc (mais novos primeiro) por causa do reverseLayout = true
        val docs = snap.documents
        if (docs.isNotEmpty()) lastVisible = docs.last()

        val list = docs.map { it.toObject(ChatMessage::class.java)!!.copy(id = it.id) }
        adapter.submitList(list)

        // marca como lidas as mensagens visíveis
        repo.markAsRead(
            docs = docs,
            onDone = { /* ok */ },
            onError = { /* ignore silencioso ou log */ }
        )
    }

    /**
     * Scroll para carregar mais quando chegar perto do fim
     */
    private fun setupPaginationScroll() {
        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(rv, dx, dy)
                if (reachedEnd || isLoadingMore) return

                val total = layoutManager.itemCount
                val lastVisiblePos = layoutManager.findLastVisibleItemPosition()
                val shouldLoad = lastVisiblePos >= total - 5 // margem de segurança

                if (shouldLoad) loadMore()
            }
        })
    }

    private fun loadMore() {
        val anchor = lastVisible ?: return
        isLoadingMore = true

        repo.loadMore(
            channelId = currentChannelId,
            lastVisible = anchor,
            pageSize = PAGE_SIZE,
            onSuccess = { snap ->
                val docs = snap.documents
                if (docs.isEmpty()) {
                    reachedEnd = true
                    isLoadingMore = false
                    return@loadMore
                }
                lastVisible = docs.last()

                // Lista atual (novas no topo)
                val current = adapter.currentList.toMutableList()
                val more = docs.map { it.toObject(ChatMessage::class.java)!!.copy(id = it.id) }
                current.addAll(more)

                adapter.submitList(current)

                // marcar como lidas também
                repo.markAsRead(docs, onDone = { }, onError = { })

                isLoadingMore = false
            },
            onError = {
                isLoadingMore = false
                // exibe erro conforme padrão do app
            }
        )
    }
}
