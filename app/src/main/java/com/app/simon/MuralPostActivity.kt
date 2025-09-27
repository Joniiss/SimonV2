package com.app.simon

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.app.simon.data.ForumData
import com.app.simon.data.MuralPostData
import com.app.simon.data.User
import com.app.simon.databinding.ActivityMuralPostBinding

class MuralPostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMuralPostBinding



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMuralPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val user = intent.getSerializableExtra("user") as User
        val post = intent.getSerializableExtra("post") as MuralPostData

        binding.tvAutor.text = post.userName
        binding.tvTitulo.text = post.title
        binding.tvDescricao.text = post.content
    }
}