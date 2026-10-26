package com.ambjn.neshpushandroid

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ambjn.neshpushandroid.auth.AuthViewModel
import com.ambjn.neshpushandroid.auth.SignInScreen
import com.ambjn.neshpushandroid.push.DeviceRegistrar
import com.ambjn.neshpushandroid.push.NotificationsService
import com.ambjn.neshpushandroid.push.PushMessage
import com.ambjn.neshpushandroid.push.Route
import com.ambjn.neshpushandroid.ui.theme.NerdPushTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    /** Route requested by a notification tap; consumed once by HomeScreen. */
    private val pendingNavigation = mutableStateOf<Route?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationsService.ensureChannels(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        handleIncomingIntent(intent)
        setContent {
            NerdPushTheme {
                val vm: AuthViewModel = viewModel()
                val user by vm.user.collectAsState()
                val loading by vm.loading.collectAsState()
                val error by vm.error.collectAsState()
                val registration by DeviceRegistrar.state.collectAsState()
                val context = LocalContext.current
                LaunchedEffect(user?.uid) { if (user != null) DeviceRegistrar.register() }
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val u = user
                    if (u == null) {
                        SignInScreen(loading = loading, error = error, onSignIn = { vm.signInWithGoogle(context) }, modifier = Modifier.padding(innerPadding))
                    } else {
                        HomeScreen(user = u, registration = registration, pendingNavigation = pendingNavigation, onSignOut = { vm.signOut(context) }, modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }

    /** Background tap (FCM-launched) and foreground tap (our PendingIntent) both land here. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val push = PushMessage.fromBundle(intent?.extras) ?: return
        PushMessage.KEYS.forEach { intent?.removeExtra(it) } // consume once; config changes never re-route
        if (!push.isNerdPush) return // foreign event_source or non-allowlisted route: ignore, never execute
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (push.targetUserId != null && uid != null && push.targetUserId != uid) return // not for this account
        pendingNavigation.value = push.route
    }
}
