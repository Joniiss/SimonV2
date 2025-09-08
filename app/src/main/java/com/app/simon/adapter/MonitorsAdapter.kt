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
import com.app.simon.R
import com.app.simon.data.MonitorData
import de.hdodenhof.circleimageview.CircleImageView
import com.bumptech.glide.Glide

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
            tvmonitorTime.text = item.horarioDisponivel[0].day + " - " + item.horarioDisponivel[0].time[0] + ":00"

            Glide.with(itemView)
                .load(item.foto)
                .into(ivmonitorPicture)

            btnAccess.setOnClickListener{
                Toast.makeText(itemView.context, "Funcionalidade não implementada", Toast.LENGTH_SHORT).show()
                //val iCourse = Intent(itemView.context, MonitorsListActivity::class.java)
                //iCourse.putExtra("course",item.name)

                //itemView.context.startActivity(iCourse)
            }
        }
    }
}