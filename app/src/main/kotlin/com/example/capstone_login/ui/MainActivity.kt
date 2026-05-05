package com.example.capstone_login.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.capstone_login.R
import com.example.capstone_login.databinding.ActivityMainBinding

/**
 * Single Activity — contains the NavHostFragment defined in activity_main.xml.
 * Navigation graph (nav_graph.xml) manages all Fragment transitions.
 *
 * Phase 3 adds: auto-login routing via FirebaseAuth.authStateChanges() Flow
 * before the nav graph's default start destination (loginFragment) is shown.
 *
 * ViewBinding used for type-safe layout access.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Phase 3: check auth state and route accordingly
        // val navController = findNavController(R.id.nav_host_fragment)
        // if (FirebaseAuth.getInstance().currentUser != null) {
        //     navController.navigate(
        //         R.id.calendarFragment,
        //         null,
        //         NavOptions.Builder().setPopUpTo(R.id.loginFragment, true).build()
        //     )
        // }
    }
}
