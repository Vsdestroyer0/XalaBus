package com.example.xalabus.core.util

interface MapFileManager {
    /**
     * Guarda los bytes del mapa en el almacenamiento interno del dispositivo.
     * Retorna la ruta absoluta del archivo guardado.
     */
    fun saveMapFile(name: String, bytes: ByteArray): String
}