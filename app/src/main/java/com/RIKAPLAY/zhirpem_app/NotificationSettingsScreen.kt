package com.RIKAPLAY.zhirpem_app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    viewModel: NotificationSettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки уведомлений") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Вибрация
            SettingsSection(title = "Общие") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Вибрация", fontSize = 16.sp)
                    Switch(
                        checked = settings.isVibrationEnabled,
                        onCheckedChange = { viewModel.updateVibration(it) }
                    )
                }
            }

            // 2. Категории
            SettingsSection(title = "Категории уведомлений") {
                CategoryItem(
                    label = "Сообщения чата",
                    checked = settings.enabledCategories.contains(NotificationType.CHAT_MESSAGE),
                    onToggle = { viewModel.toggleCategory(NotificationType.CHAT_MESSAGE) }
                )
                CategoryItem(
                    label = "Лайки",
                    checked = settings.enabledCategories.contains(NotificationType.LIKE),
                    onToggle = { viewModel.toggleCategory(NotificationType.LIKE) }
                )
                CategoryItem(
                    label = "Комментарии",
                    checked = settings.enabledCategories.contains(NotificationType.COMMENT),
                    onToggle = { viewModel.toggleCategory(NotificationType.COMMENT) }
                )
                CategoryItem(
                    label = "Новые читатели",
                    checked = settings.enabledCategories.contains(NotificationType.FOLLOW),
                    onToggle = { viewModel.toggleCategory(NotificationType.FOLLOW) }
                )
            }

            // 3. Фильтр отправителей
            SettingsSection(title = "От кого получать уведомления") {
                val filters = listOf(
                    NotificationSenderFilter.ALL to "Все",
                    NotificationSenderFilter.FOLLOWING to "Читаемые",
                    NotificationSenderFilter.DIRECT_CHAT_ONLY to "Знакомые (с личным чатом)",
                    NotificationSenderFilter.NONE to "Никто"
                )

                Column(Modifier.selectableGroup()) {
                    filters.forEach { (filter, label) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .selectable(
                                    selected = (settings.senderFilter == filter),
                                    onClick = { viewModel.updateSenderFilter(filter) },
                                    role = Role.RadioButton
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (settings.senderFilter == filter),
                                onClick = null
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
        HorizontalDivider(modifier = Modifier.padding(top = 16.dp), thickness = 0.5.dp)
    }
}

@Composable
fun CategoryItem(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 16.sp)
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
    }
}
