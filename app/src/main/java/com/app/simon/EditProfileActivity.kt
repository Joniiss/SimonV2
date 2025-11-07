package com.app.simon

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.app.simon.data.HorariosData
import com.app.simon.data.MonitorData
import com.app.simon.data.UpdateData
import com.app.simon.data.User
import com.app.simon.databinding.ActivityEditProfileBinding
import com.app.simon.databinding.ActivityProfileBinding
import com.beust.klaxon.Klaxon
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.functions
import com.google.gson.GsonBuilder
import kotlinx.coroutines.awaitAll

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding

    private lateinit var scheduleContainer: LinearLayout
    private lateinit var editScheduleContainer: LinearLayout

    private lateinit var functions: FirebaseFunctions

    private val gson = GsonBuilder().enableComplexMapKeySerialization().create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        functions = Firebase.functions("southamerica-east1") // Initialize here


        scheduleContainer = findViewById(R.id.subjectsContainer)
        editScheduleContainer = findViewById(R.id.editScheduleContainer)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val user = intent.getSerializableExtra("user") as User

        Glide.with(this)
            .load(user.foto)
            .into(binding.profileImage)

        binding.tvNome.text = user.nome
        binding.etPredio.hint = user.predio
        binding.etSala.hint = user.sala

        binding.btnChangeImage.setOnClickListener {
            Toast.makeText(this, "Clicou", Toast.LENGTH_SHORT).show()
        }

        binding.btnUpdateLocal.setOnClickListener {
            val teste = mapOf(
                "horarioDisponivel" to HorariosData("Ter", arrayOf(10, 11, 12)),

            )
            updateMonitor("MlQdNQEy0bgJn7b6HNjhEg7GaJx2", "217253", teste)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Horários salvos com sucesso!", Toast.LENGTH_SHORT).show()
                    } else {
                        println(task.exception)
                        Toast.makeText(this, "Erro ao salvar horários.", Toast.LENGTH_SHORT).show()
                    }
                }
        }
        
        binding.btnSalvar.setOnClickListener {
            Toast.makeText(this, "Clicou", Toast.LENGTH_SHORT).show()
        }
        
        binding.btnClose.setOnClickListener {
            finish()
        }

        getMonitorCourses(user.uid)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val jsonResponse = task.result
                    val genericResp =
                        gson.fromJson(jsonResponse, FunctionsGenericResponse::class.java)

                    // Using Klaxon to parse the payload into your data class
                    val courses = Klaxon().parseArray<MonitorData>(genericResp.payload.toString())

                    if (courses != null) {
                        for (course in courses) {
                            // --- FIX STARTS HERE ---
                            // The 'horarioDisponivel' from Klaxon is likely a List<HorariosData>
                            // where the 'time' property is a List of Longs or Ints, not an Array<Int>.
                            // We need to map it to the correct type.

                            val scheduleAsArrayList =
                                ArrayList(course.horarioDisponivel.map { scheduleItem ->
                                    // Ensure the 'time' property is correctly typed as Array<Int>
                                    val timeAsIntArray =
                                        scheduleItem.time.map { it.toString().toInt() }
                                            .toTypedArray()
                                    HorariosData(scheduleItem.day, timeAsIntArray)
                                })
                            // --- FIX ENDS HERE ---

                            // Now, pass the correctly typed data to addSubject
                            addSubject(course.disciplina, course.disciplinaId,scheduleAsArrayList)
                        }
                    }
                } else {
                    // Handle the error, for example, by showing a Toast
                    val e = task.exception
                    if (e is FirebaseFunctionsException) {
                        val code = e.code
                        val details = e.details
                        Toast.makeText(
                            baseContext,
                            "Error fetching courses: $code",
                            Toast.LENGTH_LONG
                        ).show()
                        println("Firebase Functions Error: $code, Details: $details")
                    } else {
                        Toast.makeText(baseContext, "An unknown error occurred.", Toast.LENGTH_LONG)
                            .show()
                        println("Error: ${e?.message}")
                    }
                }
            }

        addSubject("abuble","217253", arrayListOf(HorariosData("Ter", arrayOf(10, 11, 12)), HorariosData("Qua", arrayOf(10, 11, 15))))
    }

    private fun addScheduleLineView(container: LinearLayout, scheduleData: HorariosData? = null) {
        val scheduleDayView = layoutInflater.inflate(R.layout.item_schedule_line, container, false)
        val spDia = scheduleDayView.findViewById<Spinner>(R.id.spinnerDia)
        val spHoraInicio = scheduleDayView.findViewById<Spinner>(R.id.spinnerHoraInicio)
        val spHoraFim = scheduleDayView.findViewById<Spinner>(R.id.spinnerHoraFim)
        val btnDeleteHorario = scheduleDayView.findViewById<ImageButton>(R.id.btnDeleteHorario)

        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            )
            setBackgroundColor(Color.parseColor("#CCCCCC"))
        }

        btnDeleteHorario.setOnClickListener {
            container.removeView(scheduleDayView)
            container.removeView(divider)
        }

        if (scheduleData != null) {
            val diasArray = resources.getStringArray(R.array.dias_semana)
            val horasArray = resources.getStringArray(R.array.horarios_dia)

            val diaPosition = diasArray.indexOf(scheduleData.day)
            if (diaPosition != -1) {
                spDia.setSelection(diaPosition)
            }

            // --- FIX IS HERE ---
            // Convert the number to an Int first, and then to a String.
            // This prevents "10" from being converted to "10.0".
            val horaInicioString = scheduleData.time.first().toInt().toString()
            val horaFimString = scheduleData.time.last().toInt().toString()

            println(horaInicioString)
            println(horasArray[0])

            val horaInicioPosition = horasArray.indexOf(horaInicioString)
            if (horaInicioPosition != -1) {
                spHoraInicio.setSelection(horaInicioPosition)
            }

            val horaFimPosition = horasArray.indexOf(horaFimString)
            if (horaFimPosition != -1) {
                spHoraFim.setSelection(horaFimPosition)
            }
        }

        container.addView(scheduleDayView)
        container.addView(divider)
    }



    private fun addSubject(name: String, disciplinaId: String, schedule: ArrayList<HorariosData>) {
        val itemView = layoutInflater.inflate(R.layout.item_subject_schedule, scheduleContainer, false)
        val tvName = itemView.findViewById<TextView>(R.id.tvSubjectName)
        val ivExpand = itemView.findViewById<ImageView>(R.id.ivExpandIcon)
        val scheduleContainera = itemView.findViewById<LinearLayout>(R.id.editScheduleContainer)
        val btnAddHorario = itemView.findViewById<LinearLayout>(R.id.btnAddHorarioMateria)
        val btnSaveSchedule = itemView.findViewById<LinearLayout>(R.id.btnSaveSchedule)

        tvName.text = name

        // Clear previous views and populate from the initial schedule data
//        print(name + ": ")
//        println(schedule)
        scheduleContainera.removeAllViews()
        for (day in schedule) {
            addScheduleLineView(scheduleContainera, day)
        }

        // Set the click listener for the "Adicionar Horário" button
        btnAddHorario.setOnClickListener {
            addScheduleLineView(scheduleContainera)
        }

        // --- New code for the Save button listener ---
        btnSaveSchedule.setOnClickListener {
            val newScheduleList = ArrayList<HorariosData>()
            val childCount = scheduleContainera.childCount

            // Loop through all views in the container
            for (i in 0 until childCount) {
                val view = scheduleContainera.getChildAt(i)
                // We only care about the R.layout.item_schedule_line views, not the dividers
                // A simple way to check is to see if we can find a spinner inside
                val spDia = view.findViewById<Spinner>(R.id.spinnerDia)
                if (spDia != null) {
                    // This view is a schedule line
                    val spHoraInicio = view.findViewById<Spinner>(R.id.spinnerHoraInicio)
                    val spHoraFim = view.findViewById<Spinner>(R.id.spinnerHoraFim)

                    // Get selected values from spinners
                    val day = spDia.selectedItem.toString()
                    val horaInicio = spHoraInicio.selectedItem.toString().toInt()
                    val horaFim = spHoraFim.selectedItem.toString().toInt()

                    // Create the array of hours (inclusive)
                    val timeArray = (horaInicio..horaFim).toList().toTypedArray()

                    // Create the HorariosData object and add to the list
                    newScheduleList.add(HorariosData(day, timeArray))
                }
            }
            println("Updated schedule for $name: $newScheduleList")

            val newScheduleJsonString = gson.toJson(newScheduleList)
            val schedule = mapOf(
                "horarioDisponivel" to newScheduleJsonString
            )
            val user = intent.getSerializableExtra("user") as User
            updateMonitorSchedule(user.uid, disciplinaId, schedule)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Horários salvos com sucesso!", Toast.LENGTH_SHORT).show()
                    } else {
                        println(task.exception)
                        Toast.makeText(this, "Erro ao salvar horários.", Toast.LENGTH_SHORT).show()
                    }
                }
        }
        // --- End of new code ---

        itemView.findViewById<LinearLayout>(R.id.headerLayout).setOnClickListener {
            if (scheduleContainera.visibility == View.GONE) {
                scheduleContainera.visibility = View.VISIBLE
                ivExpand.setImageResource(R.drawable.ic_expand_less)
                btnAddHorario.visibility = View.VISIBLE
                btnSaveSchedule.visibility = View.VISIBLE
            } else {
                scheduleContainera.visibility = View.GONE
                ivExpand.setImageResource(R.drawable.ic_expand_more)
                btnAddHorario.visibility = View.GONE
                btnSaveSchedule.visibility = View.GONE
            }
        }

        scheduleContainer.addView(itemView)
    }

    private fun getMonitorCourses(uid: String): Task<String> {
        val data = hashMapOf("uid" to uid)
        return functions
            .getHttpsCallable("getMonitorCoursesMobile")
            .call(data)
            .continueWith { task ->
                // Throws an exception if the task failed, which will be handled by the caller
                if (!task.isSuccessful) {
                    throw task.exception!!
                }
                gson.toJson(task.result?.data)
            }
    }

    private fun updateMonitor(uid: String, disciplinaId: String, updates: Any): Task<String> {
        val data = hashMapOf("uid" to uid,
            "disciplinaId" to disciplinaId,
            "updates" to updates)
        return functions
            .getHttpsCallable("updateMonitorMobile")
            .call(data)
            .continueWith { task ->
                // Throws an exception if the task failed, which will be handled by the caller
                if (!task.isSuccessful) {
                    throw task.exception!!
                }
                gson.toJson(task.result?.data)
            }
    }

    private fun updateMonitorSchedule(uid: String, disciplinaId: String, schedule: Any): Task<String> {
        val data = hashMapOf("uid" to uid,
            "disciplinaId" to disciplinaId,
            "schedule" to schedule)
        return functions
            .getHttpsCallable("updateMonitorScheduleMobile")
            .call(data)
            .continueWith { task ->
                // Throws an exception if the task failed, which will be handled by the caller
                if (!task.isSuccessful) {
                    throw task.exception!!
                }
                gson.toJson(task.result?.data)
            }
    }

}