package com.example.langapp.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.langapp.data.SessionHistory
import com.example.langapp.data.Word
import com.example.langapp.databinding.ItemHistoryBinding
import com.example.langapp.databinding.ItemWordBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WordAdapter(private val onDelete: (Word) -> Unit) : ListAdapter<Word, WordAdapter.WordViewHolder>(WordDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        return WordViewHolder(ItemWordBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class WordViewHolder(private val binding: ItemWordBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(word: Word) {
            binding.tvEn.text = word.en
            binding.tvFr.text = word.fr
            binding.btnDelete.setOnClickListener { onDelete(word) }
        }
    }

    class WordDiffCallback : DiffUtil.ItemCallback<Word>() {
        override fun areItemsTheSame(oldItem: Word, newItem: Word) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Word, newItem: Word) = oldItem == newItem
    }
}

class HistoryAdapter : ListAdapter<SessionHistory, HistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        return HistoryViewHolder(ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HistoryViewHolder(private val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(session: SessionHistory) {
            val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(session.date))
            binding.tvSessionDate.text = dateStr

            val percentage = if (session.total > 0) (session.score * 100) / session.total else 0
            binding.tvSessionScore.text = "Score : ${session.score}/${session.total} ($percentage%)"

            binding.tvListName.text = "Liste : ${session.listName}"
        }
    }

    class HistoryDiffCallback : DiffUtil.ItemCallback<SessionHistory>() {
        override fun areItemsTheSame(oldItem: SessionHistory, newItem: SessionHistory) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: SessionHistory, newItem: SessionHistory) = oldItem == newItem
    }
}