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
import com.app.simon.adapter.MonitorsAdapter
import com.app.simon.data.HorariosData
import com.app.simon.data.MonitorData
import com.app.simon.data.SubjectData
import com.app.simon.data.User
import com.app.simon.databinding.ActivityCoursesListBinding
import com.app.simon.databinding.ActivityMonitorsListBinding
import com.beust.klaxon.Klaxon
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.functions
import com.google.gson.GsonBuilder

class MonitorsListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMonitorsListBinding
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mAdapter: MonitorsAdapter

    private lateinit var functions: FirebaseFunctions
    private val gson = GsonBuilder().enableComplexMapKeySerialization().create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMonitorsListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        functions = Firebase.functions("southamerica-east1")

        mRecyclerView = binding.rvMonitors

        val user = intent.getSerializableExtra("user") as User

        //setContentView(R.layout.activity_monitors_list)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.header.setOnClickListener {
            val iVoltar = Intent(this, HomeActivity::class.java)
            //startActivity(iVoltar)
            finish()
        }

        mAdapter = MonitorsAdapter(mutableListOf())

        val courseId = intent.getStringExtra("courseId")
        getMonitors(courseId!!) //ES402
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val genericResp = gson.fromJson(
                        task.result,
                        FunctionsGenericResponse::class.java
                    )

                    println(genericResp.payload)

                    val monitors = Klaxon()
                        .parseArray<MonitorData>(genericResp.payload.toString())
                    mAdapter = MonitorsAdapter(monitors!! as MutableList<MonitorData>)

                    println(monitors)
                    val teste = monitors[0].horarioDisponivel[0]
                    println(monitors[0].horarioDisponivel[0].day)

                    println(teste)


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

        binding.ivForum.setOnClickListener {
            val iForum = Intent(this, ForumActivity::class.java)
            iForum.putExtra("courseId", courseId)
            iForum.putExtra("user", user)
            startActivity(iForum)
        }

        binding.ivBoard.setOnClickListener {
            val iMural = Intent(this, MuralActivity::class.java)
            iMural.putExtra("courseId", courseId)
            iMural.putExtra("user", user)
            startActivity(iMural)
        }

        if(mAdapter.itemCount == 0){
            Toast.makeText(baseContext, "Você não possui matérias!", Toast.LENGTH_LONG).show()
        }
    }

    private fun getMonitors(subjectId: String): Task<String> {

        val data = hashMapOf(
            "courseId" to subjectId)
        return functions
            .getHttpsCallable("getCourseMonitorsMobile")
            .call(data)
            .continueWith { task ->
                gson.toJson(task.result?.data)
            }
    }
}