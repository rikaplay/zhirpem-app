package com.RIKAPLAY.zhirpem_app

import android.content.Context
import java.io.File

object OptimizationManager {

    fun getCacheSize(context: Context): Long {
        return getFolderSize(context.cacheDir)
    }

    fun getCacheSizeFormatted(context: Context): String {
        return formatSize(getCacheSize(context))
    }

    fun clearAppCache(context: Context): String {
        deleteRecursive(context.cacheDir)
        return getCacheSizeFormatted(context)
    }

    fun clearSelectedCache(context: Context, categories: List<String>): Boolean {
        if (categories.isNotEmpty()) {
            deleteRecursive(context.cacheDir)
            return true
        }
        return false
    }

    private fun getFolderSize(file: File): Long {
        var size: Long = 0
        if (file.isDirectory) {
            file.listFiles()?.forEach {
                size += getFolderSize(it)
            }
        } else {
            size = file.length()
        }
        return size
    }

    private fun deleteRecursive(fileOrDirectory: File) {
        if (fileOrDirectory.isDirectory) {
            fileOrDirectory.listFiles()?.forEach {
                deleteRecursive(it)
            }
        }
        if (fileOrDirectory.name != "cache") {
            fileOrDirectory.delete()
        }
    }

    fun formatSize(size: Long): String {
        if (size <= 0) return "0 Б"
        val units = listOf("Б", "КБ", "МБ", "ГБ")
        var s = size.toDouble()
        var unitIndex = 0
        while (s >= 1024 && unitIndex < units.size - 1) {
            s /= 1024
            unitIndex++
        }
        return "%.2f %s".format(s, units[unitIndex])
    }
}
