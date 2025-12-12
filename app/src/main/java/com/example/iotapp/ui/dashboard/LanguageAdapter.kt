package com.example.iotapp.ui.dashboard

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.iotapp.R
import com.example.iotapp.databinding.ItemLanguageBinding
import com.example.iotapp.model.LanguageOption

class LanguageAdapter(
    private val onItemClick: (LanguageOption) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    private val items = mutableListOf<LanguageOption>()
    private var selectedCode: String? = null

    fun submit(list: List<LanguageOption>, selected: String) {
        items.clear()
        items.addAll(list)
        selectedCode = selected
        notifyDataSetChanged()
    }

    fun getSelectedCode(): String? = selectedCode

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val binding = ItemLanguageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LanguageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class LanguageViewHolder(private val binding: ItemLanguageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: LanguageOption) {
            val context = binding.root.context
            val isSelected = item.code == selectedCode
            binding.tvLanguageName.text = item.label
            binding.icLanguage.setImageResource(item.iconRes)

            val bgColor =  if (isSelected) R.drawable.bg_white_12 else R.drawable.bg_346348_12
            val textColor = ContextCompat.getColor(context, if (isSelected) R.color.black else R.color.white)
            binding.clRoot.setBackgroundResource(bgColor)
            binding.tvLanguageName.setTextColor(textColor)

            binding.root.setOnClickListener {
                Log.d("LanguageAdapter", "Selected language=${item.code}")
                selectedCode = item.code
                notifyDataSetChanged()
                onItemClick(item)
            }
        }
    }
}

