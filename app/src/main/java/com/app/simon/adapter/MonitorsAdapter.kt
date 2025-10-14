package com.app.simon.adapter
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.app.simon.MonitorsListActivity
import com.app.simon.ProfileActivity
import com.app.simon.R
import com.app.simon.data.HorariosData
import com.app.simon.data.MonitorData
import de.hdodenhof.circleimageview.CircleImageView
import com.bumptech.glide.Glide
import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

fun String.toDayOfWeek(): DayOfWeek {
    return when (this) {
        "Seg" -> DayOfWeek.MONDAY
        "Ter" -> DayOfWeek.TUESDAY
        "Qua" -> DayOfWeek.WEDNESDAY
        "Qui" -> DayOfWeek.THURSDAY
        "Sex" -> DayOfWeek.FRIDAY
        "Sáb" -> DayOfWeek.SATURDAY
        "Dom" -> DayOfWeek.SUNDAY
        else -> throw IllegalArgumentException("Dia da semana inválido: $this")
    }
}

// (A função toPortugueseString também é útil, mas a toDayOfWeek é a que está causando o erro aqui)
fun DayOfWeek.toPortugueseString(): String {
    return when (this) {
        DayOfWeek.MONDAY -> "Seg"
        DayOfWeek.TUESDAY -> "Ter"
        DayOfWeek.WEDNESDAY -> "Qua"
        DayOfWeek.THURSDAY -> "Qui"
        DayOfWeek.FRIDAY -> "Sex"
        DayOfWeek.SATURDAY -> "Sáb"
        DayOfWeek.SUNDAY -> "Dom"
    }
}

class MonitorsAdapter(private val mData: MutableList<MonitorData>) : RecyclerView.Adapter<MonitorsAdapter.MonitorsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonitorsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_monitor, parent, false)
        return MonitorsViewHolder(view)
    }

    override fun onBindViewHolder(holder: MonitorsViewHolder, position: Int) {
        val item = mData[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        return mData.size
    }


    class MonitorsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvmonitorName: TextView = itemView.findViewById(R.id.tvName)
        private val ivmonitorPicture: CircleImageView = itemView.findViewById(R.id.ivMonitor)
        private val tvmonitorPlace: TextView = itemView.findViewById(R.id.tvPlace)
        private val tvmonitorTime: TextView = itemView.findViewById(R.id.tvTime)
        private val btnAccess: TextView = itemView.findViewById(R.id.btnAccessMonitor)

        fun bind(item: MonitorData) {
            tvmonitorName.text = item.nome
            tvmonitorPlace.text = "${item.local} - ${item.sala}"
            val prox = encontrarProximoHorario(item.horarioDisponivel)
            println("O próximo horário disponível é: ${prox!!.format()}")
            tvmonitorTime.text = prox!!.format()
            Glide.with(itemView)
                .load(item.foto)
                .into(ivmonitorPicture)

            btnAccess.setOnClickListener {
                val iProfile = Intent(itemView.context, ProfileActivity::class.java)
                iProfile.putExtra("monitor",item)
                itemView.context.startActivity(iProfile)

                //val iCourse = Intent(itemView.context, MonitorsListActivity::class.java)
                //iCourse.putExtra("course",item.name)

                //itemView.context.startActivity(iCourse)
            }
        }

        data class HorarioProgramado(val diaString: String, val horasAtendimento: List<Int>) {
            val diaOfWeek: DayOfWeek = diaString.toDayOfWeek()
            // Não precisa de 'compareTo' aqui, pois vamos comparar slots individuais.
        }

        data class ProximoHorarioSlot(val diaOfWeek: DayOfWeek, val hora: Int) {
            fun format(): String {
                return "${diaOfWeek.toPortugueseString()} - ${String.format("%02d", hora)}:00"
            }
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
}