package com.example.langapp.ui

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.langapp.LangApp
import com.example.langapp.R
import com.example.langapp.data.Word
import com.example.langapp.data.WordList
import com.example.langapp.databinding.FragmentDictionnaryBinding
import com.example.langapp.viewmodel.MainViewModel
import com.example.langapp.viewmodel.ViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// Injection directe du layout dans le constructeur du Fragment pour éviter d'override onCreateView
class DictionaryFragment : Fragment(R.layout.fragment_dictionnary) {

    // Utilisation du ViewBinding pour la sécurité des types et éviter la nullité ou les findViewById coûteux
    private var _binding: FragmentDictionnaryBinding? = null
    private val binding get() = _binding!!

    // Partage de l'instance du ViewModel à l'échelle de l'activité (activityViewModels)
    // Cela permet aux différents fragments de partager le même état et les mêmes données
    private val viewModel: MainViewModel by activityViewModels {
        ViewModelFactory((requireActivity().application as LangApp).firebaseManager)
    }

    private var listIds = mutableListOf<String>()
    private var allListsData = listOf<WordList>()
    private var currentListId = "all"
    private var allWordsList = listOf<Word>()
    private lateinit var adapterWords: WordAdapter

    private var pendingSelectedListName: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDictionnaryBinding.bind(view)

        // Initialisation de l'adaptateur du RecyclerView avec des lambdas pour gérer les actions (suppression/favoris)
        // Cela maintient la logique dans le fragment/ViewModel plutôt que dans l'adaptateur
        adapterWords = WordAdapter(
            onDelete = { word -> viewModel.deleteWord(word) },
            onFavoriteClick = { word -> viewModel.toggleFavorite(word) }
        )
        binding.rvWords.layoutManager = LinearLayoutManager(requireContext())
        binding.rvWords.adapter = adapterWords

        // Lancement des coroutines liées au cycle de vie de la vue
        viewLifecycleOwner.lifecycleScope.launch {
            // repeatOnLifecycle garantit que la collecte des flux (Flow) s'arrête
            // lorsque le fragment n'est plus visible (ex: mis en arrière-plan) pour préserver les ressources
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    // collectLatest permet d'annuler le traitement de la valeur précédente
                    // si une nouvelle valeur est émise rapidement, optimisant ainsi les performances
                    viewModel.allWords.collectLatest { words ->
                        allWordsList = words
                        updateRecyclerView()
                    }
                }
                launch {
                    viewModel.allLists.collectLatest { lists ->
                        if (_binding == null) return@collectLatest

                        allListsData = lists
                        listIds.clear()
                        val displayNames = mutableListOf<String>()

                        displayNames.add("Toutes les listes")
                        listIds.add("all")

                        // Logique de sélection automatique d'une liste venant d'être créée
                        if (pendingSelectedListName != null) {
                            val newList = lists.find { it.name == pendingSelectedListName }
                            if (newList != null) {
                                currentListId = newList.id
                                pendingSelectedListName = null
                            }
                        }

                        lists.forEach {
                            if (it.id == "favorites") {
                                displayNames.add(it.name)
                            } else {
                                displayNames.add("${it.name} (${it.difficulty}★)")
                            }
                            listIds.add(it.id)
                        }

                        // Utilisation d'un adaptateur natif simple pour le Spinner (menu déroulant)
                        val adapterSpinner = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, displayNames)
                        binding.spinnerTargetList.adapter = adapterSpinner

                        val index = listIds.indexOf(currentListId)
                        if (index >= 0) {
                            binding.spinnerTargetList.setSelection(index)
                        } else {
                            binding.spinnerTargetList.setSelection(0)
                        }
                    }
                }
            }
        }

        binding.spinnerTargetList.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentListId = listIds[position]
                updateRecyclerView()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.btnAddList.setOnClickListener {
            showCreateListDialog()
        }

        binding.btnEditList.setOnClickListener {
            showEditListDialog()
        }

        binding.btnAddWord.setOnClickListener {
            val en = binding.etEn.text.toString().trim()
            val fr = binding.etFr.text.toString().trim()

            // Vérification de validation côté client avant l'appel au ViewModel
            if (currentListId == "all" || currentListId == "favorites") {
                Toast.makeText(context, "Veuillez sélectionner une liste spécifique", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (en.isNotEmpty() && fr.isNotEmpty()) {
                viewModel.addWord(currentListId, en, fr)
                binding.etEn.text?.clear()
                binding.etFr.text?.clear()
            } else {
                Toast.makeText(context, "Champs requis", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Filtrage effectué localement pour éviter de faire des requêtes réseau inutiles à chaque changement de filtre
    private fun updateRecyclerView() {
        if (_binding == null) return

        binding.btnEditList.visibility = if (currentListId == "all" || currentListId == "favorites") View.GONE else View.VISIBLE

        val filteredList = when (currentListId) {
            "all" -> allWordsList
            "favorites" -> allWordsList.filter { it.isFavorite }
            else -> allWordsList.filter { it.listId == currentListId }
        }
        // submitList (issu de ListAdapter) gère de manière asynchrone le calcul des différences (DiffUtil)
        // et anime proprement les changements dans le RecyclerView
        adapterWords.submitList(filteredList)
    }

    // Construction dynamique d'une modale AlertDialog pour la création de liste
    // Évite la création d'un Fragment entier pour une simple saisie de données
    private fun showCreateListDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_list, null)
        val etListName = dialogView.findViewById<EditText>(R.id.etListName)
        val ratingDifficulty = dialogView.findViewById<RatingBar>(R.id.ratingDifficulty)

        AlertDialog.Builder(requireContext())
            .setTitle("Créer une liste")
            .setView(dialogView)
            .setPositiveButton("Créer") { _, _ ->
                val name = etListName.text.toString().trim()
                val difficulty = ratingDifficulty.rating.toInt()
                if (name.isNotEmpty()) {
                    pendingSelectedListName = name
                    viewModel.addWordList(name, difficulty)
                    Toast.makeText(context, "Liste créée", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Nom requis", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun showEditListDialog() {
        val listToEdit = allListsData.find { it.id == currentListId } ?: return

        val dialogView = layoutInflater.inflate(R.layout.dialog_create_list, null)
        val etListName = dialogView.findViewById<EditText>(R.id.etListName)
        val ratingDifficulty = dialogView.findViewById<RatingBar>(R.id.ratingDifficulty)

        etListName.setText(listToEdit.name)
        ratingDifficulty.rating = listToEdit.difficulty.toFloat()

        AlertDialog.Builder(requireContext())
            .setTitle("Modifier la liste")
            .setView(dialogView)
            .setPositiveButton("Enregistrer") { _, _ ->
                val newName = etListName.text.toString().trim()
                val newDifficulty = ratingDifficulty.rating.toInt()
                if (newName.isNotEmpty()) {
                    viewModel.updateWordList(currentListId, newName, newDifficulty)
                    Toast.makeText(context, "Liste modifiée", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Nom requis", Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton("Supprimer") { _, _ ->
                confirmDeleteList(listToEdit)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun confirmDeleteList(list: WordList) {
        AlertDialog.Builder(requireContext())
            .setTitle("Supprimer la liste ?")
            .setMessage("Voulez-vous vraiment supprimer la liste '${list.name}' ainsi que tous les mots qu'elle contient ? Cette action est irréversible.")
            .setPositiveButton("Oui, supprimer") { _, _ ->
                viewModel.deleteWordList(list.id)
                binding.spinnerTargetList.setSelection(0)
                Toast.makeText(context, "Liste supprimée", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    // Libération impérative du binding lors de la destruction de la vue
    // Évite les fuites de mémoire (Memory Leaks) courantes avec les fragments
    override fun onDestroyView() {
        super.onDestroyView()
        _binding?.spinnerTargetList?.onItemSelectedListener = null
        _binding = null
    }
}