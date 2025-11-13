package com.app.simon.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage

class ChatRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val rtdb: FirebaseDatabase = FirebaseDatabase.getInstance()
) {

    private val nomeCache = mutableMapOf<String, String>()

    private fun uid(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Usuário não autenticado")

    private fun chats(): CollectionReference =
        db.collection("Chats")

    private fun messages(channelId: String): CollectionReference =
        chats().document(channelId).collection("messages")

    private fun getUserName(uid: String, onResult: (String?) -> Unit) {
        nomeCache[uid]?.let { onResult(it); return }
        val db = FirebaseFirestore.getInstance()
        val dbAluno = db.collection("Alunos")
        val dbProfessor = db.collection("Professores")

        dbAluno.whereEqualTo("uid", uid).limit(1).get()
            .addOnSuccessListener { snap ->
                if (!snap.isEmpty) {
                    val doc = snap.documents.first()
                    val nome = doc.getString("nome")
                    nomeCache[uid] = nome ?: uid
                    onResult(nome)
                } else {
                    dbProfessor.whereEqualTo("uid", uid).limit(1).get()
                        .addOnSuccessListener { snap2 ->
                            if (!snap2.isEmpty) {
                                val doc = snap2.documents.first()
                                val nome = doc.getString("nome")
                                nomeCache[uid] = nome ?: uid
                                onResult(nome)
                            } else { onResult(null) }
                        }
                        .addOnFailureListener { onResult(null) }
                }
            }
            .addOnFailureListener { onResult(null) }
    }

    /** ID determinístico para canal 1:1: min(uidA, uidB) + "_" + max(uidA, uidB) */
    fun directChannelIdOf(a: String, b: String): String =
        if (a <= b) "${a}_$b" else "${b}_$a"

    /**
     * Primeira página: listener em tempo real
     */
    fun watchFirstPage(
        channelId: String,
        pageSize: Long,
        onUpdate: (QuerySnapshot) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return messages(channelId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .orderBy(FieldPath.documentId())
            .limit(pageSize)
            .addSnapshotListener { snap, e ->
                if (e != null) { onError(e); return@addSnapshotListener }
                if (snap != null) onUpdate(snap)
            }
    }

    /**
     * Paginação “carregar mais” (pull-to-top)
     */
    fun loadMore(
        channelId: String,
        lastVisible: DocumentSnapshot,
        pageSize: Long,
        onSuccess: (QuerySnapshot) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
/*
        val ts = lastVisible.getTimestamp("createdAt")
        if (ts == null || lastVisible.metadata.hasPendingWrites()) {
            onError(IllegalStateException("Timestamp inválido"))
            return
        }*/

        db.collection("Chats")
            .document(channelId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .orderBy(FieldPath.documentId())
            .startAfter(lastVisible)
            .limit(pageSize)
            .get()
            .addOnSuccessListener(onSuccess)
            .addOnFailureListener(onError)
    }

    /**
     * Enviar texto
     */
    fun sendText(
        channelId: String,
        text: String,
        onOk: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val me = uid()
        val msgRef = messages(channelId).document()
        db.runBatch { b ->
            b.set(msgRef, hashMapOf(
                "text" to text,
                "type" to "text",
                "senderId" to me,
                "createdAt" to FieldValue.serverTimestamp(),
                "attachments" to emptyList<Any>(),
                "status" to mapOf("deliveredTo" to emptyList<String>(), "readBy" to listOf(me))
            ))
            b.update(chats().document(channelId), mapOf(
                "lastMessage" to text,
                "lastMessageBy" to me,
                "lastMessageAt" to FieldValue.serverTimestamp()
            ))
        }.addOnSuccessListener { onOk() }
            .addOnFailureListener(onError)
    }

    /**
     * Marcar mensagens como lidas (read receipts simples)
     */
    fun markAsRead(
        docs: List<DocumentSnapshot>,
        onDone: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val me = uid()
        val batch = db.batch()
        for (d in docs) {
            val data = d.data ?: continue
            val status = data["status"] as? Map<*, *> ?: continue
            val readBy = (status["readBy"] as? List<*>)?.map { it.toString() } ?: emptyList()
            if (!readBy.contains(me)) {
                batch.update(d.reference, "status.readBy", FieldValue.arrayUnion(me))
            }
        }
        batch.commit().addOnSuccessListener { onDone() }.addOnFailureListener(onError)
    }

    private fun typingRef(channelId: String): DatabaseReference =
        rtdb.reference.child("typing").child(channelId)

    fun setTyping(channelId: String, typing: Boolean) {
        val me = uid()
        val ref = typingRef(channelId).child(me)
        if (typing) {
            ref.setValue(true)
            ref.onDisconnect().removeValue()
        } else {
            ref.removeValue()
        }
    }

    /**
     * Cria (se não existir) ou retorna o ID do canal 1:1 entre o usuário logado e otherUid.
     * @param otherUid UID do destinatário
     * @param onSuccess callback com o channelId
     * @param onError callback de erro
     */
    fun createOrGetDirectChannel(
        otherUid: String,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val me = uid()
        val channelId = directChannelIdOf(me, otherUid)
        val docRef = chats().document(channelId)

        docRef.get()
            .addOnSuccessListener { snap ->
                if (snap.exists()) {
                    // Já existe → devolve o id
                    onSuccess(channelId)
                } else {
                    // Não existe → cria o documento do canal
                    val data = hashMapOf(
                        "type" to "direct",
                        "members" to listOf(me, otherUid),
                        "createdBy" to me,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "lastMessage" to null,
                        "lastMessageAt" to null,
                        "lastMessageBy" to null
                    )
                    docRef.set(data, SetOptions.merge())
                        .addOnSuccessListener { onSuccess(channelId) }
                        .addOnFailureListener(onError)
                }
            }
            .addOnFailureListener(onError)
    }


    /**
     * Cria um canal de grupo (se não existir um com o mesmo ID fornecido).
     * @param channelId Id desejado (ou gere com UUID.randomUUID().toString())
     * @param name Nome do grupo (exibido na lista)
     * @param memberUids UIDs dos participantes (precisa incluir o usuário logado)
     */
    fun createGroupChannel(
        channelId: String,
        name: String,
        memberUids: List<String>,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val me = uid()
        require(memberUids.contains(me)) { "O usuário logado precisa estar em memberUids" }

        val docRef = chats().document(channelId)
        val data = hashMapOf(
            "type" to "group",
            "name" to name,
            "members" to memberUids,
            "createdBy" to me,
            "createdAt" to FieldValue.serverTimestamp(),
            "lastMessage" to null,
            "lastMessageAt" to null,
            "lastMessageBy" to null
        )
        docRef.set(data, SetOptions.merge())
            .addOnSuccessListener { onSuccess(channelId) }
            .addOnFailureListener(onError)
    }

}
