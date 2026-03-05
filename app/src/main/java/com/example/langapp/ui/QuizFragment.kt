package com.example.langapp.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.langapp.LangApp
import com.example.langapp.R
import com.example.langapp.data.Word
import com.example.langapp.databinding.FragmentQuizBinding
import com.example.langapp.viewmodel.MainViewModel
import com.example.langapp.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

class QuizFragment : Fragment(R.layout.fragment_quiz) {
    private var _binding: FragmentQuizBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by viewModels {
        val app = requireActivity().application as LangApp
        ViewModelFactory(app.database.wordDao(), app.database.sessionDao())
    }

    private var words = listOf<Word>()
    private var currentIndex = 0
    private var score = 0
    private lateinit var direction: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentQuizBinding.bind(view)
        direction = arguments?.getString("direction") ?: "EN_FR"

        viewLifecycleOwner.lifecycleScope.launch {
            words = viewModel.getQuizWords()
            if (words.isEmpty()) {
                Toast.makeText(context, "Dictionnaire vide", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } else {
                showNextWord()
            }
        }

        binding.btnValidate.setOnClickListener {
            checkAnswer()
        }
    }

    private fun showNextWord() {
        if (currentIndex < words.size) {
            binding.etAnswer.text.clear()
            binding.tvFeedback.text = ""
            binding.btnValidate.text = getString(R.string.validate)

            val currentWord = words[currentIndex]
            binding.tvQuestion.text = if (direction == "EN_FR") currentWord.en else currentWord.fr

            binding.btnValidate.setOnClickListener { checkAnswer() }
        } else {
            endSession()
        }
    }

    private fun checkAnswer() {
        val currentWord = words[currentIndex]
        val userAnswer = binding.etAnswer.text.toString().trim()
        val correctAnswer = if (direction == "EN_FR") currentWord.fr else currentWord.en

        if (userAnswer.equals(correctAnswer, ignoreCase = true)) {
            score++
            binding.tvFeedback.text = getString(R.string.correct_answer)
        } else {
            binding.tvFeedback.text = getString(R.string.wrong_answer, correctAnswer)
        }

        currentIndex++
        binding.btnValidate.text = if (currentIndex < words.size) getString(R.string.next) else getString(R.string.finish)
        binding.btnValidate.setOnClickListener { showNextWord() }
    }

    private fun endSession() {
        viewModel.saveSession(score, words.size)
        Toast.makeText(context, "Session terminée. Score: $score/${words.size}", Toast.LENGTH_LONG).show()
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}