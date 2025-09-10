package com.app.simon.adapter
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.app.simon.MonitorsListActivity
import com.app.simon.R
import com.app.simon.data.MonitorData
import com.app.simon.data.SubjectData
import com.app.simon.data.User

class MonitorCoursesAdapter(private val mData: MutableList<MonitorData>, private val user: User) : RecyclerView.Adapter<MonitorCoursesAdapter.MonitorCoursesViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonitorCoursesViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_course, parent, false)
        return MonitorCoursesViewHolder(view, user)
    }

    override fun onBindViewHolder(holder: MonitorCoursesViewHolder, position: Int) {
        val item = mData[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    class MonitorCoursesViewHolder(itemView: View, private val user: User) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.tvCourseName)
        private val btnAccess: TextView = itemView.findViewById(R.id.btnAccess)

        fun bind(item: MonitorData) {
            textView.text = item.disciplina

            btnAccess.setOnClickListener{
                val iCourse = Intent(itemView.context, MonitorsListActivity::class.java)
                iCourse.putExtra("courseId",item.disciplinaId)
                iCourse.putExtra("user", user)

                itemView.context.startActivity(iCourse)
            }
        }
    }
}