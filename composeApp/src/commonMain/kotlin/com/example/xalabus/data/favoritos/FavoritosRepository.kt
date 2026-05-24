package com.example.xalabus.data.favoritos

import com.example.xalabus.data.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns

/**
 * CU-10: Repositorio para operaciones CRUD sobre la tabla `favoritos` en Supabase.
 */
class FavoritosRepository {
    private val client = SupabaseClientProvider.client

    /** Inserta una nueva ruta favorita para el usuario */
    suspend fun addFavorito(favorito: Favorito) {
        client.postgrest["favoritos"].insert(favorito)
    }

    /** Elimina una ruta de favoritos por userId + routeId */
    suspend fun removeFavorito(userId: String, routeId: String) {
        client.postgrest["favoritos"].delete {
            filter {
                eq("user_id", userId)
                eq("route_id", routeId)
            }
        }
    }

    /** Obtiene todos los favoritos de un usuario */
    suspend fun getFavoritosByUser(userId: String): List<Favorito> {
        return client.postgrest["favoritos"]
            .select {
                filter { eq("user_id", userId) }
            }
            .decodeList<Favorito>()
    }

    /** Verifica si una ruta ya está en favoritos para el usuario dado */
    suspend fun isFavorito(userId: String, routeId: String): Boolean {
        val result = client.postgrest["favoritos"]
            .select(Columns.list("id")) {
                filter {
                    eq("user_id", userId)
                    eq("route_id", routeId)
                }
            }
            .decodeList<Favorito>()
        return result.isNotEmpty()
    }
}
