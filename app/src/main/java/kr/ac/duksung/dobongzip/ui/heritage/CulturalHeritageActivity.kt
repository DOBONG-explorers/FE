package kr.ac.duksung.dobongzip.ui.heritage

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.ac.duksung.dobongzip.R
import kr.ac.duksung.dobongzip.data.network.HeritageItem
import kr.ac.duksung.dobongzip.data.network.RetrofitProvider

class CulturalHeritageActivity : AppCompatActivity() {

    private lateinit var adapter: HeritageAdapter
    private lateinit var rvHeritage: RecyclerView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.cultural_heritage_activity)

        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }

        rvHeritage = findViewById(R.id.rvHeritage)
        progressBar = findViewById(R.id.progressBar)
        rvHeritage.layoutManager = LinearLayoutManager(this)

        adapter = HeritageAdapter(emptyList()) { item ->
            val intent = Intent(this, IntroCulturalHeritageActivity::class.java)
            intent.putExtra("heritageId", item.id)
            android.util.Log.d("Heritage", "Clicked item id: ${item.id}, name: ${item.name}")
            startActivity(intent)
        }
        rvHeritage.adapter = adapter

        loadHeritageList()
    }

    private fun loadHeritageList() {
        progressBar.visibility = View.VISIBLE
        rvHeritage.visibility = View.GONE
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitProvider.heritageApi.getHeritageList()
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    rvHeritage.visibility = View.VISIBLE
                    
                    if (response.success && response.data != null) {
                        adapter.submitList(response.data)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    rvHeritage.visibility = View.VISIBLE
                }
                e.printStackTrace()
            }
        }
    }
}

