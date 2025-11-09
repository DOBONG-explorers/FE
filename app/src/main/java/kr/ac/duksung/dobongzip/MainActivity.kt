package kr.ac.duksung.dobongzip


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import kr.ac.duksung.dobongzip.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var originalNavElevation: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        navView.setupWithNavController(navController)

        intent.getIntExtra(EXTRA_TARGET_DESTINATION, -1).takeIf { it != -1 }?.let { targetItemId ->
            if (navView.selectedItemId != targetItemId) {
                navView.selectedItemId = targetItemId
            }
        }

        navView.setOnItemReselectedListener {}
        
        originalNavElevation = navView.elevation
    }

    fun enableMapFragmentLayout() {
        val containerView = binding.navHostFragment
        val navView = binding.navView

        val params = containerView.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        params?.let {
            it.bottomToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            it.bottomToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
            containerView.layoutParams = it
            containerView.requestLayout()
        }

        navView.elevation = 16f
        navView.bringToFront()
    }

    fun restoreNormalLayout() {
        val containerView = binding.navHostFragment
        val navView = binding.navView
        
        val params = containerView.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        params?.let {
            it.bottomToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
            it.bottomToTop = R.id.nav_view
            containerView.layoutParams = it
            containerView.requestLayout()
        }
        
        navView.elevation = originalNavElevation
    }

    fun getBottomNavHeight(): Int = binding.navView.height

    companion object {
        const val EXTRA_TARGET_DESTINATION = "extra_target_destination"
    }
}

