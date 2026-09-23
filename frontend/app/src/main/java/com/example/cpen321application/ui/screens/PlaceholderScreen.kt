package com.example.cpen321application.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class FallingItemSpec(
    val delayMs: Int,
    val rotationEnd: Float,
    val distance: Float,
    val durationMs: Int,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderScreen(
    title: String,
    navController: NavController,
) {
    var minutesInput by rememberSaveable { mutableStateOf("") }
    var secondsInput by rememberSaveable { mutableStateOf("") }
    var remainingSeconds by rememberSaveable { mutableStateOf(0) }
    var isRunning by rememberSaveable { mutableStateOf(false) }
    var validationMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isSurpriseActive by rememberSaveable { mutableStateOf(false) }
    var isFallComplete by rememberSaveable { mutableStateOf(false) }

    val warningAlpha = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    val fallingSpecs = remember {
        listOf(
            FallingItemSpec(delayMs = 0, rotationEnd = 14f, distance = 1400f, durationMs = 780),
            FallingItemSpec(delayMs = 80, rotationEnd = -10f, distance = 1320f, durationMs = 820),
            FallingItemSpec(delayMs = 160, rotationEnd = 12f, distance = 1260f, durationMs = 840),
            FallingItemSpec(delayMs = 240, rotationEnd = -8f, distance = 1200f, durationMs = 900),
            FallingItemSpec(delayMs = 320, rotationEnd = 9f, distance = 1160f, durationMs = 960),
        )
    }
    val fallOffsets = remember { fallingSpecs.map { Animatable(0f) } }
    val fallRotations = remember { fallingSpecs.map { Animatable(0f) } }

    fun resetTimerState() {
        isRunning = false
        remainingSeconds = 0
        validationMessage = null
        isSurpriseActive = false
        isFallComplete = false
        coroutineScope.launch {
            warningAlpha.snapTo(0f)
            fallOffsets.forEach { it.snapTo(0f) }
            fallRotations.forEach { it.snapTo(0f) }
        }
    }

    fun startTimer() {
        val minutes = minutesInput.ifBlank { "0" }.toIntOrNull()
        val seconds = secondsInput.ifBlank { "0" }.toIntOrNull()

        if (minutes == null || seconds == null) {
            validationMessage = "Please enter a valid timer."
            return
        }

        if (minutes < 0 || seconds !in 0..59) {
            validationMessage = "Minutes >= 0, seconds 0-59."
            return
        }

        val totalSeconds = minutes * 60 + seconds
        if (totalSeconds <= 0) {
            validationMessage = "Total time must be greater than 0."
            return
        }

        validationMessage = null
        remainingSeconds = totalSeconds
        isRunning = true
        isSurpriseActive = false
        isFallComplete = false
        coroutineScope.launch {
            warningAlpha.snapTo(0f)
            fallOffsets.forEach { it.snapTo(0f) }
            fallRotations.forEach { it.snapTo(0f) }
        }
    }

    LaunchedEffect(isRunning) {
        if (!isRunning) return@LaunchedEffect

        while (isRunning && remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds -= 1
        }

        if (isRunning && remainingSeconds <= 0) {
            isRunning = false
            remainingSeconds = 0
            isSurpriseActive = true
            isFallComplete = false
        }
    }

    LaunchedEffect(isSurpriseActive) {
        if (!isSurpriseActive) {
            warningAlpha.snapTo(0f)
            fallOffsets.forEach { it.snapTo(0f) }
            fallRotations.forEach { it.snapTo(0f) }
            return@LaunchedEffect
        }

        warningAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        )

        fallingSpecs.forEachIndexed { index, spec ->
            launch {
                delay(spec.delayMs.toLong())
                fallRotations[index].animateTo(
                    spec.rotationEnd,
                    animationSpec = tween(durationMillis = spec.durationMs, easing = FastOutSlowInEasing),
                )
                fallOffsets[index].animateTo(
                    spec.distance,
                    animationSpec = tween(durationMillis = spec.durationMs, easing = FastOutSlowInEasing),
                )
            }
        }

        launch {
            val totalFallMs = fallingSpecs.maxOf { it.delayMs + it.durationMs } + 120
            delay(totalFallMs.toLong())
            isFallComplete = true
        }
    }

    val buttonColors = ButtonDefaults.buttonColors(
        containerColor = Color(0xFFF7E8E8),
        contentColor = Color(0xFF3A0707),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        if (isSurpriseActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF4B0707))
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter,
            ) {
                Text(
                    text = "GRAVITY OFF",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    modifier = Modifier
                        .padding(top = 72.dp)
                        .alpha(warningAlpha.value),
                )

                Text(
                    text = "GRAVITY SYSTEM FAILURE",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFFFD7D1),
                    modifier = Modifier
                        .padding(top = 150.dp)
                        .alpha(warningAlpha.value),
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 220.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "Timer",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        modifier = Modifier.graphicsLayer {
                            translationY = fallOffsets[4].value
                            rotationZ = fallRotations[4].value
                            alpha = if (fallOffsets[4].value > 1050f) 0f else 1f
                        },
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                translationY = fallOffsets[3].value
                                rotationZ = fallRotations[3].value
                                alpha = if (fallOffsets[3].value > 1000f) 0f else 1f
                            },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = minutesInput,
                            onValueChange = {},
                            label = { Text("Minutes") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            enabled = false,
                            modifier = Modifier.width(120.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color(0xFFFFB5A9),
                                focusedLabelColor = Color.White,
                                unfocusedLabelColor = Color(0xFFFFE4E0),
                            ),
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(text = ":", color = Color.White, style = MaterialTheme.typography.headlineSmall)

                        Spacer(modifier = Modifier.width(12.dp))

                        OutlinedTextField(
                            value = secondsInput,
                            onValueChange = {},
                            label = { Text("Seconds") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            enabled = false,
                            modifier = Modifier.width(120.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color(0xFFFFB5A9),
                                focusedLabelColor = Color.White,
                                unfocusedLabelColor = Color(0xFFFFE4E0),
                            ),
                        )
                    }

                    Text(
                        text = formatTimer(remainingSeconds),
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                        modifier = Modifier.graphicsLayer {
                            translationY = fallOffsets[2].value
                            rotationZ = fallRotations[2].value
                            alpha = if (fallOffsets[2].value > 980f) 0f else 1f
                        },
                    )

                    Button(
                        onClick = {},
                        enabled = false,
                        colors = buttonColors,
                        modifier = Modifier.graphicsLayer {
                            translationY = fallOffsets[1].value
                            rotationZ = fallRotations[1].value
                            alpha = if (fallOffsets[1].value > 940f) 0f else 1f
                        },
                    ) {
                        Text("Start Timer")
                    }

                    Button(
                        onClick = {},
                        enabled = false,
                        colors = buttonColors,
                        modifier = Modifier.graphicsLayer {
                            translationY = fallOffsets[0].value
                            rotationZ = fallRotations[0].value
                            alpha = if (fallOffsets[0].value > 900f) 0f else 1f
                        },
                    ) {
                        Text("Reset")
                    }
                }

                if (isFallComplete) {
                    Button(
                        onClick = {
                            resetTimerState()
                            minutesInput = ""
                            secondsInput = ""
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp),
                        colors = buttonColors,
                    ) {
                        Text("Restore Gravity")
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "Timer",
                        style = MaterialTheme.typography.headlineMedium,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = minutesInput,
                            onValueChange = { value ->
                                if (!isRunning && value.isEmpty() || value.all { it.isDigit() }) {
                                    minutesInput = value
                                }
                            },
                            label = { Text("Minutes") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            enabled = !isRunning,
                            modifier = Modifier.width(120.dp),
                        )

                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = ":", style = MaterialTheme.typography.headlineSmall)
                        Spacer(modifier = Modifier.width(12.dp))

                        OutlinedTextField(
                            value = secondsInput,
                            onValueChange = { value ->
                                if (!isRunning && value.isEmpty() || value.all { it.isDigit() }) {
                                    secondsInput = value
                                }
                            },
                            label = { Text("Seconds") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            enabled = !isRunning,
                            modifier = Modifier.width(120.dp),
                        )
                    }

                    Text(
                        text = formatTimer(remainingSeconds),
                        style = MaterialTheme.typography.displaySmall,
                    )

                    Button(
                        onClick = { startTimer() },
                        enabled = !isRunning,
                    ) {
                        Text("Start Timer")
                    }

                    Button(
                        onClick = {
                            resetTimerState()
                            minutesInput = ""
                            secondsInput = ""
                        },
                    ) {
                        Text("Reset")
                    }

                    if (!validationMessage.isNullOrBlank()) {
                        Text(
                            text = validationMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

private fun formatTimer(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
