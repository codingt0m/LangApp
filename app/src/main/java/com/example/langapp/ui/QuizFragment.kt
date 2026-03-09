package com.example.langapp.ui

import android.graphics.Color
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
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

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

        binding.btnAbandon.setOnClickListener {
            Toast.makeText(context, "Session abandonnée", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }

    private fun showNextWord() {
        if (currentIndex < words.size) {
            binding.etAnswer.text?.clear()
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
            binding.tvFeedback.setTextColor(Color.parseColor("#4CAF50"))

            val party = Party(
                speed = 0f,
                maxSpeed = 30f,
                damping = 0.9f,
                spread = 360,
                colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def),
                emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100),
                position = Position.Relative(0.5, 0.3)
            )
            binding.konfettiView.start(party)
        } else {
            binding.tvFeedback.text = getString(R.string.wrong_answer, correctAnswer)
            binding.tvFeedback.setTextColor(Color.parseColor("#F44336"))
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