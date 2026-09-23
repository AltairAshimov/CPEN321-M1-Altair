package com.example.cpen321application.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.cpen321application.BuildConfig
import com.example.cpen321application.auth.signInWithGoogle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.NetworkInterface
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginServerScreen(navController: NavController) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var serverIp by remember { mutableStateOf("-") }
    var clientIp by remember { mutableStateOf(getClientIp()) }
    var serverTime by remember { mutableStateOf("-") }
    var clientTime by remember { mutableStateOf(getClientLocalTimeString()) }
    var firstName by remember { mutableStateOf("-") }
    var lastName by remember { mutableStateOf("-") }
    var googleUserName by remember { mutableStateOf<String?>(null) }

    suspend fun loadData() {
        isLoading = true
        errorMessage = null

        clientIp = getClientIp()
        clientTime = getClientLocalTimeString()

        val googleClientId = BuildConfig.GOOGLE_CLIENT_ID
        if (googleClientId.isBlank()) {
            errorMessage = "Google sign-in is not configured. Add a valid OAuth web client ID to frontend/local.properties and rebuild the app."
            isLoading = false
            return
        }

        val signedInUser = signInWithGoogle(context, googleClientId)

        if (signedInUser == null) {
            errorMessage = "Google sign-in was not completed. Please try again."
            isLoading = false
            return
        }

        googleUserName = signedInUser.displayName

        val baseUrl = BuildConfig.API_BASE_URL.trimEnd('/')

        try {
            coroutineScope {
                val results = listOf(
                    async { fetchJson("$baseUrl/api/name") },
                    async { fetchJson("$baseUrl/api/server-time") },
                    async { fetchJson("$baseUrl/api/server-ip") },
                )

                val nameJson = results[0].await()
                val serverTimeJson = results[1].await()
                val serverIpJson = results[2].await()

                firstName = nameJson.getString("firstName")
                lastName = nameJson.getString("lastName")
                serverTime = serverTimeJson.getString("time")
                serverIp = serverIpJson.getString("ip")
                errorMessage = null
            }
        } catch (e: Exception) {
            errorMessage = "Could not reach the backend. Check that the server is running and try again."
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Login + Server") },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator()
                }
                errorMessage != null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            text = errorMessage ?: "Something went wrong",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                        )

                        Button(onClick = {
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                loadData()
                            }
                        }) {
                            Text("Try Again")
                        }
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Server IP",
                            fontWeight = FontWeight.Bold,
                        )
                        Text(serverIp)

                        Text(
                            text = "Client IP",
                            fontWeight = FontWeight.Bold,
                        )
                        Text(clientIp)

                        Text(
                            text = "Server local time",
                            fontWeight = FontWeight.Bold,
                        )
                        Text(serverTime)

                        Text(
                            text = "Client local time",
                            fontWeight = FontWeight.Bold,
                        )
                        Text(clientTime)

                        Text(
                            text = "My name",
                            fontWeight = FontWeight.Bold,
                        )
                        Text("$firstName $lastName")

                        Text(
                            text = "Google user",
                            fontWeight = FontWeight.Bold,
                        )
                        Text(googleUserName ?: "-")
                    }
                }
            }
        }
    }
}

private fun getClientIp(): String {
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces() ?: return "Unknown"

        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            if (networkInterface.isLoopback || !networkInterface.isUp) {
                continue
            }

            val addresses = networkInterface.inetAddresses ?: continue
            while (addresses.hasMoreElements()) {
                val address = addresses.nextElement()
                val hostAddress = address.hostAddress ?: continue
                if (!address.isLoopbackAddress && !hostAddress.contains(":")) {
                    return hostAddress
                }
            }
        }
    } catch (_: Exception) {
        return "Unknown"
    }

    return "Unknown"
}

private fun getClientLocalTimeString(): String {
    val now = Date()
    val timezone = TimeZone.getDefault()
    val formatter = SimpleDateFormat("HH:mm:ss", Locale.US)
    formatter.timeZone = timezone

    val time = formatter.format(now)
    val offsetMinutes = timezone.getOffset(now.time) / 60_000
    val sign = if (offsetMinutes >= 0) "+" else "-"
    val absoluteOffsetMinutes = kotlin.math.abs(offsetMinutes)
    val offsetHours = absoluteOffsetMinutes / 60
    val offsetMinuteRemainder = absoluteOffsetMinutes % 60

    return "$time GMT${sign}${offsetHours.toString().padStart(2, '0')}:${offsetMinuteRemainder.toString().padStart(2, '0')}"
}

private suspend fun fetchJson(urlString: String): JSONObject = withContext(Dispatchers.IO) {
    val url = URL(urlString)
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.connectTimeout = 5_000
    connection.readTimeout = 5_000

    val responseCode = connection.responseCode
    if (responseCode !in 200..299) {
        val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }
        throw Exception("HTTP $responseCode${errorBody?.let { ": $it" } ?: ""}")
    }

    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
    JSONObject(responseText)
}
