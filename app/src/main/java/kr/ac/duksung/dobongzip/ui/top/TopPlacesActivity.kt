package kr.ac.duksung.dobongzip.ui.top

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import kr.ac.duksung.dobongzip.R
import kr.ac.duksung.dobongzip.databinding.ActivityTopPlacesBinding

class TopPlacesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTopPlacesBinding
    private val adapter by lazy { TopPlacesAdapter() }
    private val viewModel: TopPlacesViewModel by viewModels {
        TopPlacesViewModelFactory(application)
    }
    private var lastErrorMessage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTopPlacesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        observeState()
        viewModel.refresh()
    }

    private fun setupViews() = with(binding) {
        backButton.setOnClickListener { finish() }
        recyclerViewPlaces.adapter = adapter
        imgSortToggle.setOnClickListener { viewModel.toggleSortOrder() }
        btnSort.setOnClickListener { viewModel.toggleSortOrder() }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    render(state)
                }
            }
        }
    }

    private fun render(state: TopPlacesUiState) = with(binding) {
        progressBar.isVisible = state.isLoading
        recyclerViewPlaces.isVisible = !state.isLoading && !state.isEmpty
        tvEmpty.isVisible = !state.isLoading && state.isEmpty
        adapter.submitList(state.sortedPlaces)

        val iconRes = if (state.isDescending) {
            R.drawable.ic_sort
        } else {
            R.drawable.ic_dropdown
        }
        imgSortToggle.setImageResource(iconRes)
        imgSortToggle.contentDescription = getString(R.string.top_place_sort_toggle)

        if (!state.errorMessage.isNullOrBlank() && state.errorMessage != lastErrorMessage) {
            Toast.makeText(this@TopPlacesActivity, state.errorMessage, Toast.LENGTH_SHORT).show()
            lastErrorMessage = state.errorMessage
        }
    }
}

