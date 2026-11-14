package dev.equalparts.glyph_catch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.equalparts.glyph_catch.components.PokemonSearch
import dev.equalparts.glyph_catch.data.PokedexData
import dev.equalparts.glyph_catch.data.PokemonDatabase
import dev.equalparts.glyph_catch.data.PokemonNames
import dev.equalparts.glyph_catch.data.PreferencesManager
import dev.equalparts.glyph_catch.debug.DebugExceptionTracker
import dev.equalparts.glyph_catch.gameplay.HiddenCatchUp
import dev.equalparts.glyph_catch.navigation.AppNavigationGraph
import dev.equalparts.glyph_catch.navigation.AppScreen
import dev.equalparts.glyph_catch.screens.garden.GardenSprites
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DebugExceptionTracker.install(applicationContext)
        val db = PokemonDatabase.getInstance(this)

        PokemonNames.init(this)
        PokemonSearch.init(this)
        PokedexData.init(this)

        val preferences = PreferencesManager(this)
        HiddenCatchUp.schedule(this, preferences.playWhileInUse)

        setContent {
            AppTheme {
                val preferencesManager = remember { PreferencesManager(this) }
                val colorSprites by preferencesManager.watchColorSprites()
                    .collectAsStateWithLifecycle(initialValue = preferencesManager.colorSprites)
                CompositionLocalProvider(LocalColorSprites provides colorSprites) {
                    MainScreen(db)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            HiddenCatchUp.run(applicationContext)
            withContext(Dispatchers.IO) {
                PreferencesManager(
                    applicationContext
                ).forgetMissingPokemon(PokemonDatabase.getInstance(applicationContext))
            }
            if (Features.TOWN) {
                GardenSprites.prefetch(applicationContext, PokemonDatabase.getInstance(applicationContext))
            }
        }
    }
}

@Composable
fun MainScreen(db: PokemonDatabase) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    var lastTap by remember { mutableStateOf("" to 0L) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            MainBottomBar(
                currentDestination = currentDestination,
                onNavigate = { targetRoute ->
                    val now = System.currentTimeMillis()
                    val doubleTap = lastTap.first == targetRoute && now - lastTap.second < DOUBLE_TAP_MILLIS
                    lastTap = targetRoute to now
                    val startId = navController.graph.findStartDestination().id
                    navController.navigate(targetRoute) {
                        if (doubleTap) {
                            // Double tap: back to the top of the section, forgetting where you were
                            popUpTo(startId) { inclusive = targetRoute == AppScreen.Home.route }
                        } else {
                            popUpTo(startId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        MainContent(
            db = db,
            navController = navController,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}

@Composable
private fun MainContent(db: PokemonDatabase, navController: NavHostController, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        AppNavigationGraph(
            navController = navController,
            db = db
        )
    }
}

@Composable
private fun MainBottomBar(currentDestination: NavDestination?, onNavigate: (String) -> Unit) {
    NavigationBar(
        containerColor = CatchColors.Black,
        contentColor = CatchColors.White,
        tonalElevation = AppSizes.none
    ) {
        AppScreen.bottomNavItems.forEach { screen ->
            val targetRoute = screen.bottomNavRoute()
            val selected = currentDestination.isRouteSelected(screen)

            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(id = screen.iconRes),
                        contentDescription = screen.route
                    )
                },
                label = {
                    Text(
                        text = screen.bottomNavLabel(),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                selected = selected,
                onClick = { onNavigate(targetRoute) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CatchColors.Red,
                    selectedTextColor = CatchColors.Red,
                    unselectedIconColor = CatchColors.White.copy(alpha = 0.6f),
                    unselectedTextColor = CatchColors.White.copy(alpha = 0.6f),
                    indicatorColor = CatchColors.Black
                )
            )
        }
    }
}

private const val DOUBLE_TAP_MILLIS = 500L

private fun NavDestination?.isRouteSelected(screen: AppScreen): Boolean {
    if (this == null) return false
    return hierarchy.any { destination ->
        val route = destination.route
        when (screen) {
            AppScreen.Home ->
                route == AppScreen.Home.route ||
                    route?.startsWith("home/detail") == true ||
                    route == AppScreen.Settings.route
            AppScreen.Caught -> route?.startsWith("caught") == true
            AppScreen.Inventory -> route?.startsWith("inventory") == true
            else -> route == screen.route
        }
    }
}

private fun AppScreen.bottomNavRoute(): String = when (this) {
    AppScreen.Caught -> AppScreen.Caught.createRoute()
    else -> route.substringBefore("?")
}

@Composable
private fun AppScreen.bottomNavLabel(): String = when (this) {
    AppScreen.Home -> stringResource(R.string.nav_home)
    AppScreen.Pokedex -> stringResource(R.string.nav_pokedex)
    AppScreen.Caught -> stringResource(R.string.nav_caught)
    AppScreen.Garden -> stringResource(R.string.garden_title)
    AppScreen.Inventory -> stringResource(R.string.nav_bag)
    AppScreen.Settings -> stringResource(R.string.nav_settings)
}
