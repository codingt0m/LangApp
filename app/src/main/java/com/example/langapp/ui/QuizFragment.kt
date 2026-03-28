// Emplacement : codingt0m/langapp/LangApp-ffae75213ec4e325161e96d7412b21eb86381be5/app/src/main/java/com/example/langapp/ui/QuizFragment.kt

package com.example.langapp.ui

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
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
import java.text.Normalizer
import java.util.concurrent.TimeUnit
import kotlin.math.abs

// Héritage de Fragment en passant le layout en paramètre pour simplifier l'instanciation
class QuizFragment : Fragment(R.layout.fragment_quiz) {

    // Utilisation de ViewBinding pour lier les éléments de l'interface graphique de manière sûre et typée
    private var _binding: FragmentQuizBinding? = null
    private val binding get() = _binding!!

    // Partage de l'instance du ViewModel avec l'activité hôte pour conserver l'état et communiquer avec la base de données
    private val viewModel: MainViewModel by activityViewModels {
        ViewModelFactory((requireActivity().application as LangApp).firebaseManager)
    }

    private var words = listOf<Word>()
    private var originalWords = listOf<Word>()
    private val wrongWords = mutableListOf<Word>()
    private var currentIndex = 0
    private var score = 0
    private var currentStreak = 0
    private lateinit var direction: String
    private var hasUsedAlmostCorrect = false
    private var isReviewSession = false

    private var isQcmMode = false
    private var selectedQcmAnswer = ""

    // Initialisation paresseuse (lazy) de la liste des boutons pour optimiser l'allocation mémoire au démarrage
    private val choiceButtons by lazy {
        listOf(binding.btnChoice1, binding.btnChoice2, binding.btnChoice3, binding.btnChoice4)
    }

    private var countDownTimer: CountDownTimer? = null
    // Utilisation d'une constante pour définir le temps limite par question, facilitant les futures modifications
    private val TIME_LIMIT_MS = 15000L
    private var timeRemainingMs = TIME_LIMIT_MS
    private var totalSessionDurationMs = 0L

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentQuizBinding.bind(view)

        // Récupération sécurisée des arguments passés via le composant de Navigation
        direction = arguments?.getString("direction") ?: "EN_FR"
        val listId = arguments?.getString("listId") ?: "all"
        val wordCount = arguments?.getInt("wordCount") ?: 10
        isQcmMode = arguments?.getBoolean("isQcmMode") ?: false

        binding.btnValidate.isEnabled = false
        binding.tvStreak.text = "Série : $currentStreak"

        // Exécution asynchrone pour la récupération des mots afin de ne pas bloquer le thread principal (UI Thread)
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
            countDownTimer?.cancel() // Annulation explicite du timer pour éviter des fuites ou des exécutions inattendues
            Toast.makeText(context, "Session abandonnée", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }

    // Gestion de la logique d'affichage adaptative selon le mode de jeu (Classique vs QCM)
    private fun showNextWord() {
        if (words.isEmpty()) return

        countDownTimer?.cancel()
        timeRemainingMs = TIME_LIMIT_MS
        binding.tvTimer.text = "${TIME_LIMIT_MS / 1000}s"
        binding.tvStreak.text = "Série : $currentStreak"

        if (currentIndex < words.size) {
            hasUsedAlmostCorrect = false
            binding.etAnswer.text?.clear()
            binding.tvFeedback.text = ""
            binding.btnValidate.text = getString(R.string.validate)

            val currentWord = words[currentIndex]
            binding.tvQuestion.text = if (direction == "EN_FR") currentWord.en else currentWord.fr

            binding.progressBar.max = words.size
            binding.progressBar.progress = currentIndex

            // Implémentation conditionnelle du layout selon le mode QCM
            if (isQcmMode) {
                binding.textInputLayout.visibility = View.GONE
                binding.qcmLayout.visibility = View.VISIBLE
                binding.btnValidate.visibility = View.GONE
                selectedQcmAnswer = ""

                val correctAnswer = if (direction == "EN_FR") currentWord.fr else currentWord.en

                // Génération des mauvaises réponses en piochant aléatoirement dans le ViewModel global
                val pool = viewModel.allWords.value
                    .filter { if (direction == "EN_FR") it.fr != correctAnswer else it.en != correctAnswer }
                    .map { if (direction == "EN_FR") it.fr else it.en }
                    .distinct()
                    .shuffled()

                val wrongAnswers = pool.take(3).toMutableList()

                // Mécanisme de secours (fallback) si la base de données ne contient pas assez de mots pour le QCM
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
                    button.isEnabled = true
                    button.setOnClickListener {
                        selectedQcmAnswer = options[index]
                        // Feedback visuel basique sur la sélection du bouton
                        choiceButtons.forEach { b ->
                            b.alpha = 0.5f
                            b.isEnabled = false
                        }
                        button.alpha = 1.0f
                        checkAnswer()
                    }
                }
            } else {
                binding.textInputLayout.visibility = View.VISIBLE
                binding.qcmLayout.visibility = View.GONE
                binding.btnValidate.visibility = View.VISIBLE
            }

            binding.btnValidate.setOnClickListener { checkAnswer() }
            startTimer()
        } else {
            binding.progressBar.progress = words.size
            endSession()
        }
    }

    // Utilisation de la classe native CountDownTimer pour la gestion du temps de réponse
    private fun startTimer() {
        countDownTimer = object : CountDownTimer(TIME_LIMIT_MS, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemainingMs = millisUntilFinished
                binding.tvTimer.text = "${millisUntilFinished / 1000}s"
            }

            override fun onFinish() {
                timeRemainingMs = 0
                binding.tvTimer.text = "0s"
                handleTimeUp()
            }
        }.start()
    }

    private fun handleTimeUp() {
        if (words.isEmpty() || currentIndex >= words.size) return

        totalSessionDurationMs += TIME_LIMIT_MS
        currentStreak = 0
        binding.tvStreak.text = "Série : $currentStreak"

        val currentWord = words[currentIndex]

        if (!wrongWords.contains(currentWord)) {
            wrongWords.add(currentWord)
        }

        val correctAnswer = if (direction == "EN_FR") currentWord.fr else currentWord.en
        binding.tvFeedback.text = getString(R.string.wrong_answer, correctAnswer) + " (Temps écoulé)"
        binding.tvFeedback.setTextColor(Color.parseColor("#F44336"))

        if (isQcmMode) {
            choiceButtons.forEach { it.isEnabled = false }
        }

        currentIndex++
        binding.btnValidate.visibility = View.VISIBLE
        binding.btnValidate.text = if (currentIndex < words.size) getString(R.string.next) else getString(R.string.finish)
        binding.btnValidate.setOnClickListener { showNextWord() }
    }

    // Fonction d'extension Kotlin pour normaliser les chaînes et ignorer les accents lors de la validation
    private fun String.removeAccents(): String {
        val normalized = Normalizer.normalize(this, Normalizer.Form.NFD)
        return "\\p{InCombiningDiacriticalMarks}+".toRegex().replace(normalized, "")
    }

    private fun checkAnswer() {
        if (words.isEmpty() || currentIndex >= words.size) return

        countDownTimer?.cancel()
        totalSessionDurationMs += (TIME_LIMIT_MS - timeRemainingMs)

        val currentWord = words[currentIndex]
        val userAnswerRaw = if (isQcmMode) selectedQcmAnswer else binding.etAnswer.text.toString().trim()

        if (isQcmMode && userAnswerRaw.isEmpty()) {
            startTimer()
            Toast.makeText(context, getString(R.string.select_answer), Toast.LENGTH_SHORT).show()
            return
        }

        val correctAnswerRaw = if (direction == "EN_FR") currentWord.fr else currentWord.en

        val userAnswer = userAnswerRaw.removeAccents()
        val correctAnswer = correctAnswerRaw.removeAccents()

        if (userAnswer.equals(correctAnswer, ignoreCase = true)) {
            score++
            currentStreak++
            binding.tvFeedback.text = getString(R.string.correct_answer)
            binding.tvFeedback.setTextColor(Color.parseColor("#4CAF50"))

            // Intégration d'une librairie tierce (Konfetti) pour améliorer l'UX (Gamification)
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
            binding.btnValidate.visibility = View.VISIBLE
            binding.btnValidate.text = if (currentIndex < words.size) getString(R.string.next) else getString(R.string.finish)
            binding.btnValidate.setOnClickListener { showNextWord() }

            // Tolérance aux fautes de frappe mineures : répond à l'exigence des "essais multiples"
        } else if (!isQcmMode && !hasUsedAlmostCorrect && isAlmostCorrect(userAnswer, correctAnswer)) {
            hasUsedAlmostCorrect = true
            currentStreak = 0
            binding.tvFeedback.text = getString(R.string.almost_correct)
            binding.tvFeedback.setTextColor(Color.parseColor("#FF9800"))
            startTimer()
        } else {
            if (!wrongWords.contains(currentWord)) {
                wrongWords.add(currentWord)
            }
            currentStreak = 0
            binding.tvFeedback.text = getString(R.string.wrong_answer, correctAnswerRaw)
            binding.tvFeedback.setTextColor(Color.parseColor("#F44336"))

            currentIndex++
            binding.btnValidate.visibility = View.VISIBLE
            binding.btnValidate.text = if (currentIndex < words.size) getString(R.string.next) else getString(R.string.finish)
            binding.btnValidate.setOnClickListener { showNextWord() }
        }

        binding.tvStreak.text = "Série : $currentStreak"
    }

    // Algorithme de calcul de distance (type Levenshtein simplifié) pour déterminer si la réponse est "presque" correcte
    private fun isAlmostCorrect(userAnswer: String, correctAnswer: String): Boolean {
        val a = userAnswer.lowercase()
        val b = correctAnswer.lowercase()

        if (a == b) return false

        // Tolérance : une lettre différente ou une inversion de deux lettres adjacentes
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

        // Tolérance : un caractère manquant ou en trop
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
        val durationInSeconds = (totalSessionDurationMs / 1000).toInt()

        // Sauvegarde de l'historique uniquement si ce n'est pas une session de révision (pour ne pas fausser les stats globales)
        if (!isReviewSession) {
            val listName = arguments?.getString("listName") ?: "Toutes les listes"
            viewModel.saveSession(listName, score, words.size, durationInSeconds)
        }

        // Flux de navigation additionnel proposant de revoir spécifiquement les erreurs commises
        if (wrongWords.isNotEmpty()) {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.session_finished))
                .setMessage(getString(R.string.review_mistakes_message, score, words.size, wrongWords.size))
                .setPositiveButton(getString(R.string.yes)) { _, _ ->
                    startReviewSession()
                }
                .setNegativeButton(getString(R.string.no)) { _, _ ->
                    Toast.makeText(context, "Session terminée. Score final : $score/${words.size} en ${durationInSeconds}s", Toast.LENGTH_LONG).show()
                    findNavController().popBackStack()
                }
                .setCancelable(false)
                .show()
        } else {
            Toast.makeText(context, "Session terminée. Sans faute ! Score : $score/${words.size} en ${durationInSeconds}s", Toast.LENGTH_LONG).show()
            findNavController().popBackStack()
        }
    }

    // Réinitialisation des variables d'état pour la session de révision, sans recréer un nouveau fragment
    private fun startReviewSession() {
        words = wrongWords.toList()
        wrongWords.clear()
        currentIndex = 0
        score = 0
        currentStreak = 0
        totalSessionDurationMs = 0L
        isReviewSession = true
        showNextWord()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel() // Nettoyage final du timer pour prévenir les Memory Leaks
        _binding = null
    }
}