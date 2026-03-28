package com.example.langapp.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.langapp.LangApp
import com.example.langapp.R
import com.example.langapp.data.SessionHistory
import com.example.langapp.databinding.FragmentHistoryBinding
import com.example.langapp.viewmodel.MainViewModel
import com.example.langapp.viewmodel.ViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// Injection du layout directement via le constructeur du Fragment
class HistoryFragment : Fragment(R.layout.fragment_history) {

    // Implémentation du ViewBinding pour s'affranchir des appels à findViewById et garantir la sécurité des types
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    // Récupération de l'instance partagée du ViewModel rattachée au cycle de vie de l'activité
    private val viewModel: MainViewModel by activityViewModels {
        ViewModelFactory((requireActivity().application as LangApp).firebaseManager)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHistoryBinding.bind(view)

        // Instanciation de l'adaptateur en passant une expression lambda pour gérer les interactions (clic de suppression)
        // Cela permet de découpler la logique métier de la logique d'affichage
        val adapter = HistoryAdapter { session ->
            confirmDeleteSession(session)
        }

        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = adapter

        // Démarrage des coroutines dans le scope de la vue pour une gestion automatique de l'annulation
        viewLifecycleOwner.lifecycleScope.launch {
            // repeatOnLifecycle suspend l'exécution quand le fragment n'est plus au premier plan (ex: onStop)
            // C'est la méthode recommandée pour observer des flux de données liés à l'interface utilisateur
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // collectLatest réagit aux nouveaux états émis par le ViewModel concernant l'historique
                viewModel.allSessions.collectLatest { sessions ->
                    // Sécurité additionnelle pour s'assurer que le binding n'est pas nul avant de modifier l'UI
                    // Évite un crash si le flow émet pendant la destruction de la vue
                    if (_binding == null) return@collectLatest

                    // Bascule de visibilité entre le RecyclerView et le message de liste vide
                    if (sessions.isEmpty()) {
                        binding.rvHistory.visibility = View.GONE
                        binding.tvEmptyHistory.visibility = View.VISIBLE
                    } else {
                        binding.rvHistory.visibility = View.VISIBLE
                        binding.tvEmptyHistory.visibility = View.GONE
                        adapter.submitList(sessions) // Délégation du rendu au DiffUtil de l'adaptateur
                    }
                }
            }
        }
    }

    // Utilisation d'un AlertDialog natif pour prévenir les suppressions accidentelles
    private fun confirmDeleteSession(session: SessionHistory) {
        AlertDialog.Builder(requireContext())
            .setTitle("Supprimer la session ?")
            .setMessage("Voulez-vous vraiment supprimer cette session de votre historique ?")
            .setPositiveButton("Oui") { _, _ ->
                viewModel.deleteSession(session)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    // Nettoyage obligatoire de la référence au binding pour prévenir les fuites de mémoire
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}