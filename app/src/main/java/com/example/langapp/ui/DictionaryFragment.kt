package com.example.langapp.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
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

    private val viewModel: MainViewModel by viewModels {
        val app = requireActivity().application as LangApp
        ViewModelFactory(app.database.wordDao(), app.database.sessionDao())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDictionnaryBinding.bind(view)

        val adapter = WordAdapter { word -> viewModel.deleteWord(word) }
        binding.rvWords.layoutManager = LinearLayoutManager(requireContext())
        binding.rvWords.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allWords.collectLatest { words ->
                adapter.submitList(words)
            }
        }

        binding.btnAdd.setOnClickListener {
            val en = binding.etEn.text.toString().trim()
            val fr = binding.etFr.text.toString().trim()
            if (en.isNotEmpty() && fr.isNotEmpty()) {
                viewModel.addWord(en, fr)
                binding.etEn.text?.clear()
                binding.etFr.text?.clear()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}