package kr.ac.duksung.dobongzip.ui.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.ac.duksung.dobongzip.data.local.SavedLocationStore
import kr.ac.duksung.dobongzip.data.network.RetrofitProvider
import kr.ac.duksung.dobongzip.databinding.ActivityTopPlacesBinding

class TopPlacesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTopPlacesBinding
    private val adapter by lazy { TopPlacesAdapter() }
    private val locationStore by lazy { SavedLocationStore(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTopPlacesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }
        binding.recyclerViewPlaces.adapter = adapter

        loadTopPlaces()
    }

    private fun loadTopPlaces() {
        lifecycleScope.launch {
            showLoading(true)
            val (lat, lng) = withContext(Dispatchers.IO) {
                val lat = locationStore.lastLatitude.firstOrNull()
                val lng = locationStore.lastLongitude.firstOrNull()
                (lat ?: DEFAULT_LAT) to (lng ?: DEFAULT_LNG)
            }

            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitProvider.placesApi.getTopPlaces(lat, lng)
                }

                val places = if (response.success) response.data.orEmpty() else emptyList()
                adapter.submitList(places)
                showEmpty(places.isEmpty())
            } catch (e: Exception) {
                adapter.submitList(emptyList())
                showEmpty(true)
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
        binding.recyclerViewPlaces.isVisible = !isLoading
        if (isLoading) {
            binding.tvEmpty.isVisible = false
        }
    }

    private fun showEmpty(isEmpty: Boolean) {
        binding.tvEmpty.isVisible = isEmpty
        binding.recyclerViewPlaces.isVisible = !isEmpty
    }

    companion object {
        private const val DEFAULT_LAT = 37.66877
        private const val DEFAULT_LNG = 127.04712
    }
}

