package com.app.simon

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.simon.adapter.CoursesAdapter
import com.app.simon.adapter.ForumPostsAdapter
import com.app.simon.data.ForumData
import com.app.simon.data.ForumPostData
import com.app.simon.data.SubjectData
import com.app.simon.data.User
import com.app.simon.databinding.ActivityForumBinding
import com.beust.klaxon.Klaxon
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.functions
import com.google.gson.GsonBuilder

class ForumActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForumBinding

    private lateinit var mRecyclerView: RecyclerView

    private lateinit var mAdapter: ForumPostsAdapter

    private lateinit var functions: FirebaseFunctions
    private val gson = GsonBuilder().enableComplexMapKeySerialization().create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityForumBinding.inflate(layoutInflater)
        setContentView(binding.root)

        functions = Firebase.functions("southamerica-east1")

        val user = intent.getSerializableExtra("user") as User
        val courseId = intent.getStringExtra("courseId")
        val courseName = intent.getStringExtra("courseName")


        mRecyclerView = binding.rvForumPosts

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        mAdapter = ForumPostsAdapter(mutableListOf(), user)

        binding.tvSubject.text = courseName

        getForumPosts(courseId!!)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val genericResp = gson.fromJson(
                        task.result,
                        FunctionsGenericResponse::class.java
                    )

                    println(genericResp.payload)

                    if (genericResp.payload.toString() != "No matching documents.") {
                        val posts = Klaxon()
                            .parseArray<ForumData>(genericResp.payload.toString())
                        val postsData = mutableListOf<ForumPostData>()
                        for (i in posts!!.indices) {
                            postsData.add(posts[i].data)
                        }

                        mAdapter = ForumPostsAdapter(posts!! as MutableList<ForumData>, user)

                        mRecyclerView.layoutManager = LinearLayoutManager(this)
                        mRecyclerView.adapter = mAdapter
                    }


                }
            }

        binding.tvCreatePost.setOnClickListener {
            val intent = Intent(this, NewForumPostActivity::class.java)
            intent.putExtra("user", user)
            intent.putExtra("courseId", courseId)
            startActivity(intent)
        }

        binding.ivMonitors.setOnClickListener {
            val intent = Intent(this, MonitorsListActivity::class.java)
            intent.putExtra("user", user)
            intent.putExtra("courseId", courseId)
            intent.putExtra("courseName", courseName)
            startActivity(intent)
        }

        binding.ivBoard.setOnClickListener {
            val intent = Intent(this, MuralActivity::class.java)
            intent.putExtra("user", user)
            intent.putExtra("courseId", courseId)
            intent.putExtra("courseName", courseName)
            startActivity(intent)
        }

        binding.ivBack.setOnClickListener {
            finish()
        }
    }

    private fun getForumPosts(courseId: String): Task<String> {

        val data = hashMapOf("courseId" to courseId)
        return functions
            .getHttpsCallable("getForumPostsMobile")
            .call(data)
            .continueWith { task ->
                gson.toJson(task.result?.data)
            }
    }
}