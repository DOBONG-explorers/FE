package kr.ac.duksung.dobongzip


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import kr.ac.duksung.dobongzip.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

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
    }

    companion object {
        const val EXTRA_TARGET_DESTINATION = "extra_target_destination"
    }
}

