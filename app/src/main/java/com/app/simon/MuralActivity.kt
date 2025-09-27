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
import com.app.simon.adapter.ForumPostsAdapter
import com.app.simon.adapter.MuralPostsAdapter
import com.app.simon.data.ForumData
import com.app.simon.data.ForumPostData
import com.app.simon.data.MuralPostData
import com.app.simon.data.User
import com.app.simon.databinding.ActivityMuralBinding
import com.beust.klaxon.Klaxon
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.functions
import com.google.gson.GsonBuilder

class MuralActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMuralBinding

    private lateinit var mRecyclerView: RecyclerView

    private lateinit var mAdapter: MuralPostsAdapter

    private lateinit var functions: FirebaseFunctions
    private val gson = GsonBuilder().enableComplexMapKeySerialization().create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMuralBinding.inflate(layoutInflater)
        setContentView(binding.root)

        functions = Firebase.functions("southamerica-east1")

        val user = intent.getSerializableExtra("user") as User
        val courseId = intent.getStringExtra("courseId")

        mRecyclerView = binding.rvMuralPosts

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        mAdapter = MuralPostsAdapter(mutableListOf(), user)

        getMuralPosts(courseId!!)
            .addOnCompleteListener { task ->
                Toast.makeText(baseContext, "ENTROU AQUI", Toast.LENGTH_SHORT).show()
                if (task.isSuccessful) {
                    val genericResp = gson.fromJson(
                        task.result,
                        FunctionsGenericResponse::class.java
                    )

                    println(genericResp.payload)

                    val posts = Klaxon()
                        .parseArray<MuralPostData>(genericResp.payload.toString())

                    mAdapter = MuralPostsAdapter(posts!! as MutableList<MuralPostData>, user)

                    mRecyclerView.layoutManager = LinearLayoutManager(this)
                    mRecyclerView.adapter = mAdapter
                }
            }

        binding.tvCreatePost.setOnClickListener {
            val intent = Intent(this, NewMuralPostActivity::class.java)
            intent.putExtra("user", user)
            intent.putExtra("courseId", courseId)
            startActivity(intent)
        }

    }

    private fun getMuralPosts(disciplinaId: String): Task<String> {

        val data = hashMapOf("disciplinaId" to disciplinaId)
        return functions
            .getHttpsCallable("getMuralPostsMobile")
            .call(data)
            .continueWith { task ->
                gson.toJson(task.result?.data)
            }
    }
}