package com.RIKAPLAY.zhirpem_app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePollView(
    pollData: PollData,
    onPollDataChange: (PollData) -> Unit,
    onClosePoll: () -> Unit // Кнопка отмены опроса
) {
    // Локальный изменяемый список вариантов (минимум 2 по умолчанию)
    var optionsList by remember { mutableStateOf(pollData.options.ifEmpty { listOf("", "") }) }

    // Обновляем внешнее состояние при любых изменениях внутри формы
    LaunchedEffect(optionsList, pollData.question, pollData.multipleChoice) {
        onPollDataChange(
            pollData.copy(
                options = optionsList.filter { it.isNotBlank() }
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // Заголовок формы с кнопкой удаления опроса
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Создание опроса",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary // Поддерживает зеленый / Material You
            )
            IconButton(onClick = onClosePoll, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Clear, contentDescription = "Удалить опрос", tint = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 1. ПОЛЕ ДЛЯ ВВОДА ВОПРОСА
        TextField(
            value = pollData.question,
            onValueChange = { onPollDataChange(pollData.copy(question = it)) },
            placeholder = { Text("Вопрос", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary, // Наша зеленая линия активности
                unfocusedIndicatorColor = Color.Gray.copy(alpha = 0.5f)
            ),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 2. ДИНАМИЧЕСКИЙ СПИСОК ВАРИАНТОВ ОТВЕТА
        optionsList.forEachIndexed { index, optionText ->
            TextField(
                value = optionText,
                onValueChange = { newValue ->
                    val newList = optionsList.toMutableList()
                    newList[index] = newValue
                    optionsList = newList
                },
                placeholder = { Text("Вариант ${index + 1}", color = Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }

        // 3. КНОПКА ДЛЯ ДОБАВЛЕНИЯ НОВОГО ВАРИАНТА (Максимум 10, как в ТГ)
        if (optionsList.size < 10) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .border(
                        1.dp, 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), 
                        RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        optionsList = optionsList + "" // Добавляем пустую строку в список вариантов
                    }
                    .padding(14.dp)
            ) {
                Text(
                    text = "Добавить вариант...", 
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4. НАСТРОЙКА: НЕСКОЛЬКО ВАРИАНТОВ ОТВЕТА (Чекбокс внизу формы)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPollDataChange(pollData.copy(multipleChoice = !pollData.multipleChoice)) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = pollData.multipleChoice,
                onCheckedChange = { onPollDataChange(pollData.copy(multipleChoice = it)) },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary // Красивый зеленый чекбокс по нашей теме
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Несколько вариантов ответа",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun PollView(
    poll: PollData,
    onVote: (Int) -> Unit,
    currentUserId: String
) {
    val totalVotes = poll.votes.values.flatten().distinct().size
    val hasVoted = poll.votes.values.any { it.contains(currentUserId) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = poll.question,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(12.dp))

        poll.options.forEachIndexed { index, option ->
            val optionVotes = poll.votes[index.toString()] ?: emptyList()
            val voteCount = optionVotes.size
            val percentage = if (totalVotes > 0) voteCount.toFloat() / totalVotes else 0f
            val isSelected = optionVotes.contains(currentUserId)

            val animatedProgress by animateFloatAsState(targetValue = if (hasVoted) percentage else 0f, label = "pollProgress")

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable(!hasVoted) { onVote(index) }
            ) {
                // Шкала процентов
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSelected) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = option,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    
                    if (hasVoted) {
                        Text(
                            text = "${(percentage * 100).toInt()}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$totalVotes голосов",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}
