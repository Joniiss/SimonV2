package com.app.simon

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.InputType
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.app.simon.data.MuralPostData
import com.app.simon.data.User
import com.app.simon.databinding.ActivityNewMuralPostBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID

class NewMuralPostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewMuralPostBinding

    private lateinit var containerImagens: GridLayout
    private lateinit var containerArquivos: LinearLayout
    private lateinit var containerVideos: LinearLayout

    private val selectedImageUris = mutableListOf<Uri>()
    private val selectedFileUris = mutableListOf<Uri>()

    private val videoLinks = mutableListOf<String>()

    private lateinit var storage: FirebaseStorage
    private lateinit var firestore: com.google.firebase.firestore.FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    companion object {
        private const val REQUEST_IMAGE = 1001
        private const val REQUEST_FILE = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityNewMuralPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        storage = FirebaseStorage.getInstance()
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val user = intent.getSerializableExtra("user") as User
        val courseId = intent.getStringExtra("courseId")

        containerImagens = findViewById(R.id.containerImagens)
        containerArquivos = findViewById(R.id.containerArquivos)
        containerVideos = findViewById(R.id.containerVideos)

        val btnAddImagem = findViewById<AppCompatButton>(R.id.btnAddImagem)
        val btnAddArquivo = findViewById<AppCompatButton>(R.id.btnAddArquivo)
        val btnAddVideo = findViewById<AppCompatButton>(R.id.btnAddVideo)

        btnAddImagem.setOnClickListener { pickImagesFromGallery() }
        btnAddArquivo.setOnClickListener { pickFiles() }
        btnAddVideo.setOnClickListener { addVideoInputField() }

        binding.btnPublicar.setOnClickListener {
            println("Clicou publicar")
            println(selectedImageUris)
            println(selectedFileUris)

            val title = binding.etTitulo.text.toString()
            val content = binding.etDescricao.text.toString()

            binding.btnPublicar.isEnabled = false
            videoLinks.clear()

            for (i in 0 until containerVideos.childCount) {
                val view = containerVideos.getChildAt(i)
                if (view is EditText) {
                    val videoUrl = view.text.toString().trim()
                    if (videoUrl.isNotEmpty()) { // Optional: only add non-empty links
                        videoLinks.add(videoUrl)
                    }
                }
            }

            uploadFilesAndCreatePost(title, content, user, courseId)
        }
    }

    private fun uploadFilesAndCreatePost(
        title: String,
        content: String,
        user: User?,
        courseId: String?
    ) {
        lifecycleScope.launch {
            try {
                val imageUrls = mutableListOf<String>()
                val fileUrls = mutableListOf<String>()

                for (imageUri in selectedImageUris) {
                    val downloadUrl = uploadFileToStorage(imageUri, "uploads/images/${UUID.randomUUID()}")
                    if (downloadUrl != null) {
                        imageUrls.add(downloadUrl)
                    }
                }

                for (fileUri in selectedFileUris) {
                    val millis = System.currentTimeMillis()
                    val fileName = getFileName(fileUri) ?: "file_${UUID.randomUUID()}"
                    val downloadUrl = uploadFileToStorage(fileUri, "uploads/files/${millis}_${fileName}")
                    if (downloadUrl != null) {
                        fileUrls.add(downloadUrl)
                    }
                }

                val postId = firestore.collection("MuralPosts").document().id

                val currentInstant = Instant.now()
                val newPost = MuralPostData(
                    title = title,
                    content = content,
                    disciplinaId = courseId!!,
                    userName = user!!.nome,
                    createdAt = DateTimeFormatter.ISO_INSTANT.format(currentInstant).toString(),
                    files = fileUrls,
                    images = imageUrls,
                    videos = videoLinks
                )

                firestore.collection("MuralPosts")
                    .document(postId)
                    .set(newPost)
                    .await()

                Toast.makeText(this@NewMuralPostActivity, "Post criado com sucesso!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@NewMuralPostActivity, MuralActivity::class.java)
                intent.putExtra("user", user)
                intent.putExtra("courseId", courseId)
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@NewMuralPostActivity, "Erro ao criar post!", Toast.LENGTH_SHORT).show()
                binding.btnPublicar.isEnabled = true
            }
        }
    }

    private suspend fun uploadFileToStorage(uri: Uri, storagePath: String): String? {
        return try {
            val storageRef = storage.reference.child(storagePath)
            val uploadTask = storageRef.putFile(uri).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await()
            downloadUrl.toString()
        } catch (e: Exception) {
            Toast.makeText(this@NewMuralPostActivity, "Erro ao fazer upload do arquivo", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun pickImagesFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(intent, REQUEST_IMAGE)
    }

    private fun pickFiles() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(intent, REQUEST_FILE)
    }

    @SuppressLint("Range")
    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    name = it.getString(index)
                }
            }
        }
        return name
    }
    private fun addVideoInputField() {
        val editText = EditText(this)
        editText.hint = "Cole o link do vídeo"
        editText.inputType = InputType.TYPE_TEXT_VARIATION_URI
        editText.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = 8
            bottomMargin = 8
        }
        editText.setPadding(16, 16, 16, 16)
        editText.background = ContextCompat.getDrawable(this, R.drawable.bg_input)

        containerVideos.addView(editText)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && data != null) {

            val uris = mutableListOf<Uri>()

            if (data.clipData != null) {
                val count = data.clipData!!.itemCount
                for (i in 0 until count) {
                    val uri = data.clipData!!.getItemAt(i).uri
                    uris.add(uri)
                }
            } else if (data.data != null) {
                uris.add(data.data!!)
            }

            when (requestCode) {
                REQUEST_IMAGE -> {
                    for (uri in uris) {
                        selectedImageUris.add(uri)
                        val imageView = ImageView(this)
                        imageView.setImageURI(uri)
                        val size = resources.displayMetrics.widthPixels / 6 - 16
                        val params = GridLayout.LayoutParams()
                        params.width = size
                        params.height = size
                        params.setMargins(4, 4, 4, 4)
                        imageView.layoutParams = params
                        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                        containerImagens.addView(imageView)
                    }
                }

                REQUEST_FILE -> {
                    for (uri in uris) {
                        selectedFileUris.add(uri)
                        val fileName = getFileName(uri) ?: "Arquivo sem nome"
                        val textView = EditText(this)
                        textView.setText(fileName)
                        textView.isEnabled = false
                        textView.setTextColor(Color.BLACK)
                        textView.layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            topMargin = 4
                            bottomMargin = 4
                        }
                        containerArquivos.addView(textView)
                    }
                }
            }
        }
    }
}
