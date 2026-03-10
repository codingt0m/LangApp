package com.example.langapp.ui

import android.content.Context
import android.os.Bundle
import android.view.View
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.pseudo.collect { pseudo ->
                binding.tvWelcome.text = "Bienvenue, $pseudo !"
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
        val action = HomeFragmentDirections.actionHomeToQuiz(direction)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}