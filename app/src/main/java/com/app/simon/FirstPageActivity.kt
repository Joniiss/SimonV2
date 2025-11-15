package com.app.simon

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.app.simon.adapter.MonitorsAdapter
import com.app.simon.data.MonitorData
import com.app.simon.data.User
import com.beust.klaxon.Klaxon
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.functions
import com.google.gson.GsonBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FirstPageActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var functions: FirebaseFunctions

    private val gson = GsonBuilder().enableComplexMapKeySerialization().create()
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_first_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        auth = Firebase.auth
        functions = Firebase.functions("southamerica-east1")
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser == null) {
            lifecycleScope.launch {
                delay(1500)
                val loginDialog = LoginFragment()
                loginDialog.show(supportFragmentManager, "login_popup")
            }
        }else{
            getUser(currentUser.uid)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        println("sucesso")
                        val genericResp = gson.fromJson(
                            task.result,
                            FunctionsGenericResponse::class.java
                        )

                        val user = Klaxon()
                            .parse<User>(genericResp.payload.toString())

                        println(user!!.curso)

                        lifecycleScope.launch {
                            delay(1500)
                            val iHome = Intent(this@FirstPageActivity, HomeActivity::class.java)
                            iHome.putExtra("user", user)
                            startActivity(iHome)
                            finish()
                        }
                    }
                    else {
                        Toast.makeText(baseContext, "ERRO", Toast.LENGTH_SHORT).show()
                        val e = task.exception
                        if (e is FirebaseFunctionsException) {
                            val code = e.code
                            println(code)

                            val details = e.details
                            println(details)
                        }
                    }
                }
        }
    }

    private fun getUser(userUid: String): Task<String> {

        val data = hashMapOf(
            "uid" to userUid)
        return functions
            .getHttpsCallable("findUserMobile")
            .call(data)
            .continueWith { task ->
                gson.toJson(task.result?.data)
            }
    }

}