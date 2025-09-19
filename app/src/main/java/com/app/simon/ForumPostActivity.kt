package com.app.simon

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.simon.adapter.ForumCommentsAdapter
import com.app.simon.adapter.ForumPostsAdapter
import com.app.simon.data.ForumCommentsData
import com.app.simon.data.ForumData
import com.app.simon.data.ForumPostData
import com.app.simon.data.User
import com.app.simon.databinding.ActivityForumPostBinding
import com.beust.klaxon.Klaxon
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.functions
import com.google.gson.GsonBuilder
import java.time.Instant
import java.time.format.DateTimeFormatter

class ForumPostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForumPostBinding

    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mAdapter: ForumCommentsAdapter

    private lateinit var functions: FirebaseFunctions
    private val gson = GsonBuilder().enableComplexMapKeySerialization().create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        //setContentView(R.layout.activity_forum_post)
        binding = ActivityForumPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        functions = Firebase.functions("southamerica-east1")

        mRecyclerView = binding.rvComments



        val user = intent.getSerializableExtra("user") as User
        val post = intent.getSerializableExtra("post") as ForumData

        binding.tvAuthor.text = post.data.userName
        binding.tvTitle.text = post.data.title
        binding.tvContent.text = post.data.content
        binding.tvLikes.text = post.data.likes.toString()

        var comments = mutableListOf<ForumCommentsData>()
        if (post.data.comments.isNotEmpty()) {
            getComments(post.docId)
                .addOnCompleteListener { task ->
                    Toast.makeText(baseContext, "ENTROU AQUI", Toast.LENGTH_SHORT).show()
                    if (task.isSuccessful) {
                        val genericResp = gson.fromJson(
                            task.result,
                            FunctionsGenericResponse::class.java
                        )
                        println(genericResp.message)
                        println(genericResp.payload)

                        comments = Klaxon()
                            .parseArray<ForumCommentsData>(genericResp.payload.toString()) as MutableList<ForumCommentsData>

                        mAdapter = ForumCommentsAdapter(comments!! as MutableList<ForumCommentsData>, user)

                        mRecyclerView.layoutManager = LinearLayoutManager(this)
                        mRecyclerView.adapter = mAdapter
                    }
                }
        }

        binding.btnSend.setOnClickListener {
            val currentInstant = Instant.now()
            val comment = ForumCommentsData(
                postId = post.docId,
                userId = user.uid,
                userName = user.nome,
                userRole = "ALUNO",
                createdAt = DateTimeFormatter.ISO_INSTANT.format(currentInstant).toString(),
                content = binding.etComment.text.toString()
            )
            comments.add(comment)
            mAdapter = ForumCommentsAdapter(comments!! as MutableList<ForumCommentsData>, user)

            mRecyclerView.layoutManager = LinearLayoutManager(this)
            mRecyclerView.adapter = mAdapter
            binding.etComment.text.clear()
            createComment(comment)
        }

    }

    private fun getComments(postId: String): Task<String> {

        val data = hashMapOf("postId" to postId)
        return functions
            .getHttpsCallable("getCommentsMobile")
            .call(data)
            .continueWith { task ->
                gson.toJson(task.result?.data)
            }
    }

    private fun createComment(comment: ForumCommentsData): Task<String> {

        val data = hashMapOf("postId" to comment.postId,
            "userId" to comment.userId,
            "userName" to comment.userName,
            "userRole" to comment.userRole,
            "content" to comment.content,
            "createdAt" to comment.createdAt
        )
        return functions
            .getHttpsCallable("createCommentMobile")
            .call(data)
            .continueWith { task ->
                gson.toJson(task.result?.data)
            }
    }
}