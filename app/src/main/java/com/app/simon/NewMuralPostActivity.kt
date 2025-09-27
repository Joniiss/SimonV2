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
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class NewMuralPostActivity : AppCompatActivity() {

    private lateinit var containerImagens: GridLayout
    private lateinit var containerArquivos: LinearLayout
    private lateinit var containerVideos: LinearLayout

    companion object {
        private const val REQUEST_IMAGE = 1001
        private const val REQUEST_FILE = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_new_mural_post)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        containerImagens = findViewById(R.id.containerImagens)
        containerArquivos = findViewById(R.id.containerArquivos)
        containerVideos = findViewById(R.id.containerVideos)

        val btnAddImagem = findViewById<AppCompatButton>(R.id.btnAddImagem)
        val btnAddArquivo = findViewById<AppCompatButton>(R.id.btnAddArquivo)
        val btnAddVideo = findViewById<AppCompatButton>(R.id.btnAddVideo)

        btnAddImagem.setOnClickListener { pickImagesFromGallery() }
        btnAddArquivo.setOnClickListener { pickFiles() }
        btnAddVideo.setOnClickListener { addVideoInputField() }
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
