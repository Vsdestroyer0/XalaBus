package com.example.xalabus.data.favoritos

import com.example.xalabus.data.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder

/**
 * Repositorio de favoritos — CU-10.
 * Opera sobre la tabla `favoritos` en Supabase.
 */
class FavoritosRepository {
    private val client = SupabaseClientProvider.client

    /** Agrega una ruta a los favoritos del usuario. */
    suspend fun addFavorito(favorito: Favorito) {
        client.postgrest["favoritos"].insert(favorito)
    }

    /** Elimina un favorito por userId + routeId. */
    suspend fun removeFavorito(userId: String, routeId: String) {
        client.postgrest["favoritos"].delete {
            filter {
                eq("user_id", userId)
                eq("route_id", routeId)
            }
        }
    }

    /** Obtiene todos los favoritos del usuario. */
    suspend fun getFavoritosByUser(userId: String): List<Favorito> {
        return client.postgrest["favoritos"]
            .select {
                filter { eq("user_id", userId) }
            }
            .decodeList<Favorito>()
    }

    /** Verifica si una ruta ya está en favoritos del usuario. */
    suspend fun isFavorito(userId: String, routeId: String): Boolean {
        val results = client.postgrest["favoritos"]
            .select {
                filter {
                    eq("user_id", userId)
                    eq("route_id", routeId)
                }
            }
            .decodeList<Favorito>()
        return results.isNotEmpty()
    }
}
