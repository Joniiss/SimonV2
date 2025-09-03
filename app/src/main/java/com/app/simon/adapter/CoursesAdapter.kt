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
import com.app.simon.data.SubjectData

class CoursesAdapter(private val mData: MutableList<SubjectData>) : RecyclerView.Adapter<CoursesAdapter.CoursesViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoursesViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_course, parent, false)
        return CoursesViewHolder(view)
    }

    override fun onBindViewHolder(holder: CoursesViewHolder, position: Int) {
        val item = mData[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    class CoursesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.tvCourseName)
        private val btnAccess: TextView = itemView.findViewById(R.id.btnAccess)

        fun bind(item: SubjectData) {
            textView.text = item.name

            btnAccess.setOnClickListener{
                val iCourse = Intent(itemView.context, MonitorsListActivity::class.java)
                iCourse.putExtra("course",item.name)

                itemView.context.startActivity(iCourse)
            }
        }
    }
}