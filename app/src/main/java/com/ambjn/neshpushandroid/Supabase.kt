package com.ambjn.neshpushandroid

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

val supabase = createSupabaseClient(
    supabaseUrl = BuildConfig.SUPABASE_URL,
    supabaseKey = BuildConfig.SUPABASE_KEY,
) {
    install(Postgrest)
}

@Serializable
data class TodoItem(val id: Int, val name: String)

@Composable
fun TodoList(modifier: Modifier = Modifier) {
    var items by remember { mutableStateOf<List<TodoItem>>(listOf()) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            // Missing table / no network must not crash the app; surface it instead.
            runCatching { supabase.from("todos").select().decodeList<TodoItem>() }
                .onSuccess { items = it }
                .onFailure { error = it.message }
        }
    }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = modifier.padding(8.dp)); return }
    LazyColumn(modifier) {
        items(items, key = { it.id }) { item ->
            Text(item.name, modifier = Modifier.padding(8.dp))
        }
    }
}
