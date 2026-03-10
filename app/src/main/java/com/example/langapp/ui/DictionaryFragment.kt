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
import androidx.lifecycle.lifecycleScope
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

class DictionaryFragment : Fragment(R.layout.fragment_dictionnary) {
    private var _binding: FragmentDictionnaryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels {
        ViewModelFactory((requireActivity().application as LangApp).firebaseManager)
    }

    private var listIds = mutableListOf<String>()
    private var allListsData = listOf<WordList>()
    private var currentListId = "all"
    private var allWordsList = listOf<Word>()
    private lateinit var adapterWords: WordAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDictionnaryBinding.bind(view)

        adapterWords = WordAdapter { word -> viewModel.deleteWord(word) }
        binding.rvWords.layoutManager = LinearLayoutManager(requireContext())
        binding.rvWords.adapter = adapterWords

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allWords.collectLatest { words ->
                allWordsList = words
                updateRecyclerView()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allLists.collectLatest { lists ->
                allListsData = lists
                listIds.clear()
                val displayNames = mutableListOf<String>()

                displayNames.add("Toutes les listes")
                listIds.add("all")

                lists.forEach {
                    displayNames.add("${it.name} (${it.difficulty}★)")
                    listIds.add(it.id)
                }

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

        binding.spinnerTargetList.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentListId = listIds[position]
                binding.btnEditList.visibility = if (currentListId == "all") View.GONE else View.VISIBLE
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

            if (currentListId == "all") {
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

    private fun updateRecyclerView() {
        val filteredList = if (currentListId == "all") {
            allWordsList
        } else {
            allWordsList.filter { it.listId == currentListId }
        }
        adapterWords.submitList(filteredList)
    }

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}