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
import com.app.simon.data.ForumCommentsData
import com.app.simon.data.ForumData
import com.app.simon.data.ForumPostData
import com.app.simon.data.SubjectData
import com.app.simon.data.User

class ForumCommentsAdapter(private val mData: MutableList<ForumCommentsData>, private val user: User) : RecyclerView.Adapter<ForumCommentsAdapter.ForumCommentsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForumCommentsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return ForumCommentsViewHolder(view, user)
    }

    override fun onBindViewHolder(holder: ForumCommentsViewHolder, position: Int) {
        val item = mData[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    class ForumCommentsViewHolder(itemView: View, private val user: User) : RecyclerView.ViewHolder(itemView) {
        private val tvUser: TextView = itemView.findViewById(R.id.tvUser)
        private val tvComment: TextView = itemView.findViewById(R.id.tvComment)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)

        fun bind(item: ForumCommentsData) {
            tvUser.text = item.userName
            tvComment.text = item.content
            tvTime.text = ""

        }
    }
}