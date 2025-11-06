package kr.ac.duksung.dobongzip.ui.recommend

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.launch
import kr.ac.duksung.dobongzip.R
import kr.ac.duksung.dobongzip.databinding.RecommendPlaceActivityBinding
import kr.ac.duksung.dobongzip.ui.recommend.UiState

class RecommendFragment : Fragment() {

    private var _binding: RecommendPlaceActivityBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecommendViewModel by viewModels()
    
    private var hasNavigated = false
    
    private val locationClient by lazy { LocationServices.getFusedLocationProviderClient(requireContext()) }
    
    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            getCurrentLocationAndRecommend()
        } else {
            viewModel.handleLocationError("위치 권한이 필요합니다.")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = RecommendPlaceActivityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeViewModel()
        hasNavigated = false
        viewModel.resetToInitial()
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Initial -> {
                    binding.initialView.isVisible = true
                    binding.resultView.isVisible = false
                }
                is UiState.Loading -> {
                    binding.initialView.isVisible = false
                    binding.resultView.isVisible = false

                }
                is UiState.Success -> {
                    binding.initialView.isVisible = false
                    binding.resultView.isVisible = true
                }

                is UiState.Error -> {
                    binding.initialView.isVisible = true
                    binding.resultView.isVisible = false
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.recommendedPlace.observe(viewLifecycleOwner) { place ->
            if (place != null && hasNavigated) {
                binding.tvPlaceName.text = place.name

                if (!place.imageUrl.isNullOrBlank() && place.imageUrl != "null") {
                    Glide.with(requireContext())
                        .load(place.imageUrl)
                        .placeholder(R.drawable.placeholder)
                        .error(R.drawable.placeholder)
                        .into(binding.ivPlaceImage)
                } else {
                    binding.ivPlaceImage.setImageResource(R.drawable.placeholder)
                }
                
                findNavController().navigate(R.id.action_recommend_to_commend)
            }
        }
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnRecommend.setOnClickListener {
            if (!hasNavigated) {
                hasNavigated = true
                requestLocationAndRecommend()
            }
        }

        binding.placeCard.setOnClickListener {
            val placeName = binding.tvPlaceName.text
            Toast.makeText(requireContext(), "$placeName 상세정보로 이동", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestLocationAndRecommend() {
        if (hasLocationPermission()) {
            getCurrentLocationAndRecommend()
        } else {
            requestLocationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
    
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }
    
    @SuppressLint("MissingPermission")
    private fun getCurrentLocationAndRecommend() {
        lifecycleScope.launch {
            try {
                if (!hasLocationPermission()) {
                    viewModel.handleLocationError("위치 권한이 필요합니다.")
                    return@launch
                }

                var lat = viewModel.getLastLatitude()
                var lng = viewModel.getLastLongitude()
                
                if (lat == null || lng == null) {
                    viewModel.setUiStateLoading()
                    
                    val tokenSource = CancellationTokenSource()
                    locationClient
                        .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.token)
                        .addOnSuccessListener { location ->
                            if (location != null) {
                                lifecycleScope.launch {
                                    viewModel.saveLocation(location.latitude, location.longitude)
                                    viewModel.onRecommendButtonClicked()
                                }
                            } else {
                                try {
                                    locationClient.lastLocation
                                        .addOnSuccessListener { lastLocation ->
                                            if (lastLocation != null) {
                                                lifecycleScope.launch {
                                                    viewModel.saveLocation(lastLocation.latitude, lastLocation.longitude)
                                                    viewModel.onRecommendButtonClicked()
                                                }
                                            } else {
                                                viewModel.handleLocationError("현재 위치를 가져올 수 없습니다.")
                                            }
                                        }
                                        .addOnFailureListener {
                                            viewModel.handleLocationError("위치 정보를 가져오는데 실패했습니다.")
                                        }
                                } catch (e: SecurityException) {
                                    viewModel.handleLocationError("위치 권한이 필요합니다.")
                                }
                            }
                        }
                        .addOnFailureListener {
                            try {
                                locationClient.lastLocation
                                    .addOnSuccessListener { lastLocation ->
                                        if (lastLocation != null) {
                                            lifecycleScope.launch {
                                                viewModel.saveLocation(lastLocation.latitude, lastLocation.longitude)
                                                viewModel.onRecommendButtonClicked()
                                            }
                                        } else {
                                            viewModel.handleLocationError("현재 위치를 가져올 수 없습니다.")
                                        }
                                    }
                                    .addOnFailureListener {
                                        viewModel.handleLocationError("위치 정보를 가져오는데 실패했습니다.")
                                    }
                            } catch (e: SecurityException) {
                                viewModel.handleLocationError("위치 권한이 필요합니다.")
                            }
                        }
                    return@launch
                }
                viewModel.onRecommendButtonClicked()
            } catch (e: Exception) {
                viewModel.handleLocationError("위치 정보를 가져오는데 실패했습니다: ${e.message}")
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}