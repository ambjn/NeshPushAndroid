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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.messaging.FirebaseMessaging

@Composable
fun HomeScreen(user: FirebaseUser, onSignOut: () -> Unit, modifier: Modifier = Modifier) {
    val clipboard = LocalClipboardManager.current
    var token by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token = it }
    }

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
        Text("FCM token", style = MaterialTheme.typography.labelLarge)
        Text(
            token ?: "Fetching…",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 4,
        )
        token?.let { t ->
            TextButton(onClick = { clipboard.setText(AnnotatedString(t)) }) { Text("Copy token") }
        }
        Spacer(Modifier.height(32.dp))
        OutlinedButton(onClick = onSignOut) { Text("Sign out") }
        Spacer(Modifier.height(32.dp))
        Text("Todos", style = MaterialTheme.typography.labelLarge)
        TodoList()
    }
}
