package com.app.simon

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.app.simon.adapter.ForumCommentsAdapter
import com.app.simon.data.ForumPostData
import com.app.simon.data.User
import com.app.simon.databinding.ActivityForumPostBinding
import com.google.firebase.Firebase
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.functions
import com.google.gson.GsonBuilder

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
        val post = intent.getSerializableExtra("post") as ForumPostData
        println("postforum")



        binding.tvAuthor.text = post.userName
        binding.tvTitle.text = post.title
        binding.tvContent.text = post.content
        binding.tvLikes.text = post.likes.toString()
    }
}