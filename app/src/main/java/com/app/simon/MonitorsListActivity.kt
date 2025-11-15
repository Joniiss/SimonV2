package com.app.simon

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.simon.adapter.MonitorsAdapter
import com.app.simon.data.MonitorData
import com.app.simon.data.User
import com.app.simon.databinding.ActivityMonitorsListBinding
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.functions
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken

class MonitorsListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMonitorsListBinding
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mAdapter: MonitorsAdapter

    private lateinit var functions: FirebaseFunctions
    private val gson: Gson = GsonBuilder()
        .enableComplexMapKeySerialization()
        .create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMonitorsListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        functions = Firebase.functions("southamerica-east1")

        mRecyclerView = binding.rvMonitors
        mAdapter = MonitorsAdapter(mutableListOf())
        mRecyclerView.layoutManager = LinearLayoutManager(this)
        mRecyclerView.adapter = mAdapter

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom)
            insets
        }

        val user = intent.getSerializableExtra("user") as User
        val courseId = intent.getStringExtra("courseId")
        if (courseId.isNullOrBlank()) {
            finish()
            return
        }

        val courseName = intent.getStringExtra("courseName")
        binding.tvSubject.text = courseName

        binding.header.setOnClickListener {
            finish()
        }

        getMonitors(courseId)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val raw = task.result ?: ""
                    Log.d("MONITORS", "raw[0..120]=${raw.take(120)}")

                    try {
                        val monitors: MutableList<MonitorData> = parseMonitorsFlexible(raw)

                        mAdapter = MonitorsAdapter(monitors)
                        mRecyclerView.adapter = mAdapter

                        if (monitors.isEmpty()) {
                            Toast.makeText(this, "Nenhum monitor encontrado.", Toast.LENGTH_SHORT).show()
                        } else {
                            val firstDay = monitors.firstOrNull()
                                ?.horarioDisponivel
                                ?.firstOrNull()
                                ?.day
                            Log.d("MONITORS", "Primeiro dia disponível (se existir): $firstDay")
                        }
                    } catch (e: Exception) {
                        Log.e("MONITORS", "Falha ao parsear monitores", e)
                        Toast.makeText(this, "Falha ao ler dados dos monitores.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val e = task.exception
                    if (e is FirebaseFunctionsException) {
                        Toast.makeText(this, e.code.toString(), Toast.LENGTH_SHORT).show()
                        Toast.makeText(this, (e.details ?: "").toString(), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, e?.message ?: "Erro desconhecido", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        binding.ivForum.setOnClickListener {
            val iForum = Intent(this, ForumActivity::class.java)
            iForum.putExtra("courseId", courseId)
            iForum.putExtra("courseName", courseName)
            iForum.putExtra("user", user)
            startActivity(iForum)
        }

        binding.ivBoard.setOnClickListener {
            val iMural = Intent(this, MuralActivity::class.java)
            iMural.putExtra("courseId", courseId)
            iMural.putExtra("courseName", courseName)
            iMural.putExtra("user", user)
            startActivity(iMural)
        }
    }

    private fun getMonitors(subjectId: String): Task<String> {
        val data = hashMapOf("courseId" to subjectId)
        return functions
            .getHttpsCallable("getCourseMonitorsMobile")
            .call(data)
            .continueWith { task ->
                gson.toJson(task.result?.data)
            }
    }

    private fun parseMonitorsFlexible(raw: String): MutableList<MonitorData> {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return mutableListOf()

        val first = trimmed.first()
        if (first != '{' && first != '[') {
            Log.e("MONITORS", "Conteúdo não-JSON: ${trimmed.take(120)}")
            throw IllegalStateException("Retorno não-JSON da Function")
        }

        val root: JsonElement = gson.fromJson(trimmed, JsonElement::class.java)
        val listType = object : TypeToken<MutableList<MonitorData>>() {}.type

        return when {
            root.isJsonArray -> {
                gson.fromJson(trimmed, listType)
            }
            root.isJsonObject -> {
                val obj: JsonObject = root.asJsonObject
                val payloadEl: JsonElement? = obj.get("payload")

                if (payloadEl == null || payloadEl.isJsonNull) {
                    val single = gson.fromJson(obj, MonitorData::class.java)
                    mutableListOf(single)
                } else when {
                    payloadEl.isJsonArray -> {
                        gson.fromJson(payloadEl.toString(), listType)
                    }
                    payloadEl.isJsonObject -> {
                        mutableListOf(gson.fromJson(payloadEl.toString(), MonitorData::class.java))
                    }
                    payloadEl.isJsonPrimitive -> {
                        val s = payloadEl.asString?.trim().orEmpty()
                        if (s.startsWith("[")) {
                            gson.fromJson(s, listType)
                        } else if (s.startsWith("{")) {
                            mutableListOf(gson.fromJson(s, MonitorData::class.java))
                        } else {
                            Log.e("MONITORS", "payload string não é JSON: ${s.take(120)}")
                            mutableListOf()
                        }
                    }
                    else -> {
                        mutableListOf()
                    }
                }
            }
            else -> {
                mutableListOf()
            }
        }
    }
}
