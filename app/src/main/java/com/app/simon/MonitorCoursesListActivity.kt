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
import com.app.simon.adapter.MonitorCoursesAdapter
import com.app.simon.data.MonitorData
import com.app.simon.data.SubjectData
import com.app.simon.data.User
import com.app.simon.databinding.ActivityCoursesListBinding
import com.app.simon.databinding.ActivityMonitorCoursesListBinding
import com.app.simon.databinding.ActivityMonitorsListBinding
import com.beust.klaxon.Klaxon
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.functions
import com.google.gson.GsonBuilder

class MonitorCoursesListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMonitorCoursesListBinding
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mAdapter: MonitorCoursesAdapter

    private lateinit var functions: FirebaseFunctions

    private val gson = GsonBuilder().enableComplexMapKeySerialization().create()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMonitorCoursesListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        functions = Firebase.functions("southamerica-east1")

        mRecyclerView = binding.rvCourses

        val user = intent.getSerializableExtra("user") as User

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Clique no header
        val header = findViewById<LinearLayout>(R.id.header)
        header.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            //startActivity(intent)
            finish() // fecha a CoursesListActivity
        }

        mAdapter = MonitorCoursesAdapter(mutableListOf(), user)

        println(user.uid)

        getMonitorCourses(user.uid)
            .addOnCompleteListener { task ->
                Toast.makeText(baseContext, "ENTROU AQUI", Toast.LENGTH_SHORT).show()
                if (task.isSuccessful) {
                    val genericResp = gson.fromJson(
                        task.result,
                        FunctionsGenericResponse::class.java
                    )

                    println(genericResp.payload)

                    val courses = Klaxon()
                        .parseArray<MonitorData>(genericResp.payload.toString())
                    mAdapter = MonitorCoursesAdapter(courses!! as MutableList<MonitorData>, user)

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
    }

    private fun getMonitorCourses(uid: String): Task<String> {

        val data = hashMapOf("uid" to uid)
        return functions
            .getHttpsCallable("getMonitorCoursesMobile")
            .call(data)
            .continueWith { task ->
                gson.toJson(task.result?.data)
            }
    }
}