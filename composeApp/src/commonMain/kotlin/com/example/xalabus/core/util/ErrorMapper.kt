package com.example.xalabus.core.util

/**
 * Convierte excepciones a mensajes amigables para el usuario.
 * Centraliza el manejo de errores de Supabase, red y autenticación.
 */
object ErrorMapper {

    /**
     * Convierte una excepción a un mensaje legible.
     * @param e la excepción capturada
     * @param context contexto opcional para el mensaje (ej. "al cargar favoritos")
     */
    fun toUserMessage(e: Exception, context: String = ""): String {
        val base = when {
            e.message == null -> "Error desconocido"
            e.message!!.contains("network", ignoreCase = true) ||
            e.message!!.contains("unable to resolve", ignoreCase = true) ||
            e.message!!.contains("SocketTimeoutException", ignoreCase = true) ->
                "Sin conexión a internet. Verifica tu red."
            e.message!!.contains("401") ||
            e.message!!.contains("unauthorized", ignoreCase = true) ->
                "Sesión expirada. Inicia sesión de nuevo."
            e.message!!.contains("403") ||
            e.message!!.contains("forbidden", ignoreCase = true) ->
                "No tienes permiso para realizar esta acción."
            e.message!!.contains("404") ->
                "El recurso solicitado no fue encontrado."
            e.message!!.contains("duplicate", ignoreCase = true) ||
            e.message!!.contains("unique", ignoreCase = true) ->
                "El registro ya existe en el sistema."
            e.message!!.contains("timeout", ignoreCase = true) ->
                "La operación tardó demasiado. Intenta de nuevo."
            e.message!!.contains("Invalid login", ignoreCase = true) ||
            e.message!!.contains("invalid_credentials", ignoreCase = true) ->
                "Correo o contraseña incorrectos."
            e.message!!.contains("Email not confirmed", ignoreCase = true) ->
                "Confirma tu correo electrónico antes de iniciar sesión."
            e.message!!.contains("User already registered", ignoreCase = true) ->
                "Este correo ya está registrado."
            else -> "Ocurrió un error inesperado."
        }
        return if (context.isNotEmpty()) "$base ($context)" else base
    }

    /**
     * Variante sin contexto para uso rápido.
     */
    fun toUserMessage(e: Exception): String = toUserMessage(e, "")
}
