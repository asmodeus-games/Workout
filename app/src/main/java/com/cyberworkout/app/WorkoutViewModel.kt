package com.cyberworkout.app

import android.app.Application
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.content.Context
import android.media.RingtoneManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AppState {
    IDLE,
    ACTIVE,
    REST,
    EDITING
}

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WorkoutRepository(application)
    
    private val _appState = MutableStateFlow(AppState.IDLE)
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    private val _workoutState = MutableStateFlow(WorkoutState())
    val workoutState: StateFlow<WorkoutState> = _workoutState.asStateFlow()

    private val _routines = MutableStateFlow<List<Routine>>(emptyList())
    val routines: StateFlow<List<Routine>> = _routines.asStateFlow()

    private val _currentRoutine = MutableStateFlow<Routine?>(null)
    val currentRoutine: StateFlow<Routine?> = _currentRoutine.asStateFlow()
    
    private var timerJob: Job? = null
    
    init {
        viewModelScope.launch {
            repository.routinesFlow.collect { _routines.value = it }
        }
        
        viewModelScope.launch {
            val savedState = repository.workoutStateFlow.first()
            _workoutState.value = savedState
            
            val routine = _routines.value.find { it.id == savedState.selectedRoutineId } ?: _routines.value.firstOrNull()
            _currentRoutine.value = routine

            if (routine != null && isFinished(savedState, routine)) {
                _appState.value = AppState.IDLE
            } else if (savedState.isResting) {
                _appState.value = AppState.REST
                startRestTimer(savedState.remainingRestTime)
            } else if (savedState.selectedRoutineId != null) {
                _appState.value = AppState.ACTIVE
            }
        }
    }

    private fun isFinished(state: WorkoutState, routine: Routine): Boolean {
        return state.currentExerciseIndex >= routine.exercises.size
    }
    
    fun selectRoutine(routine: Routine) {
        _currentRoutine.value = routine
    }

    fun startWorkout() {
        val routine = _currentRoutine.value ?: return
        viewModelScope.launch {
            repository.resetWorkout()
            repository.setSelectedRoutineId(routine.id)
            _workoutState.value = WorkoutState(selectedRoutineId = routine.id)
            _appState.value = AppState.ACTIVE
            saveState()
        }
    }

    fun cancelWorkout() {
        timerJob?.cancel()
        viewModelScope.launch {
            repository.resetWorkout()
            repository.setSelectedRoutineId(null)
            _workoutState.value = WorkoutState()
            _appState.value = AppState.IDLE
        }
    }

    fun completeSet() {
        val currentState = _workoutState.value
        val routine = _currentRoutine.value ?: return
        
        vibrateSinglePulse()
        
        if (routine.mode == WorkoutMode.SEQUENTIAL) {
            handleSequentialSet(currentState, routine)
        } else {
            handleCircuitSet(currentState, routine)
        }
    }

    private fun handleSequentialSet(currentState: WorkoutState, routine: Routine) {
        val currentExercise = routine.exercises[currentState.currentExerciseIndex]
        if (currentState.currentSet >= currentExercise.sets) {
            if (currentState.currentExerciseIndex + 1 >= routine.exercises.size) {
                finishWorkout()
            } else {
                val newState = currentState.copy(
                    currentExerciseIndex = currentState.currentExerciseIndex + 1,
                    currentSet = 1,
                    remainingRestTime = currentExercise.restTimeSeconds,
                    isResting = true
                )
                updateAndStartRest(newState)
            }
        } else {
            val newState = currentState.copy(
                currentSet = currentState.currentSet + 1,
                remainingRestTime = currentExercise.restTimeSeconds,
                isResting = true
            )
            updateAndStartRest(newState)
        }
    }

    private fun handleCircuitSet(currentState: WorkoutState, routine: Routine) {
        // Circuit: Exercise 1 (Set 1) -> Break -> Exercise 2 (Set 1) -> ... -> Exercise N (Set 1) -> Break -> Exercise 1 (Set 2)
        if (currentState.currentExerciseIndex + 1 >= routine.exercises.size) {
            // End of a lap
            val nextSet = currentState.currentSet + 1
            // Check if any exercise has more sets
            val maxSets = routine.exercises.maxOf { it.sets }
            if (nextSet > maxSets) {
                finishWorkout()
            } else {
                val newState = currentState.copy(
                    currentExerciseIndex = 0,
                    currentSet = nextSet,
                    remainingRestTime = routine.circuitBreakSeconds,
                    isResting = true
                )
                updateAndStartRest(newState)
            }
        } else {
            val newState = currentState.copy(
                currentExerciseIndex = currentState.currentExerciseIndex + 1,
                remainingRestTime = routine.circuitBreakSeconds,
                isResting = true
            )
            updateAndStartRest(newState)
        }
    }

    private fun finishWorkout() {
        _workoutState.value = _workoutState.value.copy(currentExerciseIndex = 999) // Mark as finished
        _appState.value = AppState.IDLE
        viewModelScope.launch { repository.resetWorkout() }
    }

    private fun updateAndStartRest(newState: WorkoutState) {
        _workoutState.value = newState
        _appState.value = AppState.REST
        startRestTimer(newState.remainingRestTime)
        saveState()
    }

    fun skipSet() {
        completeSet()
    }
    
    private fun startRestTimer(initialTimeSeconds: Int) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var timeRemaining = initialTimeSeconds
            while (timeRemaining > 0) {
                delay(1000L)
                timeRemaining--
                val newState = _workoutState.value.copy(remainingRestTime = timeRemaining)
                _workoutState.value = newState
                saveState()
            }
            vibrateDoublePulse()
            playNotificationSound()
            _workoutState.value = _workoutState.value.copy(remainingRestTime = 0, isResting = false)
            _appState.value = AppState.ACTIVE
            saveState()
        }
    }

    fun skipRest() {
        timerJob?.cancel()
        _workoutState.value = _workoutState.value.copy(remainingRestTime = 0, isResting = false)
        _appState.value = AppState.ACTIVE
        saveState()
    }

    fun saveRoutines(newRoutines: List<Routine>) {
        viewModelScope.launch {
            repository.saveRoutines(newRoutines)
            _routines.value = newRoutines
        }
    }

    fun setAppState(state: AppState) {
        _appState.value = state
    }

    private fun saveState() {
        viewModelScope.launch {
            repository.updateState(_workoutState.value)
        }
    }
    
    private fun getVibrator(): Vibrator {
        val context = getApplication<Application>()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun vibrateSinglePulse() {
        val vibrator = getVibrator()
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(100)
            }
        }
    }

    private fun vibrateDoublePulse() {
        val vibrator = getVibrator()
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 150), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 150, 100, 150), -1)
            }
        }
    }

    private fun playNotificationSound() {
        try {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val r = RingtoneManager.getRingtone(getApplication<Application>(), notification)
            r.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
