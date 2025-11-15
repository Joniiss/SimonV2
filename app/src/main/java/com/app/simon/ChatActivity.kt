package com.app.simon

import android.os.Bundle
import android.text.TextUtils
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.simon.adapter.ChatMessagesAdapter
import com.app.simon.data.ChatMessage
import com.app.simon.data.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot

class ChatActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CHANNEL_ID = "channel_id"
        private const val PAGE_SIZE = 20L
    }

    private lateinit var titleView: TextView
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val nomeCache = mutableMapOf<String, String>()

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
    private val userCache = HashMap<String, Pair<String, String?>>()

    private val currentChannelId: String by lazy {
        intent.getStringExtra(EXTRA_CHANNEL_ID)
            ?: throw IllegalStateException("EXTRA_CHANNEL_ID ausente")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(R.layout.activity_chat)

        btnBack = findViewById(R.id.btnBack)
        recycler = findViewById(R.id.recyclerChat)
        input = findViewById(R.id.edtMessage)
        sendBtn = findViewById(R.id.btnSend)
        titleView = findViewById(R.id.tvTitle)

        // 🔥 Ajuste correto para messageBar subir com teclado sem quebrar nada
        val messageBar = findViewById<android.view.View>(R.id.messageBar)

        ViewCompat.setOnApplyWindowInsetsListener(messageBar) { view, insets ->
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val nav = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            val bottomPadding =
                if (insets.isVisible(WindowInsetsCompat.Type.ime())) ime.bottom
                else nav.bottom

            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                bottomPadding
            )

            insets
        }

        btnBack.setOnClickListener { finish() }

        layoutManager = LinearLayoutManager(this).apply {
            reverseLayout = true
            stackFromEnd = false
        }
        adapter = ChatMessagesAdapter { auth.currentUser?.uid ?: "" }
        recycler.layoutManager = layoutManager
        recycler.adapter = adapter

        loadChatTitle()
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

    private fun getUserName(uid: String, onResult: (String) -> Unit) {
        nomeCache[uid]?.let { onResult(it); return }

        val dbAluno = db.collection("Alunos")
        val dbProfessor = db.collection("Professores")

        dbAluno.whereEqualTo("uid", uid).limit(1).get()
            .addOnSuccessListener { snap ->
                if (!snap.isEmpty) {
                    val doc = snap.documents.first()
                    val nome = doc.getString("nome") ?: uid
                    nomeCache[uid] = getFirstName(nome)
                    onResult(getFirstName(nome))
                } else {
                    dbProfessor.whereEqualTo("uid", uid).limit(1).get()
                        .addOnSuccessListener { snap2 ->
                            if (!snap2.isEmpty) {
                                val doc = snap2.documents.first()
                                val nome = doc.getString("nome") ?: uid
                                nomeCache[uid] = getFirstName(nome)
                                onResult(getFirstName(nome))
                            } else {
                                onResult(uid)
                            }
                        }
                        .addOnFailureListener { onResult(uid) }
                }
            }
            .addOnFailureListener { onResult(uid) }
    }

    private fun loadChatTitle() {
        val me = auth.currentUser?.uid ?: return

        db.collection("Chats").document(currentChannelId).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    titleView.text = "Conversa"
                    return@addOnSuccessListener
                }

                val type = doc.getString("type") ?: "direct"
                val nameFromDoc = doc.getString("name")
                val members = doc.get("members") as? List<*> ?: emptyList<Any>()

                if (type != "direct" && !nameFromDoc.isNullOrBlank()) {
                    titleView.text = nameFromDoc
                    return@addOnSuccessListener
                }

                val otherUid = members.map { it.toString() }.firstOrNull { it != me }
                if (otherUid == null) {
                    titleView.text = "Conversa"
                    return@addOnSuccessListener
                }

                getUserName(otherUid) { nome ->
                    titleView.text = nome
                }
            }
            .addOnFailureListener {
                titleView.text = "Conversa"
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

    private fun startListening() {
        stopListening()
        reachedEnd = false
        liveReg = repo.watchFirstPage(
            channelId = currentChannelId,
            pageSize = PAGE_SIZE,
            onUpdate = { snap -> handleFirstPage(snap) },
            onError = { }
        )
    }

    private fun stopListening() {
        liveReg?.remove()
        liveReg = null
    }

    private fun handleFirstPage(snap: QuerySnapshot) {
        val docs = snap.documents

        lastVisible = docs.lastOrNull { d ->
            d.getTimestamp("createdAt") != null && !d.metadata.hasPendingWrites()
        }

        val list = docs.mapNotNull { d ->
            d.toObject(ChatMessage::class.java)?.copy(id = d.id)
        }

        adapter.submitList(list)

        repo.markAsRead(
            docs = docs,
            onDone = { },
            onError = { }
        )

        reachedEnd = docs.size < PAGE_SIZE
    }

    private fun setupPaginationScroll() {
        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(rv, dx, dy)
                if (reachedEnd || isLoadingMore) return

                val total = layoutManager.itemCount
                val lastVisiblePos = layoutManager.findLastVisibleItemPosition()
                val shouldLoad = lastVisiblePos >= total - 5

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

                val current = adapter.currentList.toMutableList()
                val more = docs.map { it.toObject(ChatMessage::class.java)!!.copy(id = it.id) }
                current.addAll(more)

                adapter.submitList(current)

                repo.markAsRead(docs, onDone = { }, onError = { })

                isLoadingMore = false
            },
            onError = {
                isLoadingMore = false
            }
        )
    }

    private fun getFirstName(nome: String): String {
        val spaceIndex = nome.indexOf(' ')
        return if (spaceIndex == -1) nome else nome.take(spaceIndex)
    }
}
