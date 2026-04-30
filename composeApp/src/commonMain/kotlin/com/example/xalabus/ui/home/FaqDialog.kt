package com.example.xalabus.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class FaqItem(val question: String, val answer: String)

val faqList = listOf(
    FaqItem(
        question = "¿Cómo busco una ruta?",
        answer = "Puedes usar la barra de búsqueda en la pantalla principal ingresando el nombre de la ruta o de la colonia."
    ),
    FaqItem(
        question = "¿Puedo ver el trayecto de un camión en el mapa?",
        answer = "Sí, al seleccionar una ruta en la lista, se abrirá el mapa mostrando el trazado exacto del recorrido."
    ),
    FaqItem(
        question = "¿La información de las rutas es en tiempo real?",
        answer = "Por el momento mostramos las rutas y trayectos establecidos. Los tiempos de llegada pueden variar por el tráfico."
    ),
    FaqItem(
        question = "¿Funciona sin conexión a internet?",
        answer = "Si, Por el momento es 100% funcional sin internet."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaqDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Preguntas Frecuentes",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(faqList) { faq ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = faq.question,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = faq.answer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}
