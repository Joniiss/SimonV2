package com.app.simon

import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.card.MaterialCardView

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        // sombra azul customizada
        val shadowColor = 0xAD0C2E92.toInt() // #0C2E92AD

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val cardMaterias = findViewById<MaterialCardView>(R.id.cardMaterias)
            val cardCandidatar = findViewById<MaterialCardView>(R.id.cardCandidatar)
            val cardMinhasMonitorias = findViewById<MaterialCardView>(R.id.cardMinhasMonitorias)

            listOf(cardMaterias, cardCandidatar, cardMinhasMonitorias).forEach { card ->
                card.outlineSpotShadowColor = shadowColor
                card.outlineAmbientShadowColor = shadowColor
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}