package com.RIKAPLAY.zhirpem_app

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Адаптивное ограничение размера изображения для горизонтальной ориентации.
 * В Landscape режиме ограничивает высоту, чтобы оставить место для интерфейса.
 */
fun Modifier.adaptiveImageSize(isLandscape: Boolean): Modifier {
    return if (isLandscape) {
        this
            .heightIn(max = 220.dp)
            .fillMaxHeight(0.5f)
    } else {
        this
    }
}
