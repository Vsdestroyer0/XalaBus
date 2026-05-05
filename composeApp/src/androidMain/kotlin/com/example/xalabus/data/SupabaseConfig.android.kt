package com.example.xalabus.data

import com.example.xalabus.BuildConfig

actual object SupabaseConfig {
    actual val url: String = BuildConfig.SUPABASE_URL
    actual val key: String = BuildConfig.SUPABASE_KEY
}
