package com.app.simon

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.simon.adapter.ChatsAdapter
import com.app.simon.data.ChatChannelItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.Timestamp

class ChatsListActivity : AppCompatActivity() {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val nomeCache = mutableMapOf<String, String>() // uid -> nome
    private lateinit var edtSearch: EditText
    private lateinit var recycler: RecyclerView
    private lateinit var btnBack: ImageButton
    private lateinit var progress: ProgressBar
    private lateinit var adapter: ChatsAdapter
    private var reg: ListenerRegistration? = null

    // cache básico: uid -> (displayName, photoUrl)
    private val userCache = HashMap<String, Pair<String, String?>>()

    // lista mestre para filtrar localmente
    private val fullList = mutableListOf<ChatChannelItem>()

    private fun getUserName(uid: String, onResult: (String) -> Unit) {
        // 1) tenta cache primeiro
        nomeCache[uid]?.let { onResult(it); return }

        val dbAluno = db.collection("Alunos")
        val dbProfessor = db.collection("Professores")

        // tenta em Alunos
        dbAluno.whereEqualTo("uid", uid).limit(1).get()
            .addOnSuccessListener { snap ->
                if (!snap.isEmpty) {
                    val doc = snap.documents.first()
                    val nome = doc.getString("nome") ?: uid
                    nomeCache[uid] = nome
                    onResult(nome)
                } else {
                    // tenta em Professores
                    dbProfessor.whereEqualTo("uid", uid).limit(1).get()
                        .addOnSuccessListener { snap2 ->
                            if (!snap2.isEmpty) {
                                val doc = snap2.documents.first()
                                val nome = doc.getString("nome") ?: uid
                                nomeCache[uid] = nome
                                onResult(nome)
                            } else {
                                // não achou em nenhum dos dois
                                onResult(uid)
                            }
                        }
                        .addOnFailureListener { onResult(uid) }
                }
            }
            .addOnFailureListener { onResult(uid) }
    }

    private fun resolveAndRefresh(channelId: String, title: String) {
        val idx = fullList.indexOfFirst { it.channelId == channelId }
        if (idx >= 0) {
            val old = fullList[idx]
            fullList[idx] = old.copy(title = title)
            adapter.submitList(fullList.toList())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chats_list)

        btnBack = findViewById(R.id.btnBack)
        edtSearch = findViewById(R.id.edtSearch)
        recycler = findViewById(R.id.recyclerChats)
        progress = findViewById(R.id.progress)

        btnBack.setOnClickListener { finish() }

        adapter = ChatsAdapter(
            onClick = { item ->
                // Abre o ChatActivity com o channelId selecionado
                val it = Intent(this, ChatActivity::class.java)
                it.putExtra(ChatActivity.EXTRA_CHANNEL_ID, item.channelId)
                startActivity(it)
            }
        )

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        // busca local (client-side)
        edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterList(s?.toString().orEmpty())
            }
        })
    }

    override fun onStart() {
        super.onStart()
        startListeningMyChats()
    }

    override fun onStop() {
        super.onStop()
        reg?.remove()
        reg = null
    }

    /** Tenta resolver nome/foto do usuário em até três jeitos:
     *  1) users/{uid}
     *  2) users (where uid == {uid})
     *  3) Monitores (where uid == {uid})
     */
    private fun fetchUserDisplay(otherUid: String, onResult: (name: String?, photoUrl: String?) -> Unit) {
        // 0) cache
        userCache[otherUid]?.let { cached ->
            onResult(cached.first, cached.second); return
        }

        val usersDoc = db.collection("users").document(otherUid)
        usersDoc.get().addOnSuccessListener { snap ->
            if (snap.exists()) {
                val name = snap.getString("nome")
                    ?: snap.getString("displayName")
                    ?: snap.getString("name")
                    ?: otherUid
                val photo = snap.getString("foto") ?: snap.getString("photoURL")
                userCache[otherUid] = name to photo
                onResult(name, photo); return@addOnSuccessListener
            }
            // 2) users where uid == otherUid
            db.collection("users").whereEqualTo("uid", otherUid).limit(1).get()
                .addOnSuccessListener { qs ->
                    if (!qs.isEmpty) {
                        val d = qs.documents.first()
                        val name = d.getString("nome")
                            ?: d.getString("displayName")
                            ?: d.getString("name")
                            ?: otherUid
                        val photo = d.getString("foto") ?: d.getString("photoURL")
                        userCache[otherUid] = name to photo
                        onResult(name, photo)
                    } else {
                        // 3) Monitores where uid == otherUid (pelo seu print)
                        db.collection("Monitores").whereEqualTo("uid", otherUid).limit(1).get()
                            .addOnSuccessListener { qs2 ->
                                if (!qs2.isEmpty) {
                                    val d2 = qs2.documents.first()
                                    val name = d2.getString("nome") ?: otherUid
                                    val photo = d2.getString("foto")
                                    userCache[otherUid] = name to photo
                                    onResult(name, photo)
                                } else {
                                    onResult(otherUid, null) // fallback: mostra uid
                                }
                            }
                            .addOnFailureListener { onResult(otherUid, null) }
                    }
                }
                .addOnFailureListener { onResult(otherUid, null) }
        }.addOnFailureListener { onResult(otherUid, null) }
    }

    private fun startListeningMyChats() {
        val me = auth.currentUser?.uid
        if (me == null) {
            Toast.makeText(this, "Faça login para ver os chats.", Toast.LENGTH_SHORT).show()
            return
        }

        progress.visibility = View.VISIBLE

        // IMPORTANTE: coleção se chama Chats (maiúsculo)
        val query = db.collection("Chats")
            .whereArrayContains("members", me)
            .orderBy("lastMessageAt", Query.Direction.DESCENDING)
            .limit(50)

        reg?.remove()
        reg = query.addSnapshotListener { snap, e ->
            progress.visibility = View.GONE
            if (e != null) {
                Toast.makeText(this, "Erro ao carregar: ${e.message}", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }
            if (snap == null) return@addSnapshotListener

            // Converte docs em itens de lista
            buildItems(me, snap)
        }
    }

    private fun buildItems(me: String, snap: QuerySnapshot) {
        fullList.clear()

        for (doc in snap.documents) {
            val channelId = doc.id
            val data = doc.data ?: continue

            val type = (data["type"] as? String) ?: "direct"
            val members = (data["members"] as? List<*>)?.map { it.toString() } ?: emptyList()
            val lastMessage = data["lastMessage"]?.toString() ?: ""
            val lastMessageAt = data["lastMessageAt"] as? Timestamp
            val nameFromDoc = data["name"]?.toString()

            var title = nameFromDoc ?: "Conversa"

            // 🔹 Se for 1:1 e não tiver name no documento, usa o nome do outro usuário
            if (type == "direct" && nameFromDoc.isNullOrBlank()) {
                val other = members.firstOrNull { it != me }
                if (other != null) {
                    // tenta cache primeiro
                    val cached = nomeCache[other]
                    if (cached != null) {
                        title = cached
                    } else {
                        // busca de forma assíncrona e atualiza depois
                        getUserName(other) { nomeResolved ->
                            resolveAndRefresh(channelId, nomeResolved)
                        }
                    }
                }
            }

            val item = ChatChannelItem(
                channelId = channelId,
                title = title,
                lastMessage = lastMessage,
                lastMessageAt = lastMessageAt
            )
            fullList.add(item)
        }

        adapter.submitList(fullList.toList())
    }

    /** Atualiza o item na lista depois que o nome chegar */
    private fun resolveAndRefresh(channelId: String, title: String, photoUrl: String?) {
        val idx = fullList.indexOfFirst { it.channelId == channelId }
        if (idx >= 0) {
            val old = fullList[idx]
            fullList[idx] = old.copy(title = title, photoUrl = photoUrl)
            adapter.submitList(fullList.toList())
        }
    }


    private fun filterList(query: String) {
        if (query.isBlank()) {
            adapter.submitList(fullList.toList()); return
        }
        val q = query.trim().lowercase()
        val filtered = fullList.filter {
            it.title.lowercase().contains(q) || it.lastMessage.lowercase().contains(q)
        }
        adapter.submitList(filtered)
    }
}
