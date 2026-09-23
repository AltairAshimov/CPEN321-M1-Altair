package com.example.cpen321application.ui.screens

import android.graphics.Color as AndroidColor
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

private const val GRID_SIZE = 16
private const val PIXEL_WS_URL = "wss://136.116.252.182/ws/pixels"

@Composable
fun PixelGridScreen(navController: NavController) {
    val pixels = remember { mutableStateMapOf<Pair<Int, Int>, Color>() }
    var connectionStatus by remember { mutableStateOf("Connecting") }
    var websocket: WebSocket? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(PIXEL_WS_URL)
            .build()

        websocket = client.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    postToMain { connectionStatus = "Connected" }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val payload = JSONObject(text)
                        val x = payload.optInt("x", -1)
                        val y = payload.optInt("y", -1)
                        val colorHex = payload.optString("color", "")

                        if (x !in 0 until GRID_SIZE || y !in 0 until GRID_SIZE || colorHex.isBlank()) {
                            return
                        }

                        val color = try {
                            Color(AndroidColor.parseColor(colorHex))
                        } catch (e: IllegalArgumentException) {
                            Log.w("M1_DEBUG", "Malformed pixel color ignored: $colorHex", e)
                            return
                        }

                        postToMain {
                            pixels[Pair(x, y)] = color
                        }
                    } catch (_: Exception) {
                        // Ignore malformed frames and keep the grid responsive.
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    postToMain {
                        connectionStatus = "Disconnected"
                    }
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    postToMain {
                        connectionStatus = "Disconnected"
                    }
                    webSocket.close(1000, null)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    postToMain {
                        connectionStatus = "Disconnected"
                    }
                }
            },
        )

        onDispose {
            websocket?.close(1000, "Leaving live updates screen")
            connectionStatus = "Disconnected"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Live Updates",
            style = MaterialTheme.typography.headlineSmall,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Connection: $connectionStatus",
            style = MaterialTheme.typography.bodyMedium,
            color = when (connectionStatus) {
                "Connected" -> Color(0xFF2E7D32)
                "Connecting" -> Color(0xFFB7791F)
                else -> Color(0xFFB3261E)
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        PixelGridBoard(pixels)
    }
}

@Composable
private fun PixelGridBoard(pixels: Map<Pair<Int, Int>, Color>) {
    Column(
        modifier = Modifier
            .background(Color.White)
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(6.dp))
            .padding(4.dp),
    ) {
        for (y in 0 until GRID_SIZE) {
            Row {
                for (x in 0 until GRID_SIZE) {
                    val pixelColor = pixels[Pair(x, y)] ?: Color.White
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(pixelColor)
                            .border(0.5.dp, Color(0xFFE8E8E8)),
                    )
                }
            }
        }
    }
}

private fun postToMain(action: () -> Unit) {
    Handler(Looper.getMainLooper()).post(action)
}
