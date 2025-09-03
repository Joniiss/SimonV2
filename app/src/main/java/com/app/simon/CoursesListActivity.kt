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

        //setContentView(R.layout.activity_courses_list)

        functions = Firebase.functions("southamerica-east1")

        mRecyclerView = binding.rvCourses


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Clique no header
        binding.header.setOnClickListener {
            val iVoltar = Intent(this, HomeActivity::class.java)
            startActivity(iVoltar)
            finish() // fecha a CoursesListActivity
        }

        mAdapter = CoursesAdapter(mutableListOf())
        
        helloWorld().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(baseContext, task.result, Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(baseContext, "falhou", Toast.LENGTH_SHORT).show()
            }
        }

        getCourses("Engenharia de Software", 2)
            .addOnCompleteListener { task ->
                Toast.makeText(baseContext, "ENTROU AQUI", Toast.LENGTH_SHORT).show()
                if (task.isSuccessful) {
                    val genericResp = gson.fromJson(
                        task.result,
                        FunctionsGenericResponse::class.java
                    )

                    println(genericResp.payload)

                    val courses = Klaxon()
                        .parseArray<SubjectData>(genericResp.payload.toString())
                    mAdapter = CoursesAdapter(courses!! as MutableList<SubjectData>)

                    mRecyclerView.layoutManager = LinearLayoutManager(this)
                    mRecyclerView.adapter = mAdapter
                }
                else {
                    val e = task.exception
                    if (e is FirebaseFunctionsException) {
                        // Function error code, will be INTERNAL if the failure
                        // was not handled properly in the function call.
                        val code = e.code
                        println(code)
                        // Arbitrary error details passed back from the function,
                        // usually a Map<String, Any>.
                        Toast.makeText(baseContext, code.toString(), Toast.LENGTH_SHORT).show()

                        val details = e.details
                        println(details)
                        Toast.makeText(baseContext, details.toString(), Toast.LENGTH_SHORT).show()

                    }
                }
            }

        if(mAdapter.itemCount == 0){
            Toast.makeText(baseContext, "Você não possui matérias!", Toast.LENGTH_LONG).show()
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
                //gson.toJson(task.result?.data)
                task.result?.data.toString()
            }
    }
}
