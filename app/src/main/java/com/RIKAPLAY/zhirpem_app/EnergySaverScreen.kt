package com.RIKAPLAY.zhirpem_app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnergySaverScreen(
    onBack: () -> Unit,
    viewModel: EnergySaverViewModel = viewModel()
) {
    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val autoEnabled by viewModel.autoEnergySaverEnabled.collectAsState(initial = true)
    val threshold by viewModel.batteryThreshold.collectAsState(initial = 20)
    val isActive by viewModel.isEnergySaverActive.collectAsState(initial = false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Энергосбережение", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Блок статистики
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isActive) Icons.Default.Bolt else Icons.Default.BatteryChargingFull,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = if (isActive) "Режим энергосбережения активен" else "Обычный режим потребления",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Текущий заряд: $batteryLevel%",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Мастер-тумблер
            Text("Автоматизация", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Автоэнергосбережение", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text("Автоматическое управление питанием", fontSize = 12.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = autoEnabled,
                            onCheckedChange = { viewModel.toggleAutoEnergySaver(it) }
                        )
                    }

                    if (autoEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Включать при заряде ≤ $threshold%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Slider(
                            value = threshold.toFloat(),
                            onValueChange = { viewModel.setThreshold(it.toInt()) },
                            valueRange = 5f..50f,
                            steps = 8, // 5, 10, 15, 20, 25, 30, 35, 40, 45, 50
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Список опций
            Text("Параметры экономии", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    EnergyOptionItem(
                        title = "Отключение эффекта стекла",
                        subtitle = "Убирает размытие для экономии GPU",
                        checked = true, // В режиме энергосбережения это всегда включено, если режим активен
                        enabled = false // Эти опции фиксированы для режима
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    EnergyOptionItem(
                        title = "Переключение на тёмную тему",
                        subtitle = "Экономит заряд на OLED-экранах",
                        checked = true,
                        enabled = false
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    EnergyOptionItem(
                        title = "Отключение анимаций",
                        subtitle = "Упрощает интерфейс для снижения нагрузки",
                        checked = true,
                        enabled = false
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    EnergyOptionItem(
                        title = "Отключение персонализации",
                        subtitle = "Использование стандартных цветов",
                        checked = true,
                        enabled = false
                    )
                }
            }
            
            // Ручной переключатель
            Button(
                onClick = { viewModel.manualToggleEnergySaver(!isActive) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isActive) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (isActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(if (isActive) Icons.Default.Settings else Icons.Default.Bolt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isActive) "Выключить принудительно" else "Включить сейчас",
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun EnergyOptionItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, fontSize = 12.sp, color = Color.Gray)
        }
        Switch(
            checked = checked,
            onCheckedChange = if (enabled) onCheckedChange else null,
            enabled = enabled
        )
    }
}
