package com.example.xalabus.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Singleton del cliente de Supabase para toda la app.
 *
 * ⚠️  IMPORTANTE: Reemplaza SUPABASE_URL y SUPABASE_ANON_KEY con los valores
 *     reales de tu proyecto en https://supabase.com/dashboard → Project Settings → API
 */
object SupabaseClientProvider {

    private const val SUPABASE_URL = "https://TU_PROYECTO.supabase.co"
    private const val SUPABASE_ANON_KEY = "TU_ANON_KEY_AQUI"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        install(Auth)
        install(Postgrest)
    }
}
