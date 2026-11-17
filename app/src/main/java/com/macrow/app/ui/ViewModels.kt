package com.macrow.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.macrow.app.MacrowApp
import com.macrow.app.data.Entry
import com.macrow.app.data.EntryWithFood
import com.macrow.app.data.Food
import com.macrow.app.data.LookupException
import com.macrow.app.data.MacrowRepository
import com.macrow.app.data.OpenFoodFacts
import com.macrow.app.data.Settings
import com.macrow.app.domain.Nutrients
import com.macrow.app.domain.WeightPoint
import com.macrow.app.domain.loggingStreak
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Composable
inline fun <reified T : ViewModel> macrowVm(key: String? = null, crossinline make: (MacrowRepository) -> T): T {
    val repo = (LocalContext.current.applicationContext as MacrowApp).repo
    return viewModel(key = key, factory = viewModelFactory { initializer { make(repo) } })
}

/** Today as an epoch day, re-checked every minute so a screen left open past midnight rolls over. */
fun todayFlow(): Flow<Long> = flow {
    while (true) {
        emit(MacrowRepository.today())
        delay(60_000)
    }
}.distinctUntilChanged()

private fun <T> Flow<T>.hold(scope: kotlinx.coroutines.CoroutineScope, initial: T): StateFlow<T> =
    catch { UiErrors.report(it) }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), initial)

/** Totals of what was eaten (or planned) in [entries]. */
fun List<EntryWithFood>.total(): Nutrients = fold(Nutrients.ZERO) { a, e -> a + e.nutrients }

@OptIn(ExperimentalCoroutinesApi::class)
class DiaryVm(private val repo: MacrowRepository) : ViewModel() {
    val day = MutableStateFlow(MacrowRepository.today())
    val settings = repo.settingsFlow.hold(viewModelScope, Settings())
    val entries = day.flatMapLatest { repo.dayEntries(it, false) }.hold(viewModelScope, emptyList<EntryWithFood>())
    val water = day.flatMapLatest { repo.water(it) }.hold(viewModelScope, 0)
    val streak = combine(repo.loggedDays(), todayFlow()) { days, t -> loggingStreak(days.toSet(), t) }.hold(viewModelScope, 0)

    fun shift(days: Int) = day.update { it + days }
    fun goToday() { day.value = MacrowRepository.today() }
    fun addWater(ml: Int) = viewModelScope.launch(safeLaunch) { repo.addWater(day.value, ml) }
    fun undoWater() = viewModelScope.launch(safeLaunch) { repo.undoWater(day.value) }
    fun update(e: Entry) = viewModelScope.launch(safeLaunch) { repo.updateEntry(e) }
    fun delete(e: Entry) = viewModelScope.launch(safeLaunch) { repo.deleteEntry(e) }
    fun restore(e: Entry) = viewModelScope.launch(safeLaunch) { repo.restoreEntry(e) }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PlanVm(private val repo: MacrowRepository) : ViewModel() {
    val day = MutableStateFlow(MacrowRepository.today())
    val settings = repo.settingsFlow.hold(viewModelScope, Settings())
    val planned = day.flatMapLatest { repo.dayEntries(it, true) }.hold(viewModelScope, emptyList<EntryWithFood>())

    /** Planned entries for the whole week of the selected day, for the dots on the week strip. */
    val week = day.flatMapLatest { d ->
        val s = weekStart(d)
        repo.entriesBetween(s, s + 6, true)
    }.hold(viewModelScope, emptyList<EntryWithFood>())

    fun select(d: Long) { day.value = d }
    fun shiftWeek(weeks: Int) = day.update { it + 7L * weeks }
    fun update(e: Entry) = viewModelScope.launch(safeLaunch) { repo.updateEntry(e) }
    fun delete(e: Entry) = viewModelScope.launch(safeLaunch) { repo.deleteEntry(e) }
    fun restore(e: Entry) = viewModelScope.launch(safeLaunch) { repo.restoreEntry(e) }
    fun logDay(onDone: () -> Unit) = viewModelScope.launch(safeLaunch) { repo.logPlan(day.value); onDone() }

    companion object {
        /** Epoch day of the Monday of the week that holds [day]. */
        fun weekStart(day: Long): Long = LocalDate.ofEpochDay(day).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toEpochDay()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressVm(private val repo: MacrowRepository) : ViewModel() {
    val settings = repo.settingsFlow.hold(viewModelScope, Settings())
    val weights = repo.weights().map { list -> list.map { WeightPoint(it.day, it.kg) } }.hold(viewModelScope, emptyList<WeightPoint>())
    val week = todayFlow().flatMapLatest { t -> repo.entriesBetween(t - 6, t) }.hold(viewModelScope, emptyList<EntryWithFood>())
    val streak = combine(repo.loggedDays(), todayFlow()) { days, t -> loggingStreak(days.toSet(), t) }.hold(viewModelScope, 0)
    fun setWeight(day: Long, kg: Double) = viewModelScope.launch(safeLaunch) { repo.setWeight(day, kg) }
    fun deleteWeight(day: Long) = viewModelScope.launch(safeLaunch) { repo.deleteWeight(day) }
}

@OptIn(ExperimentalCoroutinesApi::class)
class NutrientsVm(private val repo: MacrowRepository) : ViewModel() {
    val settings = repo.settingsFlow.hold(viewModelScope, Settings())
    val today = todayFlow().flatMapLatest { repo.dayEntries(it, false) }.hold(viewModelScope, emptyList<EntryWithFood>())
    val week = todayFlow().flatMapLatest { t -> repo.entriesBetween(t - 6, t) }.hold(viewModelScope, emptyList<EntryWithFood>())
}

sealed interface Online {
    data object Idle : Online
    data object Loading : Online
    data class Done(val foods: List<Food>) : Online
    data class Failed(val message: String) : Online
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class AddFoodVm(private val repo: MacrowRepository) : ViewModel() {
    val query = MutableStateFlow("")
    val filter = MutableStateFlow("ALL")
    val foods = combine(query.debounce(120), filter) { q, f -> q to f }
        .flatMapLatest { (q, f) -> repo.foods(q, f) }
        .hold(viewModelScope, emptyList<Food>())
    val online = MutableStateFlow<Online>(Online.Idle)

    fun setQuery(q: String) { query.value = q; online.value = Online.Idle }
    fun setFilter(f: String) { filter.value = f }

    fun searchOnline() {
        val q = query.value.trim()
        if (q.length < 2) return
        online.value = Online.Loading
        viewModelScope.launch {
            online.value = try {
                Online.Done(OpenFoodFacts.search(q))
            } catch (e: CancellationException) {
                throw e
            } catch (e: LookupException) {
                Online.Failed(e.message ?: "Couldn't search online.")
            } catch (e: Exception) {
                Online.Failed("Couldn't search online.")
            }
        }
    }

    /** Saves an online food on the phone and returns the saved copy. */
    suspend fun keep(f: Food): Food = repo.cacheOnline(f)
    suspend fun load(id: Long): Food? = repo.food(id)

    fun log(day: Long, meal: String, food: Food, servings: Double, planned: Boolean) =
        viewModelScope.launch(safeLaunch) { repo.addEntry(day, meal, food, servings, planned) }
}

sealed interface ScanState {
    data object Idle : ScanState
    data object Looking : ScanState
    data class Found(val food: Food) : ScanState
    data class Missing(val code: String) : ScanState
    data class Failed(val message: String) : ScanState
}

class ScanVm(private val repo: MacrowRepository) : ViewModel() {
    val state = MutableStateFlow<ScanState>(ScanState.Idle)

    fun lookup(code: String) {
        val c = code.trim()
        state.value = ScanState.Looking
        viewModelScope.launch {
            state.value = try {
                val local = repo.foodByBarcode(c)
                if (local != null) {
                    ScanState.Found(local)
                } else {
                    val online = OpenFoodFacts.byBarcode(c)
                    if (online == null) ScanState.Missing(c) else ScanState.Found(repo.cacheOnline(online))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: LookupException) {
                ScanState.Failed(e.message ?: "Couldn't look that up.")
            } catch (e: Exception) {
                ScanState.Failed("Couldn't look that up.")
            }
        }
    }

    fun reset() { state.value = ScanState.Idle }
    fun openFood(id: Long) = viewModelScope.launch(safeLaunch) { repo.food(id)?.let { state.value = ScanState.Found(it) } }
    fun fail(message: String) { state.value = ScanState.Failed(message) }
    fun log(day: Long, meal: String, food: Food, servings: Double, planned: Boolean) =
        viewModelScope.launch(safeLaunch) { repo.addEntry(day, meal, food, servings, planned) }
}
