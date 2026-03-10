package com.example.langapp.ui

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.langapp.LangApp
import com.example.langapp.R
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDictionnaryBinding.bind(view)

        val adapterWords = WordAdapter { word -> viewModel.deleteWord(word) }
        binding.rvWords.layoutManager = LinearLayoutManager(requireContext())
        binding.rvWords.adapter = adapterWords

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allWords.collectLatest { words ->
                adapterWords.submitList(words)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allLists.collectLatest { lists ->
                listIds.clear()
                val displayNames = mutableListOf<String>()

                lists.forEach {
                    displayNames.add(it.name)
                    listIds.add(it.id)
                }

                val adapterSpinner = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, displayNames)
                binding.spinnerTargetList.adapter = adapterSpinner
            }
        }

        binding.btnCreateList.setOnClickListener {
            val name = binding.etListName.text.toString().trim()
            val difficulty = binding.ratingDifficulty.rating.toInt()

            if (name.isNotEmpty()) {
                viewModel.addWordList(name, difficulty)
                binding.etListName.text?.clear()
                binding.ratingDifficulty.rating = 1f
                Toast.makeText(context, "Liste créée", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Nom de liste requis", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnAddWord.setOnClickListener {
            val en = binding.etEn.text.toString().trim()
            val fr = binding.etFr.text.toString().trim()
            val selectedIndex = binding.spinnerTargetList.selectedItemPosition

            if (listIds.isEmpty()) {
                Toast.makeText(context, "Veuillez d'abord créer une liste", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedListId = listIds[selectedIndex]

            if (en.isNotEmpty() && fr.isNotEmpty()) {
                viewModel.addWord(selectedListId, en, fr)
                binding.etEn.text?.clear()
                binding.etFr.text?.clear()
            } else {
                Toast.makeText(context, "Champs requis", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}