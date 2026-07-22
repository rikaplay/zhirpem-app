package com.RIKAPLAY.zhirpem_app

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

// ==========================================
// 1. МОНИТОРИНГ СЕТИ
// ==========================================
class NetworkMonitor(private val context: Context) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun isConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

// ==========================================
// 2. МОДЕЛИ ИГРЫ
// ==========================================
data class Obstacle(
    var x: Float,
    val width: Float = 40f,
    val height: Float = 60f
)

// ==========================================
// 3. КОМПОНЕНТ ИГРЫ
// ==========================================
@Composable
fun PixelDinoGame(
    isHardcore: Boolean,
    onGameOver: (Int) -> Unit,
    highScore: Int
) {
    val smartphonePainter = rememberVectorPainter(Icons.Default.Smartphone)
    val serverPainter = rememberVectorPainter(Icons.Default.Storage)
    
    var playerY by remember { mutableStateOf(0f) }
    var playerVelocity by remember { mutableStateOf(0f) }
    var obstacles by remember { mutableStateOf(listOf<Obstacle>()) }
    var score by remember { mutableIntStateOf(0) }
    var gameSpeed by remember { mutableStateOf(5f) }
    var isJumping by remember { mutableStateOf(false) }
    var frames by remember { mutableIntStateOf(0) }

    val gravity = 0.8f
    val jumpStrength = -15f
    val speedIncrement = if (isHardcore) 0.005f * 2.5f else 0.005f

    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameMillis {
                frames++
                
                // Физика игрока
                playerVelocity += gravity
                playerY += playerVelocity
                if (playerY > 0) {
                    playerY = 0f
                    playerVelocity = 0f
                    isJumping = false
                }

                // Движение препятствий
                val newObstacles = obstacles.map { it.copy(x = it.x - gameSpeed) }.filter { it.x > -100 }
                obstacles = newObstacles

                // Спавн препятствий
                if (frames % 100 == 0 || (obstacles.isEmpty() && frames > 50)) {
                    if (Random.nextFloat() > 0.7f || obstacles.isEmpty()) {
                        obstacles = obstacles + Obstacle(x = 1000f)
                    }
                }

                // Коллизии
                val playerRect = Rect(50f, 200f + playerY - 40f, 50f + 40f, 200f + playerY)
                obstacles.forEach { obs ->
                    val obsRect = Rect(obs.x, 200f - obs.height, obs.x + obs.width, 200f)
                    if (playerRect.overlaps(obsRect)) {
                        onGameOver(score)
                    }
                }

                // Счет и ускорение
                score++
                gameSpeed += speedIncrement
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) {
                if (!isJumping) {
                    playerVelocity = jumpStrength
                    isJumping = true
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val groundY = 200.dp.toPx()
            val playerX = 50.dp.toPx()
            val playerSize = 40.dp.toPx()
            
            // Земля
            drawLine(
                color = Color.Gray,
                start = Offset(0f, groundY),
                end = Offset(size.width, groundY),
                strokeWidth = 2.dp.toPx()
            )

            // Игрок (Смартфон)
            translate(left = playerX, top = groundY + playerY.dp.toPx() - playerSize) {
                with(smartphonePainter) {
                    draw(size = Size(playerSize, playerSize))
                }
            }

            // Препятствия (Сервера)
            obstacles.forEach { obs ->
                translate(left = obs.x.dp.toPx(), top = groundY - obs.height.dp.toPx()) {
                    with(serverPainter) {
                        draw(size = Size(obs.width.dp.toPx(), obs.height.dp.toPx()))
                    }
                }
            }
        }
        
        // Счет
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text("Счет: $score", fontWeight = FontWeight.Bold, color = Color.Gray)
            Text("Лучший: $highScore", fontSize = 12.sp, color = Color.Gray.copy(alpha = 0.7f))
        }
    }
}

private data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun overlaps(other: Rect): Boolean {
        return left < other.right && right > other.left && top < other.bottom && bottom > other.top
    }
}

// ==========================================
// 4. ГЛАВНЫЙ ЭКРАН ОЖИДАНИЯ СЕТИ
// ==========================================
@Composable
fun NetworkGameScreen(onConnected: () -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("dino_game_prefs", Context.MODE_PRIVATE) }
    val networkMonitor = remember { NetworkMonitor(context) }
    
    var gameState by remember { mutableStateOf("MENU") } // MENU, PLAYING, GAMEOVER, SETTINGS, LEADERBOARD
    var highScore by remember { mutableIntStateOf(sharedPrefs.getInt("high_score", 0)) }
    var isHardcore by remember { mutableStateOf(sharedPrefs.getBoolean("hardcore_mode", false)) }
    var lastScore by remember { mutableIntStateOf(0) }

    // Проверка сети каждые 3 секунды
    LaunchedEffect(Unit) {
        while (isActive) {
            if (networkMonitor.isConnected()) {
                onConnected()
            }
            delay(3000L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Ожидание сети",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            when (gameState) {
                "MENU" -> {
                    GameButton("Играть", Icons.Default.PlayArrow) { gameState = "PLAYING" }
                    Spacer(modifier = Modifier.height(12.dp))
                    GameButton("Таблица рекордов", Icons.Default.Leaderboard) { gameState = "LEADERBOARD" }
                    Spacer(modifier = Modifier.height(12.dp))
                    GameButton("Настройки", Icons.Default.Settings) { gameState = "SETTINGS" }
                }
                "PLAYING" -> {
                    PixelDinoGame(
                        isHardcore = isHardcore,
                        highScore = highScore,
                        onGameOver = { score ->
                            lastScore = score
                            if (score > highScore) {
                                highScore = score
                                sharedPrefs.edit().putInt("high_score", score).apply()
                            }
                            gameState = "GAMEOVER"
                        }
                    )
                }
                "GAMEOVER" -> {
                    Text("ИГРА ОКОНЧЕНА", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                    Text("Ваш счет: $lastScore", fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    GameButton("Попробовать снова", Icons.Default.PlayArrow) { gameState = "PLAYING" }
                    Spacer(modifier = Modifier.height(12.dp))
                    GameButton("В меню", null) { gameState = "MENU" }
                }
                "SETTINGS" -> {
                    Text("Настройки", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("Hardcore режим (x2.5 ускорение)")
                        Switch(
                            checked = isHardcore,
                            onCheckedChange = {
                                isHardcore = it
                                sharedPrefs.edit().putBoolean("hardcore_mode", it).apply()
                            }
                        )
                    }
                    GameButton("Назад", null) { gameState = "MENU" }
                }
                "LEADERBOARD" -> {
                    Text("Рекорды", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Ваш лучший результат: $highScore", modifier = Modifier.padding(16.dp))
                    // Можно добавить список из Firebase, но пользователь просил "сверху мини счет за эту игру и за лучшую"
                    // В ТЗ сказано про таблицу рекордов в меню, так что оставим заглушку или просто свой рекорд.
                    GameButton("Назад", null) { gameState = "MENU" }
                }
            }
        }
    }
}

@Composable
fun GameButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector?, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(0.7f)
            .height(56.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text, fontWeight = FontWeight.Bold)
    }
}

// ==========================================
// 5. ОБЕРТКА ДЛЯ ИНТЕГРАЦИИ В MAIN
// ==========================================
@Composable
fun NetworkStabilityWrapper(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val networkMonitor = remember { NetworkMonitor(context) }
    
    var isConnected by remember { mutableStateOf(networkMonitor.isConnected()) }
    var showGame by remember { mutableStateOf(false) }
    var offlineTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (isActive) {
            val currentStatus = networkMonitor.isConnected()
            if (currentStatus) {
                isConnected = true
                showGame = false
                offlineTime = 0L
            } else {
                if (isConnected) {
                    // Только что отключились
                    isConnected = false
                    offlineTime = System.currentTimeMillis()
                } else {
                    // Уже оффлайн, проверяем сколько времени
                    if (!showGame && System.currentTimeMillis() - offlineTime > 10000) {
                        showGame = true
                    }
                }
            }
            delay(1000L)
        }
    }

    if (showGame) {
        NetworkGameScreen(onConnected = {
            isConnected = true
            showGame = false
        })
    } else {
        content()
    }
}
