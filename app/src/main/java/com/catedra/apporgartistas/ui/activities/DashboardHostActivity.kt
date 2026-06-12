package com.catedra.apporgartistas.ui.activities

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.catedra.apporgartistas.R
import com.catedra.apporgartistas.activities.LoginActivity
import com.catedra.apporgartistas.data.models.Agrupacion
import com.catedra.apporgartistas.viewmodels.DashboardHostViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlin.math.roundToInt

class DashboardHostActivity : AppCompatActivity() {

    private val viewModel: DashboardHostViewModel by viewModels()
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var directorItemsContainer: LinearLayout
    private lateinit var drawerProgress: ProgressBar

    private val directorBullets = listOf(
        "\uD83D\uDFE2",
        "\uD83D\uDFE3",
        "\uD83D\uDD35",
        "\uD83D\uDFE1"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_host)

        drawerLayout = findViewById(R.id.drawer_layout)
        directorItemsContainer = findViewById(R.id.drawer_director_items)
        drawerProgress = findViewById(R.id.drawer_progress)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_dashboard) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val toolbar = findViewById<MaterialToolbar>(R.id.dashboard_toolbar)

        setSupportActionBar(toolbar)
        configurarToolbar(toolbar)
        configurarTitulos(toolbar, navController)
        configurarDrawer(navController)
        configurarBackDelDrawer()
        observarViewModel()

        bottomNavigation.setupWithNavController(navController)
        viewModel.cargarAgrupacionesDirector()
    }

    private fun configurarToolbar(toolbar: MaterialToolbar) {
        toolbar.setNavigationContentDescription(R.string.drawer_open)
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    private fun configurarTitulos(toolbar: MaterialToolbar, navController: NavController) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            toolbar.title = destination.label?.toString() ?: getString(R.string.app_name)
        }
    }

    private fun configurarDrawer(navController: NavController) {
        findViewById<TextView>(R.id.drawer_item_shows).setOnClickListener {
            navegarEnDashboard(navController, R.id.nav_show_setlist)
        }

        findViewById<TextView>(R.id.drawer_item_setlists).setOnClickListener {
            navegarEnDashboard(navController, R.id.item_1)
        }

        findViewById<TextView>(R.id.drawer_item_logout).setOnClickListener {
            viewModel.cerrarSesion()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    private fun navegarEnDashboard(navController: NavController, destinationId: Int) {
        if (navController.currentDestination?.id != destinationId) {
            val navOptions = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(navController.graph.startDestinationId, false)
                .build()

            navController.navigate(destinationId, null, navOptions)
        }

        drawerLayout.closeDrawer(GravityCompat.START)
    }

    private fun configurarBackDelDrawer() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
    }

    private fun observarViewModel() {
        viewModel.agrupaciones.observe(this) { agrupaciones ->
            mostrarAgrupacionesEnDrawer(agrupaciones)
        }

        viewModel.isLoading.observe(this) { isLoading ->
            drawerProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { mensaje ->
            Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarAgrupacionesEnDrawer(agrupaciones: List<Agrupacion>) {
        directorItemsContainer.removeAllViews()

        if (agrupaciones.isEmpty()) {
            directorItemsContainer.addView(crearTextoSinAgrupaciones())
            return
        }

        agrupaciones
            .sortedBy { it.nombre.lowercase() }
            .forEachIndexed { index, agrupacion ->
                directorItemsContainer.addView(crearItemAgrupacion(agrupacion, index))
            }
    }

    private fun crearTextoSinAgrupaciones(): TextView {
        return TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(16.dp(), 8.dp(), 16.dp(), 8.dp())
            text = getString(R.string.drawer_no_agrupaciones)
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            textSize = 14f
        }
    }

    private fun crearItemAgrupacion(agrupacion: Agrupacion, index: Int): View {
        return LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                48.dp()
            )
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            setPadding(16.dp(), 0, 16.dp(), 0)
            isClickable = true
            isFocusable = true
            setBackgroundResource(selectableItemBackground())
            setOnClickListener { abrirAgrupacion(agrupacion) }

            addView(TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(32.dp(), ViewGroup.LayoutParams.WRAP_CONTENT)
                text = directorBullets[index % directorBullets.size]
                textSize = 18f
                gravity = Gravity.CENTER
            })

            addView(TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                )
                text = agrupacion.nombre
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                textSize = 16f
            })
        }
    }

    private fun abrirAgrupacion(agrupacion: Agrupacion) {
        drawerLayout.closeDrawer(GravityCompat.START)

        startActivity(Intent(this, ShowsDashboardActivity::class.java).apply {
            putExtra("AGRUPACION_ID", agrupacion.id)
            putExtra("AGRUPACION_NOMBRE", agrupacion.nombre)
        })
    }

    private fun selectableItemBackground(): Int {
        val outValue = TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        return outValue.resourceId
    }

    private fun Int.dp(): Int {
        return (this * resources.displayMetrics.density).roundToInt()
    }
}
