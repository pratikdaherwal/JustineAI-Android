package com.patikprojects.justineai.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.patikprojects.justineai.models.ChatMessagesModel
import com.patikprojects.justineai.R

class ChatAdapter(internal val messages: MutableList<ChatMessagesModel>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_AI = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) VIEW_TYPE_USER else VIEW_TYPE_AI
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_USER -> UserMessageViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_user, parent, false)
            )
            else -> AIMessageViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_ai, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is UserMessageViewHolder -> holder.bind(messages[position])
            is AIMessageViewHolder -> holder.bind(messages[position])
        }
    }

    override fun getItemCount() = messages.size

    fun addMessage(message: ChatMessagesModel) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    fun updateLastMessage(message: String) {
        if (messages.isNotEmpty()) {
            val lastIndex = messages.size - 1
            val currentMessage = messages[lastIndex]
            if (currentMessage.text != message) {
                messages[lastIndex] = currentMessage.copy(text = message)
                notifyItemChanged(lastIndex)
            }
        }
    }

    inner class UserMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(message: ChatMessagesModel) {
            itemView.findViewById<TextView>(R.id.messageText).text = message.text
        }
    }

    inner class AIMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(message: ChatMessagesModel) {
            itemView.findViewById<TextView>(R.id.messageText).text = message.text
        }
    }
}