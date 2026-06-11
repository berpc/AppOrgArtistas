package com.catedra.apporgartistas.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.catedra.apporgartistas.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class DashboardHostActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_host)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_dashboard) as NavHostFragment
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        bottomNavigation.setupWithNavController(navHostFragment.navController)
    }
}
