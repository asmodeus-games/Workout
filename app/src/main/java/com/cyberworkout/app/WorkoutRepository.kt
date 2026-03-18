package com.cyberworkout.app

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "workout_prefs")

class WorkoutRepository(private val context: Context) {

    companion object {
        val EXERCISE_INDEX = intPreferencesKey("current_exercise_index")
        val CURRENT_SET = intPreferencesKey("current_set")
        val REMAINING_REST = intPreferencesKey("remaining_rest_time")
        val IS_RESTING = booleanPreferencesKey("is_resting")
        val SELECTED_ROUTINE_ID = stringPreferencesKey("selected_routine_id")
        val ROUTINES_JSON = stringPreferencesKey("routines_json")
    }

    val workoutStateFlow: Flow<WorkoutState> = context.dataStore.data
        .map { preferences ->
            WorkoutState(
                currentExerciseIndex = preferences[EXERCISE_INDEX] ?: 0,
                currentSet = preferences[CURRENT_SET] ?: 1,
                remainingRestTime = preferences[REMAINING_REST] ?: 0,
                isResting = preferences[IS_RESTING] ?: false,
                selectedRoutineId = preferences[SELECTED_ROUTINE_ID]
            )
        }

    val routinesFlow: Flow<List<Routine>> = context.dataStore.data
        .map { preferences ->
            val json = preferences[ROUTINES_JSON]
            if (json == null) {
                listOf(defaultRoutine)
            } else {
                try {
                    Json.decodeFromString<List<Routine>>(json)
                } catch (e: Exception) {
                    listOf(defaultRoutine)
                }
            }
        }

    suspend fun updateState(state: WorkoutState) {
        context.dataStore.edit { preferences ->
            preferences[EXERCISE_INDEX] = state.currentExerciseIndex
            preferences[CURRENT_SET] = state.currentSet
            preferences[REMAINING_REST] = state.remainingRestTime
            preferences[IS_RESTING] = state.isResting
            state.selectedRoutineId?.let { preferences[SELECTED_ROUTINE_ID] = it }
        }
    }

    suspend fun saveRoutines(routines: List<Routine>) {
        context.dataStore.edit { preferences ->
            preferences[ROUTINES_JSON] = Json.encodeToString(routines)
        }
    }

    suspend fun setSelectedRoutineId(id: String?) {
        context.dataStore.edit { preferences ->
            if (id == null) preferences.remove(SELECTED_ROUTINE_ID)
            else preferences[SELECTED_ROUTINE_ID] = id
        }
    }
    
    suspend fun resetWorkout() {
        context.dataStore.edit { preferences ->
            preferences.remove(EXERCISE_INDEX)
            preferences.remove(CURRENT_SET)
            preferences.remove(REMAINING_REST)
            preferences.remove(IS_RESTING)
        }
    }
}

data class WorkoutState(
    val currentExerciseIndex: Int = 0,
    val currentSet: Int = 1,
    val remainingRestTime: Int = 0,
    val isResting: Boolean = false,
    val selectedRoutineId: String? = null
)
