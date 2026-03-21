package com.example.langapp.ui

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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
import kotlin.math.abs

class QuizFragment : Fragment(R.layout.fragment_quiz) {
    private var _binding: FragmentQuizBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels {
        ViewModelFactory((requireActivity().application as LangApp).firebaseManager)
    }

    private var words = listOf<Word>()
    private var originalWords = listOf<Word>()
    private val wrongWords = mutableListOf<Word>()
    private var currentIndex = 0
    private var score = 0
    private lateinit var direction: String
    private var hasUsedAlmostCorrect = false
    private var isReviewSession = false

    private var isQcmMode = false
    private var selectedQcmAnswer = ""
    private val choiceButtons by lazy {
        listOf(binding.btnChoice1, binding.btnChoice2, binding.btnChoice3, binding.btnChoice4)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentQuizBinding.bind(view)

        direction = arguments?.getString("direction") ?: "EN_FR"
        val listId = arguments?.getString("listId") ?: "all"
        val wordCount = arguments?.getInt("wordCount") ?: 10
        isQcmMode = arguments?.getBoolean("isQcmMode") ?: false

        binding.btnValidate.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            words = viewModel.getQuizWords(listId, wordCount)
            originalWords = words.toList()
            if (words.isEmpty()) {
                Toast.makeText(context, "Cette liste est vide", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } else {
                binding.btnValidate.isEnabled = true
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
        if (words.isEmpty()) return

        if (currentIndex < words.size) {
            hasUsedAlmostCorrect = false
            binding.etAnswer.text?.clear()
            binding.tvFeedback.text = ""
            binding.btnValidate.text = getString(R.string.validate)

            val currentWord = words[currentIndex]
            binding.tvQuestion.text = if (direction == "EN_FR") currentWord.en else currentWord.fr

            binding.progressBar.max = words.size
            binding.progressBar.progress = currentIndex

            if (isQcmMode) {
                binding.textInputLayout.visibility = View.GONE
                binding.qcmLayout.visibility = View.VISIBLE
                selectedQcmAnswer = ""

                val correctAnswer = if (direction == "EN_FR") currentWord.fr else currentWord.en

                val pool = originalWords
                    .filter { if (direction == "EN_FR") it.fr != correctAnswer else it.en != correctAnswer }
                    .map { if (direction == "EN_FR") it.fr else it.en }
                    .distinct()
                    .shuffled()

                val wrongAnswers = pool.take(3).toMutableList()

                var fallbackIndex = 1
                while (wrongAnswers.size < 3) {
                    val fallbackWord = if (direction == "EN_FR") "Option $fallbackIndex" else "Choice $fallbackIndex"
                    if (!wrongAnswers.contains(fallbackWord) && fallbackWord != correctAnswer) {
                        wrongAnswers.add(fallbackWord)
                    }
                    fallbackIndex++
                }

                val options = (wrongAnswers + correctAnswer).shuffled()

                choiceButtons.forEachIndexed { index, button ->
                    button.visibility = View.VISIBLE
                    button.text = options[index]
                    button.alpha = 1.0f
                    button.setOnClickListener {
                        selectedQcmAnswer = options[index]
                        choiceButtons.forEach { b -> b.alpha = 0.5f }
                        button.alpha = 1.0f
                    }
                }
            } else {
                binding.textInputLayout.visibility = View.VISIBLE
                binding.qcmLayout.visibility = View.GONE
            }

            binding.btnValidate.setOnClickListener { checkAnswer() }
        } else {
            binding.progressBar.progress = words.size
            endSession()
        }
    }

    private fun checkAnswer() {
        if (words.isEmpty() || currentIndex >= words.size) return

        val currentWord = words[currentIndex]
        val userAnswer = if (isQcmMode) selectedQcmAnswer else binding.etAnswer.text.toString().trim()

        if (isQcmMode && userAnswer.isEmpty()) {
            Toast.makeText(context, getString(R.string.select_answer), Toast.LENGTH_SHORT).show()
            return
        }

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

            currentIndex++
            binding.btnValidate.text = if (currentIndex < words.size) getString(R.string.next) else getString(R.string.finish)
            binding.btnValidate.setOnClickListener { showNextWord() }

        } else if (!isQcmMode && !hasUsedAlmostCorrect && isAlmostCorrect(userAnswer, correctAnswer)) {
            hasUsedAlmostCorrect = true
            binding.tvFeedback.text = getString(R.string.almost_correct)
            binding.tvFeedback.setTextColor(Color.parseColor("#FF9800"))
        } else {
            if (!wrongWords.contains(currentWord)) {
                wrongWords.add(currentWord)
            }
            binding.tvFeedback.text = getString(R.string.wrong_answer, correctAnswer)
            binding.tvFeedback.setTextColor(Color.parseColor("#F44336"))

            currentIndex++
            binding.btnValidate.text = if (currentIndex < words.size) getString(R.string.next) else getString(R.string.finish)
            binding.btnValidate.setOnClickListener { showNextWord() }
        }
    }

    private fun isAlmostCorrect(userAnswer: String, correctAnswer: String): Boolean {
        val a = userAnswer.lowercase()
        val b = correctAnswer.lowercase()

        if (a == b) return false

        if (a.length == b.length) {
            val diffs = mutableListOf<Int>()
            for (i in a.indices) {
                if (a[i] != b[i]) diffs.add(i)
            }
            if (diffs.size == 1) return true
            if (diffs.size == 2) {
                val i = diffs[0]
                val j = diffs[1]
                if (i + 1 == j && a[i] == b[j] && a[j] == b[i]) {
                    return true
                }
            }
        }

        if (abs(a.length - b.length) == 1) {
            val shorter = if (a.length < b.length) a else b
            val longer = if (a.length > b.length) a else b

            var i = 0
            var j = 0
            var diff = 0
            while (i < shorter.length && j < longer.length) {
                if (shorter[i] != longer[j]) {
                    diff++
                    if (diff > 1) return false
                    j++
                } else {
                    i++
                    j++
                }
            }
            return true
        }

        return false
    }

    private fun endSession() {
        if (!isReviewSession) {
            val listName = arguments?.getString("listName") ?: "Toutes les listes"
            viewModel.saveSession(listName, score, words.size)
        }

        if (wrongWords.isNotEmpty()) {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.session_finished))
                .setMessage(getString(R.string.review_mistakes_message, score, words.size, wrongWords.size))
                .setPositiveButton(getString(R.string.yes)) { _, _ ->
                    startReviewSession()
                }
                .setNegativeButton(getString(R.string.no)) { _, _ ->
                    Toast.makeText(context, "Session terminée. Score final : $score/${words.size}", Toast.LENGTH_LONG).show()
                    findNavController().popBackStack()
                }
                .setCancelable(false)
                .show()
        } else {
            Toast.makeText(context, "Session terminée. Sans faute ! Score : $score/${words.size}", Toast.LENGTH_LONG).show()
            findNavController().popBackStack()
        }
    }

    private fun startReviewSession() {
        words = wrongWords.toList()
        wrongWords.clear()
        currentIndex = 0
        score = 0
        isReviewSession = true
        showNextWord()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}