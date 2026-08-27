package com.RIKAPLAY.zhirpem_app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class BatteryStatus(
    val level: Int,
    val isCharging: Boolean,
    val isPowerSaveMode: Boolean
)

class BatteryObserver(private val context: Context) {
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    val batteryStatus: Flow<BatteryStatus> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                // Получаем интент текущего состояния батареи (может быть null, если это POWER_SAVE_MODE_CHANGED)
                val batteryStatusIntent = if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                    intent
                } else {
                    context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                }
                
                val level = batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                val status = batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                 status == BatteryManager.BATTERY_STATUS_FULL
                
                val batteryPct = if (level != -1 && scale != -1) {
                    (level * 100 / scale.toFloat()).toInt()
                } else {
                    0
                }
                
                trySend(BatteryStatus(batteryPct, isCharging, powerManager.isPowerSaveMode))
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        context.registerReceiver(receiver, filter)

        // Начальное состояние
        trySend(BatteryStatus(0, false, powerManager.isPowerSaveMode))

        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }
}
