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
        val tasks = mutableListOf<com.google.android.gms.tasks.Task<DocumentSnapshot>>()

        for (doc in snap.documents) {
            val channelId = doc.id
            val data = doc.data ?: continue

            val type = (data["type"] as? String) ?: "direct"
            val members = (data["members"] as? List<*>)?.map { it.toString() } ?: emptyList()
            val lastMessage = data["lastMessage"]?.toString() ?: ""
            val lastMessageAt = data["lastMessageAt"] as? Timestamp
            val nameFromDoc = data["name"]?.toString() // pode existir em grupo

            // título: se for grupo, usa "name"; se for direct, pega o "outro" user
            var title = nameFromDoc
            var photoUrl: String? = null

            if (title.isNullOrBlank() && type == "direct") {
                val other = members.firstOrNull { it != me }
                if (other != null) {
                    val cached = userCache[other]
                    if (cached != null) {
                        title = cached.first
                        photoUrl = cached.second
                    } else {
                        // carrega user/other e guarda para a próxima
                        val t = db.collection("users").document(other).get()
                            .addOnSuccessListener { u ->
                                val display = u.getString("displayName") ?: other
                                val photo = u.getString("photoURL")
                                userCache[other] = display to photo
                                // atualiza item dessa conversa na lista após resolver nome
                                resolveAndRefresh(channelId, display, photo)
                            }
                        tasks.add(t)
                    }
                }
            }

            val item = ChatChannelItem(
                channelId = channelId,
                title = title ?: "Conversa",
                lastMessage = lastMessage,
                lastMessageAt = lastMessageAt,
                photoUrl = photoUrl
            )
            fullList.add(item)
        }

        // Mostra imediatamente com o que já temos; updates virão ao resolver nomes
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
