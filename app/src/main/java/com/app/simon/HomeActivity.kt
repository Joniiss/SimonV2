package com.app.simon

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.app.simon.databinding.ActivityHomeBinding
import com.google.android.material.card.MaterialCardView

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inicializa o binding
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // sombra azul customizada
        val shadowColor = 0xAD0C2E92.toInt() // #0C2E92AD

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            listOf(binding.cardSubjects, binding.cardEnlist, binding.cardMyTutoring).forEach { card ->
                card.outlineSpotShadowColor = shadowColor
                card.outlineAmbientShadowColor = shadowColor
            }
        }

        // Ajusta padding para as system bars
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Clique no cardSubjects -> abrir CoursesListActivity
        binding.cardSubjects.setOnClickListener {
            val intent = Intent(this, CoursesListActivity::class.java)
            startActivity(intent)
        }
    }
}
