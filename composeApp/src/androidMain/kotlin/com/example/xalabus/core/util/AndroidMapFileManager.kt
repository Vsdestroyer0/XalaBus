package com.example.xalabus.core.util

import android.content.Context
import java.io.File

/**
 * Implementación de MapFileManager específica para Android.
 * Se movió a androidMain porque usa dependencias de Android (Context).
 */
class AndroidMapFileManager(private val context: Context) : MapFileManager {
    override fun saveMapFile(name: String, bytes: ByteArray): String {
        // Buscamos la carpeta interna de la app (invisible para el usuario, segura para nosotros)
        val file = File(context.filesDir, name)

        // Si el archivo ya existe (por una ejecución previa), no lo sobreescribimos para ahorrar tiempo
        if (!file.exists()) {
            file.writeBytes(bytes)
        }

        return file.absolutePath
    }
}
