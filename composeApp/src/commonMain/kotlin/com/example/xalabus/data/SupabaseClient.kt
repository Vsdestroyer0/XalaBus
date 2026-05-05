package com.example.xalabus.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Singleton del cliente de Supabase para toda la app.
 */
expect object SupabaseConfig {
    val url: String
    val key: String
}

object SupabaseClientProvider {
    private val SUPABASE_URL = SupabaseConfig.url
    private val SUPABASE_ANON_KEY = SupabaseConfig.key

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        install(Auth)
        install(Postgrest)
    }
}
