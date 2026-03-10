package com.example.langapp.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.langapp.LangApp
import com.example.langapp.R
import com.example.langapp.databinding.FragmentHomeBinding
import com.example.langapp.viewmodel.MainViewModel
import com.example.langapp.viewmodel.ViewModelFactory



class HomeFragment : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels {
        ViewModelFactory((requireActivity().application as LangApp).firebaseManager)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        binding.etPseudo.setText(viewModel.pseudo.value)

        binding.etPseudo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setPseudo(s.toString().trim())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnEnToFr.setOnClickListener { startQuiz("EN_FR") }
        binding.btnFrToEn.setOnClickListener { startQuiz("FR_EN") }
    }

    private fun startQuiz(direction: String) {
        if (viewModel.pseudo.value.isBlank()) {
            Toast.makeText(context, "Veuillez entrer un pseudo", Toast.LENGTH_SHORT).show()
            return
        }
        val action = HomeFragmentDirections.actionHomeToQuiz(direction)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}