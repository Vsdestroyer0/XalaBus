package com.example.xalabus.core.util

/**
 * ErrorMapper — Traductor central de excepciones a mensajes en español.
 *
 * Todos los ViewModels del proyecto deben llamar a [toUserMessage] en sus
 * bloques catch(e: Exception) en lugar de exponer e.message directamente.
 *
 * Fuentes de error cubiertas:
 *   - Supabase Auth (credenciales, sesión, email, OTP)
 *   - Supabase PostgREST (permisos, duplicados, FK, red)
 *   - Errores de conectividad (sin internet)
 *   - Errores genéricos de KMP/Kotlin
 */
object ErrorMapper {

    /**
     * Convierte cualquier excepción en un mensaje legible para el usuario.
     *
     * @param e  La excepción capturada en el bloque catch.
     * @param context  Contexto opcional (ej: "al iniciar sesión") para mensajes genéricos.
     * @return  Cadena en español lista para mostrarse en la UI.
     */
    fun toUserMessage(e: Exception, context: String = ""): String {
        val raw = e.message?.lowercase() ?: ""
        val suffix = if (context.isNotBlank()) " $context" else ""

        return when {

            // ── AUTENTICACIÓN ─────────────────────────────────────────────────

            // Contraseña incorrecta
            raw.contains("invalid login credentials") ||
            raw.contains("invalid_credentials") ||
            raw.contains("wrong password") ||
            raw.contains("incorrect password") ->
                "Contraseña incorrecta. Verifica tus datos."

            // Correo no registrado
            raw.contains("user not found") ||
            raw.contains("no user found") ||
            raw.contains("email not found") ->
                "No existe una cuenta con ese correo."

            // Correo no confirmado
            raw.contains("email not confirmed") ||
            raw.contains("email_not_confirmed") ->
                "Debes confirmar tu correo antes de iniciar sesión. Revisa tu bandeja de entrada."

            // Correo ya registrado
            raw.contains("user already registered") ||
            raw.contains("already been registered") ||
            raw.contains("email already") ->
                "Este correo ya tiene una cuenta registrada. Intenta iniciar sesión."

            // Contraseña muy corta / débil
            raw.contains("password should be at least") ||
            raw.contains("password is too short") ||
            raw.contains("weak_password") ->
                "La contraseña debe tener al menos 6 caracteres."

            // Correo inválido
            raw.contains("invalid email") ||
            raw.contains("unable to validate email") ->
                "El formato del correo no es válido."

            // OTP / código de verificación inválido
            raw.contains("token has expired") ||
            raw.contains("otp expired") ||
            raw.contains("token_expired") ->
                "El código de verificación ha expirado. Solicita uno nuevo."

            raw.contains("invalid otp") ||
            raw.contains("otp_invalid") ||
            raw.contains("token is invalid") ||
            raw.contains("invalid token") ->
                "El código ingresado no es válido. Verifica e intenta de nuevo."

            // Demasiadas solicitudes (rate limit)
            raw.contains("rate limit") ||
            raw.contains("too many requests") ||
            raw.contains("429") ->
                "Demasiados intentos. Espera unos minutos e intenta de nuevo."

            // Sesión expirada
            raw.contains("session_not_found") ||
            raw.contains("session not found") ||
            raw.contains("refresh_token_not_found") ->
                "Tu sesión ha expirado. Inicia sesión nuevamente."

            // No autenticado
            raw.contains("not authenticated") ||
            raw.contains("jwt expired") ||
            raw.contains("invalid jwt") ->
                "No estás autenticado. Inicia sesión para continuar."

            // ── BASE DE DATOS / POSTGREST ──────────────────────────────────────

            // Sin permisos (RLS)
            raw.contains("permission denied") ||
            raw.contains("42501") ||
            raw.contains("row-level security") ->
                "No tienes permisos para realizar esta acción."

            // Registro duplicado
            raw.contains("duplicate key") ||
            raw.contains("unique_violation") ||
            raw.contains("23505") ->
                "Este registro ya existe en el sistema."

            // Violación de clave foránea
            raw.contains("foreign key") ||
            raw.contains("23503") ->
                "Error de datos: referencia inválida. Contacta al administrador."

            // Tabla o columna no encontrada
            raw.contains("relation") && raw.contains("does not exist") ||
            raw.contains("42p01") ->
                "Error interno: recurso no encontrado. Contacta al administrador."

            // ── CONECTIVIDAD / RED ─────────────────────────────────────────────

            raw.contains("unable to resolve host") ||
            raw.contains("network is unreachable") ||
            raw.contains("no route to host") ||
            raw.contains("connection refused") ||
            raw.contains("failed to connect") ||
            raw.contains("sockettimeout") ||
            raw.contains("connection reset") ||
            raw.contains("nodataexception") ->
                "Sin conexión a internet. Verifica tu red e intenta de nuevo."

            raw.contains("timeout") ||
            raw.contains("timed out") ->
                "La solicitud tardó demasiado. Verifica tu conexión e intenta de nuevo."

            // ── ALMACENAMIENTO (Supabase Storage) ─────────────────────────────

            raw.contains("payload too large") ||
            raw.contains("file too large") ||
            raw.contains("413") ->
                "El archivo es demasiado grande para subirse."

            raw.contains("invalid mime type") ||
            raw.contains("unsupported media") ->
                "Tipo de archivo no soportado. Usa una imagen JPG o PNG."

            // ── FALLBACK GENÉRICO ──────────────────────────────────────────────
            else ->
                "Ocurrió un error inesperado$suffix. Intenta de nuevo."
        }
    }
}
