package com.example.langapp.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.langapp.LangApp
import com.example.langapp.R
import com.example.langapp.databinding.FragmentLoginBinding
import com.example.langapp.viewmodel.MainViewModel
import com.example.langapp.viewmodel.ViewModelFactory

class LoginFragment : Fragment(R.layout.fragment_login) {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels {
        ViewModelFactory((requireActivity().application as LangApp).firebaseManager)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLoginBinding.bind(view)

        val sharedPref = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val savedPseudo = sharedPref.getString("PSEUDO", null)

        if (!savedPseudo.isNullOrBlank()) {
            viewModel.setPseudo(savedPseudo)
            findNavController().navigate(LoginFragmentDirections.actionLoginToHome())
            return
        }

        binding.btnContinue.setOnClickListener {
            val pseudo = binding.etPseudo.text.toString().trim()
            if (pseudo.isBlank()) {
                Toast.makeText(context, "Veuillez entrer un pseudo", Toast.LENGTH_SHORT).show()
            } else {
                sharedPref.edit().putString("PSEUDO", pseudo).apply()
                viewModel.setPseudo(pseudo)
                findNavController().navigate(LoginFragmentDirections.actionLoginToHome())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}