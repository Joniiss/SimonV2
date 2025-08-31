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

class CoursesListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCoursesListBinding
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mAdapter: CoursesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityCoursesListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //setContentView(R.layout.activity_courses_list)

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



        val testList = mutableListOf<SubjectData>(
            SubjectData("teste1"),
            SubjectData("teste2"),
            SubjectData("teste3")
        )
        mAdapter = CoursesAdapter(testList)

        mRecyclerView.layoutManager = LinearLayoutManager(this)
        mRecyclerView.adapter = mAdapter

        if(mAdapter.itemCount == 0){
            Toast.makeText(baseContext, "Olá tudo bem?", Toast.LENGTH_SHORT).show()
        }
    }


}
