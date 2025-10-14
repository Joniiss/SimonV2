package com.app.simon

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.app.simon.adapter.MonitorsAdapter.MonitorsViewHolder.ProximoHorarioSlot
import com.app.simon.adapter.toDayOfWeek
import com.app.simon.data.HorariosData
import com.app.simon.data.MonitorData
import com.app.simon.data.User
import com.app.simon.databinding.ActivityProfileBinding
import com.bumptech.glide.Glide
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var subjectsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
        //    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        //    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
        //    insets
        //}

        subjectsContainer = findViewById(R.id.subjectsContainer)

        val monitor = intent.getSerializableExtra("monitor") as MonitorData

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

        // EXEMPLO, JONIS USAR COMO VIER VIA JSON
        addSubject(
            "Engenharia e Elicitação de Requisitos",
            listOf("Terça-feira - 16h até 18h", "Quinta-feira - 20h até 22h")
        )

        addSubject(
            "Algoritmos e Linguagem de Programação",
            listOf("Segunda-feira - 10h até 12h", "Quarta-feira - 14h até 16h")
        )
    }

    private fun addSubject(name: String, schedules: List<String>) {
        val itemView = layoutInflater.inflate(R.layout.item_subject, subjectsContainer, false)
        val tvName = itemView.findViewById<TextView>(R.id.tvSubjectName)
        val ivExpand = itemView.findViewById<ImageView>(R.id.ivExpandIcon)
        val scheduleContainer = itemView.findViewById<LinearLayout>(R.id.scheduleContainer)

        tvName.text = name

        scheduleContainer.removeAllViews()
        for (text in schedules) {
            val tv = TextView(this).apply {
                this.text = text
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

        // 1. Gerar todos os slots de horário individuais válidos a partir de HorariosData
        val allIndividualSlots = mutableListOf<ProximoHorarioSlot>()
        for (horarioData in horariosDisponiveis) { // Itera sobre a sua HorariosData
            val dayOfWeek = horarioData.day.toDayOfWeek() // Converte a string do dia para DayOfWeek

            // Converte Array<Int> para List<Int> e então remove o último elemento (hora final)
            val slotsValidos = horarioData.time.toList().dropLast(1)

            for (hora in slotsValidos) {
                allIndividualSlots.add(ProximoHorarioSlot(dayOfWeek, hora))
            }
        }

        // Se após processar, não houver slots válidos, retorna null
        if (allIndividualSlots.isEmpty()) {
            return null
        }

        // Ponto de referência: o momento atual exato
        val agora = ZonedDateTime.now(ZoneId.systemDefault())

        var proximoSlotEncontrado: ProximoHorarioSlot? = null
        var menorDuracaoAteProximo = Long.MAX_VALUE // Para encontrar o slot mais próximo, em minutos

        // Iterar sobre todos os slots individuais para encontrar o mais próximo no futuro
        for (slot in allIndividualSlots) {
            // Começa com a data/hora atual e ajusta para o dia da semana e hora do slot
            var dataHoraPotencialSlot = agora
                .withHour(slot.hora)
                .withMinute(0) // Horários são considerados no início da hora
                .withSecond(0)
                .withNano(0)

            // Determina se o slot deve ser esta semana ou a próxima.
            val currentDayValue = agora.dayOfWeek.value // Monday=1, Sunday=7
            val slotDayValue = slot.diaOfWeek.value

            if (slotDayValue < currentDayValue) {
                // Se o dia do slot é anterior ao dia atual na semana (e.g., hoje é Quarta, slot é Segunda),
                // então o slot deve ser na próxima semana.
                dataHoraPotencialSlot = dataHoraPotencialSlot.plusWeeks(1)
                dataHoraPotencialSlot = dataHoraPotencialSlot.with(TemporalAdjusters.nextOrSame(slot.diaOfWeek))
            } else if (slotDayValue > currentDayValue) {
                // Se o dia do slot é posterior ao dia atual (e.g., hoje é Segunda, slot é Quarta),
                // então o slot é para esta semana.
                dataHoraPotencialSlot = dataHoraPotencialSlot.with(TemporalAdjusters.nextOrSame(slot.diaOfWeek))
            } else { // Mesmo dia
                // Se a hora do slot já passou, ou se é a mesma hora mas os minutos atuais já avançaram,
                // então o slot é para a próxima semana (mesmo dia).
                if (slot.hora < agora.hour || (slot.hora == agora.hour && agora.minute > 0)) {
                    dataHoraPotencialSlot = dataHoraPotencialSlot.plusWeeks(1)
                    dataHoraPotencialSlot = dataHoraPotencialSlot.with(TemporalAdjusters.nextOrSame(slot.diaOfWeek))
                } else {
                    // O slot está no futuro (ou exatamente agora) no mesmo dia desta semana.
                    dataHoraPotencialSlot = dataHoraPotencialSlot.with(TemporalAdjusters.nextOrSame(slot.diaOfWeek))
                }
            }

            // Finalmente, verifica se o slot potencial está estritamente no futuro em relação ao 'agora'.
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
}