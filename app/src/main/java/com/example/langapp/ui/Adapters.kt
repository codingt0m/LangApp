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

// Utilisation de ListAdapter plutôt que RecyclerView.Adapter classique.
// ListAdapter intègre nativement AsyncListDiffer qui calcule les différences entre l'ancienne et la nouvelle liste
// sur un thread en arrière-plan (background thread), évitant ainsi de bloquer l'interface utilisateur.
class WordAdapter(
    // Injection de fonctions (lambdas) pour déléguer la gestion des événements de clic au Fragment/ViewModel.
    // Cela maintient l'adaptateur "stupide" (stateless) et respecte le principe de responsabilité unique (SRP).
    private val onDelete: (Word) -> Unit,
    private val onFavoriteClick: (Word) -> Unit
) : ListAdapter<Word, WordAdapter.WordViewHolder>(WordDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        // Instanciation du ViewHolder en utilisant le ViewBinding généré pour l'élément item_word.xml
        // LayoutInflater est obtenu depuis le contexte du parent pour respecter le thème de l'activité.
        return WordViewHolder(ItemWordBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // Le ViewHolder est responsable de la liaison des données (data binding) avec la vue.
    // L'utilisation d'une inner class permet d'accéder aux propriétés de la classe externe si nécessaire.
    inner class WordViewHolder(private val binding: ItemWordBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(word: Word) {
            binding.tvEn.text = word.en
            binding.tvFr.text = word.fr

            // Appel de la lambda injectée lors du clic, en passant l'objet Word courant.
            binding.btnDelete.setOnClickListener { onDelete(word) }

            // Logique conditionnelle pour mettre à jour l'icône de favori en utilisant les ressources natives Android.
            val starResource = if (word.isFavorite) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off
            binding.btnFavorite.setImageResource(starResource)

            binding.btnFavorite.setOnClickListener { onFavoriteClick(word) }
        }
    }

    // Implémentation de DiffUtil.ItemCallback pour indiquer à ListAdapter comment comparer les éléments.
    class WordDiffCallback : DiffUtil.ItemCallback<Word>() {
        // areItemsTheSame vérifie si deux objets représentent la même entité logique (via l'identifiant unique).
        override fun areItemsTheSame(oldItem: Word, newItem: Word) = oldItem.id == newItem.id

        // areContentsTheSame vérifie si les données de l'entité ont changé (grâce à l'implémentation equals() des data classes).
        override fun areContentsTheSame(oldItem: Word, newItem: Word) = oldItem == newItem
    }
}

// L'approche pour HistoryAdapter est identique à celle de WordAdapter,
// assurant une cohérence architecturale dans toute l'application.
class HistoryAdapter(
    private val onDeleteClick: (SessionHistory) -> Unit
) : ListAdapter<SessionHistory, HistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        return HistoryViewHolder(ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HistoryViewHolder(private val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(session: SessionHistory) {
            // Formatage de la date (stockée sous forme de timestamp Long) dans un format lisible par l'utilisateur.
            // Locale.getDefault() est utilisé pour respecter les paramètres régionaux de l'appareil.
            val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(session.date))
            binding.tvDate.text = dateStr

            // Calcul du pourcentage de réussite pour afficher une statistique claire.
            // La vérification session.total > 0 prévient une potentielle division par zéro.
            val percentage = if (session.total > 0) (session.score * 100) / session.total else 0
            binding.tvScore.text = "Score : ${session.score}/${session.total} ($percentage%)"
            binding.tvListName.text = "Liste : ${session.listName}"
            binding.tvDuration.text = "Durée : ${session.duration}s"

            binding.btnDeleteSession.setOnClickListener {
                onDeleteClick(session)
            }
        }
    }

    class HistoryDiffCallback : DiffUtil.ItemCallback<SessionHistory>() {
        override fun areItemsTheSame(oldItem: SessionHistory, newItem: SessionHistory) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: SessionHistory, newItem: SessionHistory) = oldItem == newItem
    }
}