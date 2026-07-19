package com.RIKAPLAY.zhirpem_app

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 1. МОДЕЛЬ ДАННЫХ
data class StorageCategory(
    val id: String,
    val name: String,
    val sizeBytes: Long,
    val percentage: Float,
    val color: Color
)

// 2. КОМПОНЕНТ ДИАГРАММЫ
@Composable
fun StoragePieChart(
    categories: List<StorageCategory>,
    totalSize: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(200.dp)) {
            var startAngle = -90f
            categories.forEach { category ->
                val sweepAngle = category.percentage * 360f
                drawArc(
                    color = category.color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 30.dp.toPx())
                )
                startAngle += sweepAngle
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Всего", fontSize = 14.sp, color = Color.Gray)
            Text(text = totalSize, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptimizationScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val totalSizeBytes = remember { OptimizationManager.getCacheSize(context) }
    var cacheSizeText by remember { mutableStateOf(OptimizationManager.formatSize(totalSizeBytes)) }
    var isOptimizing by remember { mutableStateOf(false) }

    // Разделение кэша на категории (имитация для наглядности)
    val categories = remember(totalSizeBytes) {
        listOf(
            StorageCategory("photo", "Фото", (totalSizeBytes * 0.4).toLong(), 0.4f, Color(0xFF2196F3)),
            StorageCategory("video", "Видео", (totalSizeBytes * 0.3).toLong(), 0.3f, Color(0xFFE91E63)),
            StorageCategory("text", "Текст", (totalSizeBytes * 0.2).toLong(), 0.2f, Color(0xFF4CAF50)),
            StorageCategory("other", "Другое", (totalSizeBytes * 0.1).toLong(), 0.1f, Color(0xFFFFC107))
        )
    }

    val selectedCategoriesMap = remember { 
        mutableStateMapOf<String, Boolean>().apply {
            categories.forEach { put(it.id, true) }
        }
    }

    val selectedCount = selectedCategoriesMap.values.count { it }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Оптимизация", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Круговая диаграмма
            StoragePieChart(
                categories = categories,
                totalSize = cacheSizeText,
                modifier = Modifier.padding(vertical = 24.dp)
            )

            // Список категорий
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(categories) { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .clickable { selectedCategoriesMap[category.id] = !(selectedCategoriesMap[category.id] ?: false) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(12.dp).background(category.color, CircleShape))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = category.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(text = OptimizationManager.formatSize(category.sizeBytes), fontSize = 12.sp, color = Color.Gray)
                        }
                        Checkbox(
                            checked = selectedCategoriesMap[category.id] ?: false,
                            onCheckedChange = { selectedCategoriesMap[category.id] = it }
                        )
                    }
                }
            }

            // Анимация сканирования
            AnimatedVisibility(
                visible = isOptimizing,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    ScanningLoader()
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Кнопка очистки
            Button(
                onClick = {
                    if (!isOptimizing && selectedCount > 0) {
                        isOptimizing = true
                        scope.launch {
                            val randomDelayTime = kotlin.random.Random.nextLong(1000, 5001)
                            delay(randomDelayTime)
                            
                            val selectedIds = selectedCategoriesMap.filter { it.value }.keys.toList()
                            val success = OptimizationManager.clearSelectedCache(context, selectedIds)
                            
                            isOptimizing = false
                            if (success) {
                                cacheSizeText = OptimizationManager.getCacheSizeFormatted(context)
                                Toast.makeText(context, "Очищено элементов: $selectedCount", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .bounceClick(),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isOptimizing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                ),
                enabled = !isOptimizing && selectedCount > 0
            ) {
                AnimatedContent(targetState = isOptimizing, label = "buttonContent") { optimizing ->
                    if (optimizing) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Уничтожение...", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Очистить выбранное ($selectedCount)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
