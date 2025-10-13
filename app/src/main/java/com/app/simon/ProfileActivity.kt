package com.app.simon

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ProfileActivity : AppCompatActivity() {
    private lateinit var subjectsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        subjectsContainer = findViewById(R.id.subjectsContainer)

        // EXEMPLO, JONIS USAR COMO VIER VIA JSON
        addSubject(
            "Engenharia e Elicitação de Requisitos",
            listOf("Terça-feira - 16h até 18h", "Quinta-feira - 20h até 22h")
        )

        addSubject(
            "Algoritmos e Linguagem de Programação",
            listOf("Segunda-feira - 10h até 12h", "Quarta-feira - 14h até 16h")
        )
    }

    private fun addSubject(name: String, schedules: List<String>) {
        val itemView = layoutInflater.inflate(R.layout.item_subject, subjectsContainer, false)
        val tvName = itemView.findViewById<TextView>(R.id.tvSubjectName)
        val ivExpand = itemView.findViewById<ImageView>(R.id.ivExpandIcon)
        val scheduleContainer = itemView.findViewById<LinearLayout>(R.id.scheduleContainer)

        tvName.text = name

        scheduleContainer.removeAllViews()
        for (text in schedules) {
            val tv = TextView(this).apply {
                this.text = text
                this.textSize = 14f
                this.setTextColor(Color.parseColor("#333333"))
                this.setPadding(0, 4, 0, 4)
            }
            scheduleContainer.addView(tv)

            val divider = View(this).apply {
                this.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1
                )
                this.setBackgroundColor(Color.parseColor("#CCCCCC"))
            }
            scheduleContainer.addView(divider)
        }

        itemView.findViewById<LinearLayout>(R.id.headerLayout).setOnClickListener {
            if (scheduleContainer.visibility == View.GONE) {
                scheduleContainer.visibility = View.VISIBLE
                ivExpand.setImageResource(R.drawable.ic_expand_less)
            } else {
                scheduleContainer.visibility = View.GONE
                ivExpand.setImageResource(R.drawable.ic_expand_more)
            }
        }

        subjectsContainer.addView(itemView)
    }
}