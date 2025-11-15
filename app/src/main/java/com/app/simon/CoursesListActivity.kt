package com.app.simon

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.simon.adapter.CoursesAdapter
import com.app.simon.data.SubjectData
import com.app.simon.data.User
import com.app.simon.databinding.ActivityCoursesListBinding
import com.beust.klaxon.Klaxon
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.functions
import com.google.gson.GsonBuilder

class CoursesListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCoursesListBinding
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mAdapter: CoursesAdapter

    private lateinit var functions: FirebaseFunctions
    private val gson = GsonBuilder().enableComplexMapKeySerialization().create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityCoursesListBinding.inflate(layoutInflater)
        setContentView(binding.root)


        functions = Firebase.functions("southamerica-east1")

        mRecyclerView = binding.rvCourses

        val user = intent.getSerializableExtra("user") as User

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.header.setOnClickListener {
            val iVoltar = Intent(this, HomeActivity::class.java)
            finish()
        }

        mAdapter = CoursesAdapter(mutableListOf(), user, true)

        getCourses(user.curso, user.periodo)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val genericResp = gson.fromJson(
                        task.result,
                        FunctionsGenericResponse::class.java
                    )

                    println(genericResp.payload)

                    val courses = Klaxon()
                        .parseArray<SubjectData>(genericResp.payload.toString())
                    mAdapter = CoursesAdapter(courses!! as MutableList<SubjectData>, user, true)

                    mRecyclerView.layoutManager = LinearLayoutManager(this)
                    mRecyclerView.adapter = mAdapter
                }
                else {
                    val e = task.exception
                    if (e is FirebaseFunctionsException) {
                        val code = e.code
                        println(code)

                        val details = e.details
                        println(details)
                    }
                }
            }
    }

    private fun getCourses(course: String, term: Int): Task<String> {

        val data = hashMapOf("course" to course,
            "term" to term)
        return functions
            .getHttpsCallable("getCoursesMobile")
            .call(data)
            .continueWith { task ->
                gson.toJson(task.result?.data)
            }
    }

    private fun helloWorld(): Task<String> {

        val data = hashMapOf("course" to "course",
            "term" to "term")
        return functions
            .getHttpsCallable("helloWorld")
            .call(data)
            .continueWith { task ->
                task.result?.data.toString()
            }
    }
}
