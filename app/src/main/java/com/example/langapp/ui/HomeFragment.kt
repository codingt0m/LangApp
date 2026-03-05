package com.example.langapp.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.langapp.R
import com.example.langapp.databinding.FragmentHomeBinding

class HomeFragment : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        binding.btnEnToFr.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeToQuiz("EN_FR")
            findNavController().navigate(action)
        }
        binding.btnFrToEn.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeToQuiz("FR_EN")
            findNavController().navigate(action)
        }
        binding.btnDictionary.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_dict)
        }
        binding.btnHistory.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_history)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}