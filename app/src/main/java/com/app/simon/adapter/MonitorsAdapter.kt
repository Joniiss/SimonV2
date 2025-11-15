package com.app.simon.adapter
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources.getDrawable
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
        private val status: View = itemView.findViewById(R.id.status)


        fun bind(item: MonitorData) {
            tvmonitorName.text = item.nome
            tvmonitorPlace.text = "${item.local} - ${item.sala}"
            val prox = encontrarProximoHorario(item.horarioDisponivel)
            println("O próximo horário disponível é: ${prox!!.format()}")
            tvmonitorTime.text = prox!!.format()
            Glide.with(itemView)
                .load(item.foto)
                .into(ivmonitorPicture)

            status.background = when (item.status) {
                true -> getDrawable(itemView.context, R.drawable.status_dot_online)
                false -> getDrawable(itemView.context, R.drawable.bg_status_circle)
                else -> getDrawable(itemView.context, R.drawable.bg_status_circle)
            }

            btnAccess.setOnClickListener {
                val iProfile = Intent(itemView.context, ProfileActivity::class.java)
                iProfile.putExtra("monitor",item)
                itemView.context.startActivity(iProfile)
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

    }
}