package com.ambjn.neshpushandroid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ambjn.neshpushandroid.push.DeviceRegistrar
import com.ambjn.neshpushandroid.push.Route
import com.google.firebase.auth.FirebaseUser

/** Five tabs, one per allowlisted route. A notification tap selects the tab named by the payload. */
@Composable
fun HomeScreen(
    user: FirebaseUser,
    registration: DeviceRegistrar.State,
    pendingNavigation: MutableState<Route?>,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by remember { mutableStateOf(Route.HOME) }
    val requested = pendingNavigation.value
    LaunchedEffect(requested) {
        if (requested != null) {
            selected = requested
            pendingNavigation.value = null
        }
    }
    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                Route.entries.forEach { r -> NavigationBarItem(selected = selected == r, onClick = { selected = r }, icon = {}, label = { Text(r.title) }) }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(selected.title, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            when (selected) {
                Route.HOME -> {
                    Text("Hello ${user.displayName ?: "there"}!", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        when (registration) {
                            DeviceRegistrar.State.Registered -> "Ready to receive notifications"
                            DeviceRegistrar.State.Failed -> "Registration failed; will retry on next launch"
                            DeviceRegistrar.State.Pending -> "Registering…"
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(32.dp))
                    OutlinedButton(onClick = onSignOut) { Text("Sign out") }
                }
                else -> Text("Opened via route \"${selected.key}\".", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
