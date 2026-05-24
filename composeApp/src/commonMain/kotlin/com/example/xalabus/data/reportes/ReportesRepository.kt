package com.example.xalabus.data.reportes

import com.example.xalabus.data.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest

/**
 * CU-13: Repositorio para operaciones sobre la tabla `reportes` en Supabase.
 *
 * Nota sobre fotos: la subida de imágenes requiere supabase-storage.
 * Si la dependencia está disponible en el proyecto, se puede habilitar
 * el método [uploadPhoto]. Por ahora se acepta [fotoUrl] como campo opcional nulo.
 */
class ReportesRepository {
    private val client = SupabaseClientProvider.client

    /**
     * Inserta un nuevo reporte en Supabase.
     * [fotoUrl] puede ser null si el usuario no adjuntó foto.
     */
    suspend fun insertReporte(reporte: Reporte) {
        client.postgrest["reportes"].insert(reporte)
    }

    /**
     * Carga todos los reportes en estado "pendiente" (para mostrar en el mapa).
     * Postcondición (CU-13): reportes visibles para otros usuarios.
     */
    suspend fun getReportesPendientes(): List<Reporte> {
        return client.postgrest["reportes"]
            .select {
                filter { eq("estado", "pendiente") }
            }
            .decodeList<Reporte>()
    }
}
