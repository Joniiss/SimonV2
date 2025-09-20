package com.app.simon

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.app.simon.data.ForumPostData
import com.app.simon.data.User
import com.app.simon.databinding.ActivityForumBinding
import com.app.simon.databinding.ActivityNewForumPostBinding
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.functions
import com.google.gson.GsonBuilder
import java.time.Instant
import java.time.format.DateTimeFormatter

class NewForumPostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewForumPostBinding

    private lateinit var functions: FirebaseFunctions
    private val gson = GsonBuilder().enableComplexMapKeySerialization().create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityNewForumPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        functions = Firebase.functions("southamerica-east1")

        val user = intent.getSerializableExtra("user") as User
        val courseId = intent.getStringExtra("courseId")

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnEnviar.setOnClickListener {
            val currentInstant = Instant.now()
            addForumPost(ForumPostData(
                title = binding.etTitulo.text.toString(),
                userName = user.nome,
                userId = user.uid,
                likes = 0,
                createdAt = DateTimeFormatter.ISO_INSTANT.format(currentInstant).toString(),
                courseId = courseId!!,
                content = binding.etCorpo.text.toString(),
                comments = emptyArray()
            )).addOnCompleteListener { task ->
                println(task.isSuccessful)
                if (task.isSuccessful) {
                    val genericResp = gson.fromJson(
                        task.result,
                        FunctionsGenericResponse::class.java
                    )
                    println(genericResp.message)
                    println(genericResp.payload)

                    val intent = Intent(this, ForumActivity::class.java)
                    intent.putExtra("user", user)
                    intent.putExtra("courseId", courseId)
                    startActivity(intent)
                    finish()
                }
                else {
                    val e = task.exception
                    println(e)
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
                    else {
                        println("false")
                    }
                }

            }
        }
    }

    private fun addForumPost(post: ForumPostData): Task<String> {

        val data = hashMapOf("title" to post.title,
            "userName" to post.userName,
            "userId" to post.userId,
            "likes" to post.likes,
            "createdAt" to post.createdAt,
            "courseId" to post.courseId,
            "content" to post.content
            )
        return functions
            .getHttpsCallable("createForumPostMobile")
            .call(data)
            .continueWith { task ->
                gson.toJson(task.result?.data)
            }
    }
}