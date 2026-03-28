package com.example.langapp.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.langapp.LangApp
import com.example.langapp.R
import com.example.langapp.data.Word
import com.example.langapp.data.WordList
import com.example.langapp.databinding.FragmentHomeBinding
import com.example.langapp.viewmodel.MainViewModel
import com.example.langapp.viewmodel.ViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// Héritage de Fragment avec le layout passé au constructeur pour alléger le code (remplace la surcharge de onCreateView)
class HomeFragment : Fragment(R.layout.fragment_home) {

    // Utilisation du pattern ViewBinding pour un accès sécurisé et typé aux vues du layout
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // Partage du ViewModel à l'échelle de l'activité pour conserver l'état entre les différents fragments de l'application
    private val viewModel: MainViewModel by activityViewModels {
        ViewModelFactory((requireActivity().application as LangApp).firebaseManager)
    }

    private var listIds = mutableListOf<String>()
    private var allListsData = listOf<WordList>()
    private var allWordsList = listOf<Word>()
    private var wordCount = 10
    private var maxWordsForSelectedList = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        // Observation du pseudo via le StateFlow du ViewModel
        // lifecycleScope est utilisé pour annuler automatiquement la coroutine à la destruction de la vue
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.pseudo.collect { pseudo ->
                binding.tvWelcome.text = "Bienvenue, $pseudo !"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // collectLatest annule l'exécution en cours si une nouvelle liste de mots est émise, optimisant les ressources
            viewModel.allWords.collectLatest { words ->
                allWordsList = words
                updateMaxWords()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allLists.collect { lists ->
                allListsData = lists
                val displayNames = mutableListOf("Toutes les listes")
                listIds.clear()
                listIds.add("all")

                displayNames.add("Favoris")
                listIds.add("favorites")

                lists.forEach {
                    displayNames.add("${it.name} (${it.difficulty} étoiles)")
                    listIds.add(it.id)
                }

                // Configuration d'un adaptateur natif pour lier les données textuelles au menu déroulant (Spinner)
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, displayNames)
                binding.spinnerLists.adapter = adapter
            }
        }

        binding.spinnerLists.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateMaxWords()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.btnMinus.setOnClickListener {
            if (wordCount > 1) {
                wordCount--
                binding.tvWordCount.text = wordCount.toString()
            }
        }

        binding.btnPlus.setOnClickListener {
            if (wordCount < maxWordsForSelectedList) {
                wordCount++
                binding.tvWordCount.text = wordCount.toString()
            } else {
                Toast.makeText(context, "Maximum atteint pour cette liste ($maxWordsForSelectedList mots)", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnStartQuiz.setOnClickListener {
            // Validation préventive empêchant le lancement d'un quiz sur une liste vide
            if (maxWordsForSelectedList == 0) {
                Toast.makeText(context, "Cette liste ne contient aucun mot", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val direction = if (binding.rbEnFr.isChecked) "EN_FR" else "FR_EN"
            val isQcmMode = binding.switchQcmMode.isChecked
            startQuiz(direction, isQcmMode)
        }

        binding.btnLogout.setOnClickListener {
            // Utilisation des SharedPreferences pour persister la déconnexion localement de manière synchrone
            val sharedPref = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            sharedPref.edit().remove("PSEUDO").apply()

            viewModel.setPseudo("")
            // Utilisation du composant Navigation d'Android Jetpack pour gérer les transitions de manière déclarative
            findNavController().navigate(R.id.loginFragment)
        }
    }

    // Méthode de synchronisation pour s'assurer que l'utilisateur ne demande pas plus de mots qu'il n'y en a
    private fun updateMaxWords() {
        val selectedIndex = binding.spinnerLists.selectedItemPosition
        val selectedListId = if (selectedIndex >= 0 && listIds.isNotEmpty()) listIds[selectedIndex] else "all"

        maxWordsForSelectedList = when (selectedListId) {
            "all" -> allWordsList.size
            "favorites" -> allWordsList.count { it.isFavorite }
            else -> allWordsList.count { it.listId == selectedListId }
        }

        if (maxWordsForSelectedList == 0) {
            wordCount = 0
        } else if (wordCount > maxWordsForSelectedList) {
            wordCount = maxWordsForSelectedList
        } else if (wordCount == 0 && maxWordsForSelectedList > 0) {
            wordCount = minOf(10, maxWordsForSelectedList)
        }

        binding.tvWordCount.text = wordCount.toString()
    }

    private fun startQuiz(direction: String, isQcmMode: Boolean) {
        val selectedIndex = binding.spinnerLists.selectedItemPosition
        val selectedListId = if (selectedIndex >= 0 && listIds.isNotEmpty()) listIds[selectedIndex] else "all"
        val selectedListName = when (selectedListId) {
            "all" -> "Toutes les listes"
            "favorites" -> "Favoris"
            else -> allListsData.find { it.id == selectedListId }?.name ?: "Toutes les listes"
        }

        // Transmission des paramètres du quiz via un Bundle à l'aide de l'API de Navigation
        val bundle = Bundle().apply {
            putString("direction", direction)
            putString("listId", selectedListId)
            putInt("wordCount", wordCount)
            putString("listName", selectedListName)
            putBoolean("isQcmMode", isQcmMode)
        }
        findNavController().navigate(R.id.quizFragment, bundle)
    }

    // Libération de la mémoire allouée au ViewBinding pour éviter les fuites de mémoire (Memory Leaks) lors de la destruction de la vue
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}