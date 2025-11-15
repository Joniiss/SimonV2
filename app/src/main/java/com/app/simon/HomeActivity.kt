package com.app.simon

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.app.simon.data.User
import com.app.simon.databinding.ActivityHomeBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        auth = Firebase.auth

        // Inicializa o binding
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // sombra azul customizada
        val shadowColor = 0xAD0C2E92.toInt() // #0C2E92AD

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            listOf(binding.cardSubjects, binding.cardMyTutoring).forEach { card ->
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

        val user = intent.getSerializableExtra("user") as User
        Toast.makeText(baseContext, user.nome, Toast.LENGTH_SHORT).show()
        binding.tvUser.text = "Olá, ${user.nome}"

        // Clique no cardSubjects -> abrir CoursesListActivity
        binding.cardSubjects.setOnClickListener {
            val intent = Intent(this, CoursesListActivity::class.java)
            intent.putExtra("user", user)
            startActivity(intent)
        }

        // Clique no cardSubjects -> abrir CoursesListActivity
        binding.cardMyTutoring.setOnClickListener {
            val intent = Intent(this, MonitorCoursesListActivity::class.java)
            intent.putExtra("user", user)
            startActivity(intent)
        }

        binding.ivChat.setOnClickListener {
            startActivity(Intent(this, ChatsListActivity::class.java))
        }

        binding.ivUserImg.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            intent.putExtra("user", user)
            startActivity(intent)
        }

        binding.tvLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, FirstPageActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
