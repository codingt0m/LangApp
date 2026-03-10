package com.example.langapp.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.langapp.LangApp
import com.example.langapp.R
import com.example.langapp.databinding.FragmentHomeBinding
import com.example.langapp.viewmodel.MainViewModel
import com.example.langapp.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels {
        ViewModelFactory((requireActivity().application as LangApp).firebaseManager)
    }

    private var listIds = mutableListOf<String>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.pseudo.collect { pseudo ->
                binding.tvWelcome.text = "Bienvenue, $pseudo !"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allLists.collect { lists ->
                val displayNames = mutableListOf("Tout")
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

        binding.btnEnToFr.setOnClickListener { startQuiz("EN_FR") }
        binding.btnFrToEn.setOnClickListener { startQuiz("FR_EN") }

        binding.btnLogout.setOnClickListener {
            val sharedPref = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            sharedPref.edit().remove("PSEUDO").apply()

            viewModel.setPseudo("")
            findNavController().navigate(HomeFragmentDirections.actionHomeToLogin())
        }
    }

    private fun startQuiz(direction: String) {
        val selectedIndex = binding.spinnerLists.selectedItemPosition
        val selectedListId = if (selectedIndex >= 0) listIds[selectedIndex] else "all"

        val action = HomeFragmentDirections.actionHomeToQuiz(direction, selectedListId)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}