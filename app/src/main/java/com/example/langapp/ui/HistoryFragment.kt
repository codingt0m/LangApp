package com.example.langapp.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.langapp.LangApp
import com.example.langapp.R
import com.example.langapp.databinding.FragmentHistoryBinding
import com.example.langapp.viewmodel.MainViewModel
import com.example.langapp.viewmodel.ViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HistoryFragment : Fragment(R.layout.fragment_history) {
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels {
        ViewModelFactory((requireActivity().application as LangApp).firebaseManager)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHistoryBinding.bind(view)

        val adapter = HistoryAdapter()
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allSessions.collectLatest { sessions ->
                    if (_binding == null) return@collectLatest

                    if (sessions.isEmpty()) {
                        binding.rvHistory.visibility = View.GONE
                        binding.tvEmptyHistory.visibility = View.VISIBLE
                    } else {
                        binding.rvHistory.visibility = View.VISIBLE
                        binding.tvEmptyHistory.visibility = View.GONE
                        adapter.submitList(sessions)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}