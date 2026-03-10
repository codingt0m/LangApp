package com.example.langapp.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.langapp.LangApp
import com.example.langapp.R
import com.example.langapp.data.Word
import com.example.langapp.data.WordList
import com.example.langapp.databinding.FragmentHomeBinding
import com.example.langapp.viewmodel.MainViewModel
import com.example.langapp.viewmodel.ViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels {
        ViewModelFactory((requireActivity().application as LangApp).firebaseManager)
    }

    private var listIds = mutableListOf<String>()
    private var allListsData = listOf<WordList>()
    private var allWordsList = listOf<Word>()
    private var wordCount = 10
    private var maxWordsForSelectedList = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.pseudo.collect { pseudo ->
                binding.tvWelcome.text = "Bienvenue, $pseudo !"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allWords.collectLatest { words ->
                allWordsList = words
                updateMaxWords()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allLists.collect { lists ->
                allListsData = lists
                val displayNames = mutableListOf("Toutes les listes")
                listIds.clear()
                listIds.add("all")

                lists.forEach {
                    displayNames.add("${it.name} (${it.difficulty} étoiles)")
                    listIds.add(it.id)
                }

                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, displayNames)
                binding.spinnerLists.adapter = adapter
            }
        }

        binding.spinnerLists.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateMaxWords()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.btnMinus.setOnClickListener {
            if (wordCount > 1) {
                wordCount--
                binding.tvWordCount.text = wordCount.toString()
            }
        }

        binding.btnPlus.setOnClickListener {
            if (wordCount < maxWordsForSelectedList) {
                wordCount++
                binding.tvWordCount.text = wordCount.toString()
            } else {
                Toast.makeText(context, "Maximum atteint pour cette liste ($maxWordsForSelectedList mots)", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnStartQuiz.setOnClickListener {
            if (maxWordsForSelectedList == 0) {
                Toast.makeText(context, "Cette liste ne contient aucun mot", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val direction = if (binding.rbEnFr.isChecked) "EN_FR" else "FR_EN"
            startQuiz(direction)
        }

        binding.btnLogout.setOnClickListener {
            val sharedPref = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            sharedPref.edit().remove("PSEUDO").apply()

            viewModel.setPseudo("")
            findNavController().navigate(R.id.loginFragment)
        }
    }

    private fun updateMaxWords() {
        val selectedIndex = binding.spinnerLists.selectedItemPosition
        val selectedListId = if (selectedIndex >= 0 && listIds.isNotEmpty()) listIds[selectedIndex] else "all"

        maxWordsForSelectedList = if (selectedListId == "all") {
            allWordsList.size
        } else {
            allWordsList.count { it.listId == selectedListId }
        }

        if (maxWordsForSelectedList == 0) {
            wordCount = 0
        } else if (wordCount > maxWordsForSelectedList) {
            wordCount = maxWordsForSelectedList
        } else if (wordCount == 0 && maxWordsForSelectedList > 0) {
            wordCount = minOf(10, maxWordsForSelectedList)
        }

        binding.tvWordCount.text = wordCount.toString()
    }

    private fun startQuiz(direction: String) {
        val selectedIndex = binding.spinnerLists.selectedItemPosition
        val selectedListId = if (selectedIndex >= 0 && listIds.isNotEmpty()) listIds[selectedIndex] else "all"
        val selectedListName = if (selectedListId == "all") "Toutes les listes" else allListsData.find { it.id == selectedListId }?.name ?: "Toutes les listes"

        val bundle = Bundle().apply {
            putString("direction", direction)
            putString("listId", selectedListId)
            putInt("wordCount", wordCount)
            putString("listName", selectedListName)
        }
        findNavController().navigate(R.id.quizFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}