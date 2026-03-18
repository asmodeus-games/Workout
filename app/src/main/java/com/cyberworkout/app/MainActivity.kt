package com.cyberworkout.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.UUID

val CyberBlack = Color(0xFF000000)
val CyberCyan = Color(0xFF00F5FF)
val CyberPink = Color(0xFFFF0055)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = CyberBlack,
                    surface = Color(0xFF111111),
                    primary = CyberCyan,
                    secondary = CyberPink,
                    onBackground = Color.White,
                    onSurface = Color.White,
                    onPrimary = Color.Black
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: WorkoutViewModel = viewModel()
                    val appState by viewModel.appState.collectAsState()
                    val workoutState by viewModel.workoutState.collectAsState()
                    val currentRoutine by viewModel.currentRoutine.collectAsState()
                    val routines by viewModel.routines.collectAsState()

                    DisposableEffect(appState) {
                        if (appState == AppState.ACTIVE || appState == AppState.REST) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                        onDispose {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = CyberBlack,
                        contentColor = Color.White
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            @OptIn(ExperimentalAnimationApi::class)
                            AnimatedContent(
                                targetState = appState,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                                },
                                label = "AppStateTransition"
                            ) { state ->
                                when (state) {
                                    AppState.IDLE -> IdleScreen(
                                        routines = routines,
                                        selectedRoutine = currentRoutine,
                                        onSelect = { viewModel.selectRoutine(it) },
                                        onStart = { viewModel.startWorkout() },
                                        onEdit = { viewModel.setAppState(AppState.EDITING) }
                                    )
                                    AppState.ACTIVE -> ActiveScreen(
                                        routine = currentRoutine,
                                        workoutState = workoutState,
                                        onDone = { viewModel.completeSet() },
                                        onSkip = { viewModel.skipSet() },
                                        onCancel = { viewModel.cancelWorkout() }
                                    )
                                    AppState.REST -> RestScreen(
                                        routine = currentRoutine,
                                        workoutState = workoutState,
                                        onSkipRest = { viewModel.skipRest() },
                                        onCancel = { viewModel.cancelWorkout() }
                                    )
                                    AppState.EDITING -> RoutineEditorScreen(
                                        routines = routines,
                                        onSave = { 
                                            viewModel.saveRoutines(it)
                                            viewModel.setAppState(AppState.IDLE)
                                        },
                                        onBack = { viewModel.setAppState(AppState.IDLE) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CancelButton(onCancel: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.padding(16.dp)) {
        IconButton(
            onClick = { showDialog = true },
            modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
        ) {
            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Cancel Workout?") },
            text = { Text("Do you really want to end this session? All progress will be lost.") },
            confirmButton = {
                TextButton(onClick = { 
                    showDialog = false
                    onCancel()
                }) {
                    Text("YES, CANCEL", color = CyberPink)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("CONTINUE", color = CyberCyan)
                }
            },
            containerColor = Color(0xFF111111),
            titleContentColor = Color.White,
            textContentColor = Color.Gray
        )
    }
}

@Composable
fun IdleScreen(
    routines: List<Routine>,
    selectedRoutine: Routine?,
    onSelect: (Routine) -> Unit,
    onStart: () -> Unit,
    onEdit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "WORKOUT",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = CyberCyan
            )
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Settings, contentDescription = "Edit Routines", tint = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(routines) { routine ->
                val isSelected = routine.id == selectedRoutine?.id
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFF222222) else Color(0xFF111111)
                    ),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CyberCyan) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(routine) }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = routine.name.uppercase(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) CyberCyan else Color.White
                        )
                        Text(
                            text = "${routine.exercises.size} Exercises • ${routine.mode.name}",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
        
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CyberPink),
            enabled = selectedRoutine != null
        ) {
            Text("START SESSION", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
        }
    }
}

@Composable
fun ActiveScreen(
    routine: Routine?,
    workoutState: WorkoutState,
    onDone: () -> Unit,
    onSkip: () -> Unit,
    onCancel: () -> Unit
) {
    if (routine == null || workoutState.currentExerciseIndex >= routine.exercises.size) return
    val currentExercise = routine.exercises[workoutState.currentExerciseIndex]
    
    var totalDrag by remember { mutableFloatStateOf(0f) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = { totalDrag = 0f },
                        onDragCancel = { totalDrag = 0f }
                    ) { change, dragAmount ->
                        change.consume()
                        totalDrag += dragAmount
                        if (totalDrag < -150f) {
                            totalDrag = 0f
                            onSkip()
                        }
                    }
                }
        ) {
            Spacer(modifier = Modifier.height(64.dp))
            SegmentedProgressBar(
                currentExerciseIndex = workoutState.currentExerciseIndex,
                totalExercises = routine.exercises.size
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "SET ${workoutState.currentSet} / ${currentExercise.sets}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = CyberPink
                )
                Spacer(modifier = Modifier.height(16.dp))
                @OptIn(ExperimentalAnimationApi::class)
                AnimatedContent(
                    targetState = currentExercise.name,
                    transitionSpec = {
                        fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                    }, label = "ExerciseNameAnim"
                ) { name ->
                    Text(
                        text = name.uppercase(),
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 48.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "TARGET: ${currentExercise.target}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal,
                    color = CyberCyan
                )
            }
            
            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.3f),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
            ) {
                Text("DONE", fontSize = 40.sp, fontWeight = FontWeight.Black, color = Color.Black)
            }
        }

        CancelButton(onCancel = onCancel)
    }
}

@Composable
fun RestScreen(
    routine: Routine?,
    workoutState: WorkoutState,
    onSkipRest: () -> Unit,
    onCancel: () -> Unit
) {
    if (routine == null) return
    val nextExercise = if (workoutState.currentExerciseIndex < routine.exercises.size) {
        routine.exercises[workoutState.currentExerciseIndex]
    } else null
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val glowAlpha = if (workoutState.remainingRestTime <= 3) alphaAnim * 2 else alphaAnim

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind { drawRect(color = CyberCyan.copy(alpha = glowAlpha)) }
            .clickable { onSkipRest() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "REST", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = CyberPink)
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "${workoutState.remainingRestTime}", fontSize = 120.sp, fontWeight = FontWeight.Black, color = Color.White)
            Spacer(modifier = Modifier.height(24.dp))
            if (nextExercise != null) {
                Text(text = "UP NEXT: ${nextExercise.name}", fontSize = 20.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(48.dp))
            Text(text = "Tap to skip", fontSize = 16.sp, color = Color.White.copy(alpha = 0.5f))
        }

        CancelButton(onCancel = onCancel)
    }
}

@Composable
fun RoutineEditorScreen(
    routines: List<Routine>,
    onSave: (List<Routine>) -> Unit,
    onBack: () -> Unit
) {
    var editableRoutines by remember { mutableStateOf(routines) }
    var selectedRoutineIndex by remember { mutableIntStateOf(0) }
    
    val currentRoutine = editableRoutines.getOrNull(selectedRoutineIndex)

    BackHandler(onBack = onBack)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) }
            Text("MANAGE ROUTINES", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = { onSave(editableRoutines) }, colors = ButtonDefaults.buttonColors(containerColor = CyberPink)) {
                Text("SAVE")
            }
        }

        if (currentRoutine != null) {
            OutlinedTextField(
                value = currentRoutine.name,
                onValueChange = { newName ->
                    editableRoutines = editableRoutines.toMutableList().also {
                        it[selectedRoutineIndex] = it[selectedRoutineIndex].copy(name = newName)
                    }
                },
                label = { Text("Routine Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                Text("MODE: ", color = Color.Gray)
                Text(currentRoutine.mode.name, color = CyberCyan, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = currentRoutine.mode == WorkoutMode.CIRCUIT,
                    onCheckedChange = { isCircuit ->
                        val newMode = if (isCircuit) WorkoutMode.CIRCUIT else WorkoutMode.SEQUENTIAL
                        editableRoutines = editableRoutines.toMutableList().also {
                            it[selectedRoutineIndex] = it[selectedRoutineIndex].copy(mode = newMode)
                        }
                    }
                )
            }
            
            if (currentRoutine.mode == WorkoutMode.CIRCUIT) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Circuit Break: ", color = Color.Gray)
                    Slider(
                        value = currentRoutine.circuitBreakSeconds.toFloat(),
                        onValueChange = { val newVal = it.toInt(); editableRoutines = editableRoutines.toMutableList().also { list -> list[selectedRoutineIndex] = list[selectedRoutineIndex].copy(circuitBreakSeconds = newVal) } },
                        valueRange = 0f..120f,
                        modifier = Modifier.weight(1f)
                    )
                    Text("${currentRoutine.circuitBreakSeconds}s", color = Color.White, modifier = Modifier.width(40.dp))
                }
            }

            Text("EXERCISES", fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(currentRoutine.exercises) { idx, exercise ->
                    Card(modifier = Modifier.padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row {
                                OutlinedTextField(
                                    value = exercise.name,
                                    onValueChange = { n ->
                                        val newList = currentRoutine.exercises.toMutableList().also { it[idx] = it[idx].copy(name = n) }
                                        editableRoutines = editableRoutines.toMutableList().also { it[selectedRoutineIndex] = it[selectedRoutineIndex].copy(exercises = newList) }
                                    },
                                    label = { Text("Exercise Name") },
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = {
                                    val newList = currentRoutine.exercises.toMutableList().also { it.removeAt(idx) }
                                    editableRoutines = editableRoutines.toMutableList().also { it[selectedRoutineIndex] = it[selectedRoutineIndex].copy(exercises = newList) }
                                }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red) }
                            }
                            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = exercise.sets.toString(),
                                    onValueChange = { s ->
                                        val newVal = s.toIntOrNull() ?: 0
                                        val newList = currentRoutine.exercises.toMutableList().also { it[idx] = it[idx].copy(sets = newVal) }
                                        editableRoutines = editableRoutines.toMutableList().also { it[selectedRoutineIndex] = it[selectedRoutineIndex].copy(exercises = newList) }
                                    },
                                    label = { Text("Sets") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = exercise.restTimeSeconds.toString(),
                                    onValueChange = { r ->
                                        val newVal = r.toIntOrNull() ?: 0
                                        val newList = currentRoutine.exercises.toMutableList().also { it[idx] = it[idx].copy(restTimeSeconds = newVal) }
                                        editableRoutines = editableRoutines.toMutableList().also { it[selectedRoutineIndex] = it[selectedRoutineIndex].copy(exercises = newList) }
                                    },
                                    label = { Text("Rest(s)") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = exercise.target,
                                    onValueChange = { t ->
                                        val newList = currentRoutine.exercises.toMutableList().also { it[idx] = it[idx].copy(target = t) }
                                        editableRoutines = editableRoutines.toMutableList().also { it[selectedRoutineIndex] = it[selectedRoutineIndex].copy(exercises = newList) }
                                    },
                                    label = { Text("Target") },
                                    modifier = Modifier.weight(1.5f)
                                )
                            }
                        }
                    }
                }
                item {
                    Button(
                        onClick = {
                            val newList = currentRoutine.exercises.toMutableList().also { it.add(Exercise(name = "New Exercise", sets = 3, restTimeSeconds = 60, target = "10 reps")) }
                            editableRoutines = editableRoutines.toMutableList().also { it[selectedRoutineIndex] = it[selectedRoutineIndex].copy(exercises = newList) }
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("ADD EXERCISE")
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val newRoutine = Routine(name = "New Routine", exercises = emptyList())
                    editableRoutines = editableRoutines + newRoutine
                    selectedRoutineIndex = editableRoutines.size - 1
                },
                modifier = Modifier.weight(1f)
            ) { Text("NEW ROUTINE") }
            
            if (editableRoutines.size > 1) {
                Button(
                    onClick = {
                        editableRoutines = editableRoutines.toMutableList().also { it.removeAt(selectedRoutineIndex) }
                        selectedRoutineIndex = 0
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("DELETE ROUTINE") }
            }
        }
    }
}

@Composable
fun SegmentedProgressBar(currentExerciseIndex: Int, totalExercises: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (i in 0 until totalExercises) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (i < currentExerciseIndex) CyberCyan
                        else if (i == currentExerciseIndex) CyberPink
                        else Color(0xFF222222)
                    )
            )
        }
    }
}
