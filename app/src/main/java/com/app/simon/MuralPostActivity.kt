package com.app.simon

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.app.simon.adapter.ImagePagerAdapter
import com.app.simon.data.MuralPostData
import com.app.simon.data.User
import com.app.simon.databinding.ActivityMuralPostBinding

class MuralPostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMuralPostBinding
    private var imageAdapter: ImagePagerAdapter? = null

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

        val images: List<Uri> = post.images.toList().map { Uri.parse(it) }
        if (images.isNotEmpty()) {
            imageAdapter = ImagePagerAdapter(this, images)
            binding.ivAnexo.adapter = imageAdapter
        }

        post.videos.toList().forEach { link ->
            val tv = TextView(this).apply {
                text = link
                setTextColor(ContextCompat.getColor(context, android.R.color.holo_blue_dark))
                setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                    startActivity(intent)
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 4
                    bottomMargin = 4
                }
            }
            binding.flVideos.addView(tv)
        }

        post.files.toList().forEach { link ->
            val tv = TextView(this).apply {
                println(link.split("%2Ffiles%")[1].split("?alt")[0].replace("%", " "))
                text = link.split("%2Ffiles%")[1].split("?alt")[0].replace("%", " ")
                setTextColor(ContextCompat.getColor(context, android.R.color.holo_blue_dark))
                setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                    startActivity(intent)
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 4
                    bottomMargin = 4
                }
            }
            binding.flArquivos.addView(tv)
        }
    }
}
