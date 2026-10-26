package com.ambjn.neshpushandroid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ambjn.neshpushandroid.push.DeviceRegistrar
import com.ambjn.neshpushandroid.push.Route
import com.google.firebase.auth.FirebaseUser

private val Route.icon: ImageVector
    get() = when (this) {
        Route.HOME -> Icons.Filled.Home
        Route.SCHEDULE -> Icons.Filled.DateRange
        Route.MESSAGES -> Icons.Filled.Email
        Route.ACCOUNT -> Icons.Filled.AccountCircle
        Route.STUDY_PLAN -> Icons.Filled.CheckCircle
    }

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
                Route.entries.forEach { r ->
                    NavigationBarItem(selected = selected == r, onClick = { selected = r }, icon = { Icon(r.icon, contentDescription = r.title) }, label = { Text(r.title) })
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(selected.icon, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(selected.title, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            if (selected == Route.HOME) {
                Text("Hello ${user.displayName ?: "there"}!", style = MaterialTheme.typography.bodyLarge)
                user.email?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Spacer(Modifier.height(24.dp))
                RegistrationCard(registration)
                Spacer(Modifier.height(32.dp))
                OutlinedButton(onClick = onSignOut) { Text("Sign out") }
            } else {
                Text("Opened via route \"${selected.key}\".", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun RegistrationCard(state: DeviceRegistrar.State) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            when (state) {
                DeviceRegistrar.State.Pending -> CircularProgressIndicator(Modifier.size(24.dp))
                DeviceRegistrar.State.Registered -> Icon(Icons.Filled.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                is DeviceRegistrar.State.Failed -> Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    when (state) {
                        DeviceRegistrar.State.Pending -> "Registering this device…"
                        DeviceRegistrar.State.Registered -> "Ready to receive notifications"
                        is DeviceRegistrar.State.Failed -> "Registration failed"
                    },
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    when (state) {
                        DeviceRegistrar.State.Pending -> "Sending the push token to the dashboard"
                        DeviceRegistrar.State.Registered -> "Test sends from the dashboard will arrive here"
                        is DeviceRegistrar.State.Failed -> "${state.reason}. Retries on next launch."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
