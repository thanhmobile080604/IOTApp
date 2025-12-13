package com.example.iotapp.ui.chatbot

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.iotapp.R
import com.example.iotapp.databinding.ItemMessageBotBinding
import com.example.iotapp.databinding.ItemMessageUserBinding
import com.example.iotapp.model.ChatMessage

class ChatMessageAdapter(
    private val onFileClick: (ChatMessage) -> Unit
) : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).isUser) {
            TYPE_USER
        } else {
            TYPE_BOT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_USER -> {
                val binding = ItemMessageUserBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                UserMessageViewHolder(binding)
            }
            else -> {
                val binding = ItemMessageBotBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                BotMessageViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is UserMessageViewHolder -> holder.bind(message)
            is BotMessageViewHolder -> holder.bind(message)
        }
    }

    inner class UserMessageViewHolder(
        private val binding: ItemMessageUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            // Handle image message
            if (message.imageUri != null) {
                binding.ivImage.visibility = android.view.View.VISIBLE
                binding.tvMessage.visibility = android.view.View.GONE
                Glide.with(binding.root.context)
                    .load(message.imageUri)
                    .into(binding.ivImage)
            } else {
                binding.ivImage.visibility = android.view.View.GONE
                binding.tvMessage.visibility = android.view.View.VISIBLE
                
                // Handle file message
                if (message.fileUri != null && message.fileName != null) {
                    binding.tvMessage.text = message.fileName
                    binding.tvMessage.paintFlags = binding.tvMessage.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
                    binding.tvMessage.setTextColor(ContextCompat.getColor(binding.root.context, android.R.color.holo_blue_dark))
                    binding.root.setOnClickListener {
                        onFileClick(message)
                    }
                } else {
                    binding.tvMessage.text = message.text
                    binding.tvMessage.paintFlags = binding.tvMessage.paintFlags and android.graphics.Paint.UNDERLINE_TEXT_FLAG.inv()
                    binding.tvMessage.setTextColor(ContextCompat.getColor(binding.root.context, R.color.white))
                    binding.root.setOnClickListener(null)
                }
            }
        }
    }

    inner class BotMessageViewHolder(
        private val binding: ItemMessageBotBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            binding.tvMessage.text = message.text
        }
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_BOT = 2
    }
}

