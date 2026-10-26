package com.ambjn.neshpushandroid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ambjn.neshpushandroid.push.DeviceRegistrar
import com.google.firebase.auth.FirebaseUser

@Composable
fun HomeScreen(
    user: FirebaseUser,
    registration: DeviceRegistrar.State,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Hello ${user.displayName ?: "there"}!", style = MaterialTheme.typography.headlineMedium)
        user.email?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(32.dp))
        Text(
            when (registration) {
                DeviceRegistrar.State.Registered -> "Push registered"
                DeviceRegistrar.State.Failed -> "Push registration failed; will retry on next launch"
                DeviceRegistrar.State.Pending -> "Registering push…"
            },
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(32.dp))
        OutlinedButton(onClick = onSignOut) { Text("Sign out") }
    }
}
