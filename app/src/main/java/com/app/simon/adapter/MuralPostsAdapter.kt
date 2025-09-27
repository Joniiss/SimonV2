package com.app.simon.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.simon.ForumPostActivity
import com.app.simon.MonitorsListActivity
import com.app.simon.MuralPostActivity
import com.app.simon.R
import com.app.simon.data.ForumData
import com.app.simon.data.ForumPostData
import com.app.simon.data.MuralPostData
import com.app.simon.data.SubjectData
import com.app.simon.data.User

class MuralPostsAdapter(private val mData: MutableList<MuralPostData>, private val user: User) : RecyclerView.Adapter<MuralPostsAdapter.MuralPostsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MuralPostsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_mural, parent, false)
        return MuralPostsViewHolder(view, user)
    }

    override fun onBindViewHolder(holder: MuralPostsViewHolder, position: Int) {
        val item = mData[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    class MuralPostsViewHolder(itemView: View, private val user: User) : RecyclerView.ViewHolder(itemView) {
        private val tvAuthor: TextView = itemView.findViewById(R.id.tvAutor)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitulo)
        private val tvContent: TextView = itemView.findViewById(R.id.tvDescricao)
        private val postAccess: LinearLayout = itemView.findViewById(R.id.llPost)

        fun bind(item: MuralPostData) {
            tvAuthor.text = item.userName
            tvTitle.text = item.title
            tvContent.text = item.content

            postAccess.setOnClickListener{
                val iCourse = Intent(itemView.context, MuralPostActivity::class.java)
                iCourse.putExtra("post",item)
                iCourse.putExtra("user",user)
                itemView.context.startActivity(iCourse)
            }
        }
    }
}