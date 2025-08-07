package com.example.projectpa

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.projectpa.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        // ✅ Pasang MaterialToolbar sebagai ActionBar
        setSupportActionBar(binding.topAppBar)

        // ✅ Bottom nav navigation
        binding.navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    navController.popBackStack(R.id.navigation_home, false)
                    true
                }
                R.id.navigation_help -> {
                    navController.navigate(R.id.navigation_help)
                    true
                }
                else -> false
            }
        }

        // ✅ Konfigurasi AppBar dan Navigation
        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.navigation_home, R.id.navigation_help)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        // ✅ Ubah title toolbar & warna icon back setiap destinasi berubah
        navController.addOnDestinationChangedListener { _, destination, _ ->
            supportActionBar?.title = destination.label

            // Ubah warna icon panah back
            binding.topAppBar.navigationIcon?.let { icon ->
                icon.setColorFilter(Color.parseColor("#74B4AE"), PorterDuff.Mode.SRC_IN)
                binding.topAppBar.navigationIcon = icon
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
