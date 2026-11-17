package com.macrow.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.macrow.app.data.MacrowRepository
import com.macrow.app.data.Settings
import com.macrow.app.domain.Meal
import com.macrow.app.ui.components.FloatingNav
import com.macrow.app.ui.components.LocalHaptics
import com.macrow.app.ui.components.TabRoutes
import com.macrow.app.ui.screens.AddFoodScreen
import com.macrow.app.ui.screens.DiaryScreen
import com.macrow.app.ui.screens.FoodEditScreen
import com.macrow.app.ui.screens.NutrientsScreen
import com.macrow.app.ui.screens.OnboardingScreen
import com.macrow.app.ui.screens.PlanScreen
import com.macrow.app.ui.screens.ProgressScreen
import com.macrow.app.ui.screens.RecipeEditScreen
import com.macrow.app.ui.screens.ScanScreen
import com.macrow.app.ui.screens.SettingsScreen
import com.macrow.app.ui.theme.macrow

val LocalSnack = staticCompositionLocalOf<SnackbarHostState> { error("No SnackbarHostState provided") }

private const val NEW_FOOD = "newFood"

@Composable
fun MacrowRoot(settings: Settings) {
    val nav = rememberNavController()
    val snack = remember { SnackbarHostState() }
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    val showNav = route in TabRoutes
    // Decided once: a first launch goes to setup, everything after goes to the diary.
    val start = rememberSaveable { if (settings.onboarded) "diary" else "onboarding/0" }
    LaunchedEffect(Unit) { UiErrors.events.collect { snack.showSnackbar(it) } }

    CompositionLocalProvider(LocalHaptics provides settings.haptics, LocalSnack provides snack) {
        // Phones fill the screen; tablets and landscape get a centred column so nothing stretches.
        Box(Modifier.fillMaxSize().background(MaterialTheme.macrow.bg), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.widthIn(max = 640.dp).fillMaxSize()) {
                NavHost(
                    nav, start, Modifier.fillMaxSize(),
                    enterTransition = { fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 24 } },
                    exitTransition = { fadeOut(tween(140)) },
                    popEnterTransition = { fadeIn(tween(220)) },
                    popExitTransition = { fadeOut(tween(160)) + slideOutVertically(tween(220)) { it / 24 } },
                ) {
                    composable("diary") {
                        DiaryScreen(onAddFood = { meal, day -> nav.navigate("add/0/${meal.name}/$day") }, onSettings = { nav.navigate("settings") })
                    }
                    composable("plan") {
                        PlanScreen(onAddFood = { meal, day -> nav.navigate("add/1/${meal.name}/$day") })
                    }
                    composable("progress") { ProgressScreen(onSettings = { nav.navigate("settings") }) }
                    composable("nutrients") { NutrientsScreen() }
                    composable("settings") {
                        SettingsScreen(onBack = { nav.popBackStack() }, onEditProfile = { nav.navigate("onboarding/1") })
                    }
                    composable("onboarding/{edit}", listOf(navArgument("edit") { type = NavType.IntType })) {
                        val editing = it.arguments!!.getInt("edit") == 1
                        OnboardingScreen(
                            editing, settings.profile, settings.targets,
                            onBack = { nav.popBackStack() },
                            onDone = { if (editing) nav.popBackStack() else nav.navigate("diary") { popUpTo("onboarding/0") { inclusive = true } } },
                        )
                    }
                    composable(
                        "add/{planned}/{meal}/{day}",
                        listOf(navArgument("planned") { type = NavType.IntType }, navArgument("meal") { type = NavType.StringType }, navArgument("day") { type = NavType.LongType }),
                    ) { entry ->
                        val a = entry.arguments!!
                        val handle = entry.savedStateHandle
                        val newId by handle.getStateFlow(NEW_FOOD, 0L).collectAsStateWithLifecycle()
                        val planned = a.getInt("planned") == 1
                        val meal = Meal.of(a.getString("meal")!!)
                        val day = a.getLong("day")
                        AddFoodScreen(
                            planned, meal, day,
                            newFoodId = newId, onNewFoodHandled = { handle[NEW_FOOD] = 0L },
                            onBack = { nav.popBackStack() },
                            onScan = { nav.navigate("scan/${a.getInt("planned")}/${meal.name}/$day") },
                            onCreateFood = { nav.navigate("food/0?barcode=") },
                            onNewRecipe = { nav.navigate("recipe/0") },
                            onEdit = { id, isRecipe -> nav.navigate(if (isRecipe) "recipe/$id" else "food/$id?barcode=") },
                        )
                    }
                    composable(
                        "scan/{planned}/{meal}/{day}",
                        listOf(navArgument("planned") { type = NavType.IntType }, navArgument("meal") { type = NavType.StringType }, navArgument("day") { type = NavType.LongType }),
                    ) { entry ->
                        val a = entry.arguments!!
                        val handle = entry.savedStateHandle
                        val newId by handle.getStateFlow(NEW_FOOD, 0L).collectAsStateWithLifecycle()
                        ScanScreen(
                            a.getInt("planned") == 1, Meal.of(a.getString("meal")!!), a.getLong("day"),
                            newFoodId = newId, onNewFoodHandled = { handle[NEW_FOOD] = 0L },
                            onBack = { nav.popBackStack() },
                            onCreateWithBarcode = { code -> nav.navigate("food/0?barcode=$code") },
                        )
                    }
                    composable(
                        "food/{id}?barcode={barcode}",
                        listOf(navArgument("id") { type = NavType.LongType }, navArgument("barcode") { type = NavType.StringType; defaultValue = "" }),
                    ) { entry ->
                        val id = entry.arguments!!.getLong("id")
                        FoodEditScreen(
                            id, entry.arguments!!.getString("barcode")?.ifEmpty { null },
                            onBack = { nav.popBackStack() },
                            onSaved = { saved -> nav.finishWithResult(if (id == 0L) saved else 0L) },
                        )
                    }
                    composable("recipe/{id}", listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                        val id = entry.arguments!!.getLong("id")
                        RecipeEditScreen(id, onBack = { nav.popBackStack() }, onSaved = { saved -> nav.finishWithResult(if (id == 0L) saved else 0L) })
                    }
                }
                AnimatedVisibility(
                    showNav, Modifier.align(Alignment.BottomCenter),
                    enter = slideInVertically(tween(260)) { it } + fadeIn(), exit = slideOutVertically(tween(200)) { it } + fadeOut(),
                ) {
                    FloatingNav(
                        route,
                        onSelect = { nav.goTab(it) },
                        onScan = { nav.navigate("scan/0/${Meal.forHour(java.time.LocalTime.now().hour).name}/${MacrowRepository.today()}") },
                    )
                }
                SnackbarHost(snack, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = if (showNav) 96.dp else 24.dp))
            }
        }
    }
}

/** Closes the current screen and, for a food or recipe that was just created, tells the screen behind it so it can open the log sheet. */
private fun NavHostController.finishWithResult(newFoodId: Long) {
    if (newFoodId > 0) previousBackStackEntry?.savedStateHandle?.set(NEW_FOOD, newFoodId)
    popBackStack()
}

private fun NavHostController.goTab(route: String) = navigate(route) {
    popUpTo("diary") { saveState = true }
    launchSingleTop = true
    restoreState = true
}
