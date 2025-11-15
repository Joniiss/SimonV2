package com.app.simon

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.app.simon.adapter.MonitorsAdapter.MonitorsViewHolder.ProximoHorarioSlot
import com.app.simon.adapter.toDayOfWeek
import com.app.simon.data.HorariosData
import com.app.simon.data.MonitorData
import com.app.simon.databinding.ActivityProfileBinding
import com.beust.klaxon.Klaxon
import com.bumptech.glide.Glide
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.functions
import com.google.gson.GsonBuilder
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

class ProfileActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var subjectsContainer: LinearLayout
    private lateinit var functions: FirebaseFunctions
    private val gson = GsonBuilder().enableComplexMapKeySerialization().create()
    private var googleMap: GoogleMap? = null
    private lateinit var location: LatLng
    private lateinit var monitor: MonitorData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        functions = Firebase.functions("southamerica-east1")

        subjectsContainer = findViewById(R.id.subjectsContainer)

        monitor = intent.getSerializableExtra("monitor") as MonitorData
        location = LatLng(monitor.geoLoc!!.latitude, monitor.geoLoc!!.longitude)

        binding.tvNome.text = monitor.nome
        binding.tvLocalValue.text = "${monitor.local} - ${monitor.sala}"
        val prox = encontrarProximoHorario(monitor.horarioDisponivel)
        println("O próximo horário disponível é: ${prox!!.format()}")
        binding.tvHorarioValue.text = prox!!.format()
        print(monitor.status)
        binding.statusDot.background = when (monitor.status) {
            true -> getDrawable(R.drawable.status_dot_online)
            false -> getDrawable(R.drawable.bg_status_circle)
            else -> getDrawable(R.drawable.bg_status_circle)
        }

        Glide.with(this)
            .load(monitor.foto)
            .into(binding.profileImage)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        getMonitorCourses(monitor.uid)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val genericResp = gson.fromJson(
                        task.result,
                        FunctionsGenericResponse::class.java
                    )

                    println(genericResp.payload)

                    val courses = Klaxon()
                        .parseArray<MonitorData>(genericResp.payload.toString())

                    for (course in courses!!) {
                        addSubject(course.disciplina, course.horarioDisponivel)
                    }
                }
                else {
                    val e = task.exception
                    if (e is FirebaseFunctionsException) {
                        val code = e.code
                        println(code)
                        Toast.makeText(baseContext, code.toString(), Toast.LENGTH_SHORT).show()

                        val details = e.details
                        println(details)
                        Toast.makeText(baseContext, details.toString(), Toast.LENGTH_SHORT).show()

                    }
                }
            }
        binding.btnClose.setOnClickListener {
            finish()
        }

        binding.btnIniciarConversa.setOnClickListener {
            iniciarChat()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.addMarker(
            MarkerOptions()
                .position(location)
                .title("Local do Monitor")
        )
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 16f))
    }

    private fun getMonitorCourses(uid: String): Task<String> {

        val data = hashMapOf("uid" to uid)
        return functions
            .getHttpsCallable("getMonitorCoursesMobile")
            .call(data)
            .continueWith { task ->
                gson.toJson(task.result?.data)
            }
    }

    private fun addSubject(name: String, schedule: ArrayList<HorariosData>) {
        val itemView = layoutInflater.inflate(R.layout.item_subject, subjectsContainer, false)
        val tvName = itemView.findViewById<TextView>(R.id.tvSubjectName)
        val ivExpand = itemView.findViewById<ImageView>(R.id.ivExpandIcon)
        val scheduleContainer = itemView.findViewById<LinearLayout>(R.id.scheduleContainer)

        tvName.text = name

        scheduleContainer.removeAllViews()
        for (day in schedule) {
            println(day)
            val tv = TextView(this).apply {
                this.text = "${day.day}: Das ${day.time[0]}:00 às ${day.time[day.time.lastIndex]}:00"
                this.textSize = 14f
                this.setTextColor(Color.parseColor("#333333"))
                this.setPadding(0, 4, 0, 4)
            }
            scheduleContainer.addView(tv)

            val divider = View(this).apply {
                this.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1
                )
                this.setBackgroundColor(Color.parseColor("#CCCCCC"))
            }
            scheduleContainer.addView(divider)
        }

        itemView.findViewById<LinearLayout>(R.id.headerLayout).setOnClickListener {
            if (scheduleContainer.visibility == View.GONE) {
                scheduleContainer.visibility = View.VISIBLE
                ivExpand.setImageResource(R.drawable.ic_expand_less)
            } else {
                scheduleContainer.visibility = View.GONE
                ivExpand.setImageResource(R.drawable.ic_expand_more)
            }
        }

        subjectsContainer.addView(itemView)
    }

    fun encontrarProximoHorario(horariosDisponiveis: List<HorariosData>): ProximoHorarioSlot? {
        if (horariosDisponiveis.isEmpty()) {
            return null
        }

        val allIndividualSlots = mutableListOf<ProximoHorarioSlot>()
        for (horarioData in horariosDisponiveis) {
            val dayOfWeek = horarioData.day.toDayOfWeek()

            val slotsValidos = horarioData.time.toList().dropLast(1)

            for (hora in slotsValidos) {
                allIndividualSlots.add(ProximoHorarioSlot(dayOfWeek, hora))
            }
        }

        if (allIndividualSlots.isEmpty()) {
            return null
        }

        val agora = ZonedDateTime.now(ZoneId.systemDefault())

        var proximoSlotEncontrado: ProximoHorarioSlot? = null
        var menorDuracaoAteProximo = Long.MAX_VALUE

        for (slot in allIndividualSlots) {
            var dataHoraPotencialSlot = agora
                .withHour(slot.hora)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)

            val currentDayValue = agora.dayOfWeek.value
            val slotDayValue = slot.diaOfWeek.value

            if (slotDayValue < currentDayValue) {
                dataHoraPotencialSlot = dataHoraPotencialSlot.plusWeeks(1)
                dataHoraPotencialSlot = dataHoraPotencialSlot.with(TemporalAdjusters.nextOrSame(slot.diaOfWeek))
            } else if (slotDayValue > currentDayValue) {
                dataHoraPotencialSlot = dataHoraPotencialSlot.with(TemporalAdjusters.nextOrSame(slot.diaOfWeek))
            } else {
                if (slot.hora < agora.hour || (slot.hora == agora.hour && agora.minute > 0)) {
                    dataHoraPotencialSlot = dataHoraPotencialSlot.plusWeeks(1)
                    dataHoraPotencialSlot = dataHoraPotencialSlot.with(TemporalAdjusters.nextOrSame(slot.diaOfWeek))
                } else {
                    dataHoraPotencialSlot = dataHoraPotencialSlot.with(TemporalAdjusters.nextOrSame(slot.diaOfWeek))
                }
            }

            if (dataHoraPotencialSlot.isAfter(agora)) {
                val duracao = ChronoUnit.MINUTES.between(agora, dataHoraPotencialSlot)
                if (duracao < menorDuracaoAteProximo) {
                    menorDuracaoAteProximo = duracao
                    proximoSlotEncontrado = slot
                }
            }
        }

        return proximoSlotEncontrado
    }

    private fun iniciarChat() {
        val me = FirebaseAuth.getInstance().currentUser?.uid
        val other = monitor.uid
        if (me.isNullOrBlank() || other.isNullOrBlank()) {
            Toast.makeText(this, "Usuário inválido para iniciar chat.", Toast.LENGTH_SHORT).show()
            return
        }

        val channelId = directChannelId(me, other)

        val db = FirebaseFirestore.getInstance()
        val ref = db.collection("Chats").document(channelId)

        db.runTransaction { tx ->
            val snap = tx.get(ref)
            if (!snap.exists()) {
                val members = listOf(me, other)
                val data = hashMapOf(
                    "type" to "direct",
                    "members" to members,
                    "createdBy" to me,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "lastMessage" to "",
                    "lastMessageAt" to FieldValue.serverTimestamp()
                )
                tx.set(ref, data)
            }
            null
        }.addOnSuccessListener {
            val it = Intent(this, ChatActivity::class.java)
            it.putExtra(ChatActivity.EXTRA_CHANNEL_ID, channelId)
            startActivity(it)
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Falha ao iniciar chat: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun directChannelId(a: String, b: String): String {
        return if (a < b) "${a}_$b" else "${b}_$a"
    }

}