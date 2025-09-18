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
import com.app.simon.R
import com.app.simon.data.ForumPostData
import com.app.simon.data.SubjectData
import com.app.simon.data.User

class ForumPostsAdapter(private val mData: MutableList<ForumPostData>, private val user: User) : RecyclerView.Adapter<ForumPostsAdapter.ForumPostsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForumPostsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_forum, parent, false)
        return ForumPostsViewHolder(view, user)
    }

    override fun onBindViewHolder(holder: ForumPostsViewHolder, position: Int) {
        val item = mData[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    class ForumPostsViewHolder(itemView: View, private val user: User) : RecyclerView.ViewHolder(itemView) {
        private val tvAuthor: TextView = itemView.findViewById(R.id.tvAuthor)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvContent: TextView = itemView.findViewById(R.id.tvContent)
        private val tvLikes: TextView = itemView.findViewById(R.id.tvLikes)
        private val tvComments: TextView = itemView.findViewById(R.id.tvComments)
        private val postAccess: LinearLayout = itemView.findViewById(R.id.llPost)

        fun bind(item: ForumPostData) {
            tvAuthor.text = item.userName
            tvTitle.text = item.title
            tvContent.text = item.content
            tvLikes.text = item.likes.toString()
            tvComments.text = item.comments.size.toString()

            postAccess.setOnClickListener{
                val iCourse = Intent(itemView.context, ForumPostActivity::class.java)
                iCourse.putExtra("post",item)
                iCourse.putExtra("user",user)
                itemView.context.startActivity(iCourse)
            }
        }
    }
}