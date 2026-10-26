package com.ambjn.neshpushandroid

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ambjn.neshpushandroid.auth.AuthViewModel
import com.ambjn.neshpushandroid.auth.SignInScreen
import com.ambjn.neshpushandroid.push.DeviceRegistrar
import com.ambjn.neshpushandroid.push.PushService
import com.ambjn.neshpushandroid.push.Routes
import com.ambjn.neshpushandroid.push.TapReporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import com.ambjn.neshpushandroid.ui.theme.NeshPushAndroidTheme

class MainActivity : ComponentActivity() {
    private val route = MutableStateFlow(Routes.HOME)
    private var pendingTap: TapReporter.Tap? = null

    /** Foreground, background and cold-start taps all arrive here (cold start via onCreate's intent). */
    private fun handleTap(intent: Intent?) {
        val tap = TapReporter.consume(intent) ?: return
        route.value = tap.routeKey
        pendingTap = tap
        lifecycleScope.launch { TapReporter.report(this@MainActivity, tap); pendingTap = null }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleTap(intent)
    }
    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        // Permission state changed: re-register so the server records it.
        lifecycleScope.launch { DeviceRegistrar.register(this@MainActivity) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        PushService.ensureChannel(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        handleTap(intent)
        setContent {
            NeshPushAndroidTheme {
                val vm: AuthViewModel = viewModel()
                val user by vm.user.collectAsState()
                val loading by vm.loading.collectAsState()
                val error by vm.error.collectAsState()
                val context = LocalContext.current
                val registration by DeviceRegistrar.state.collectAsState()
                val currentRoute by route.collectAsState()
                LaunchedEffect(user?.uid) {
                    if (user != null) {
                        DeviceRegistrar.register(context)
                        // A tap that arrived before sign-in completed is reported once the user exists.
                        pendingTap?.let { TapReporter.report(context, it); pendingTap = null }
                    }
                }
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Auth gate: the auth-state flow drives the redirect, so there is no
                    // back stack to leak between signed-in and signed-out screens.
                    val u = user
                    if (u == null) {
                        SignInScreen(
                            loading = loading,
                            error = error,
                            onSignIn = { vm.signInWithGoogle(context) },
                            modifier = Modifier.padding(innerPadding),
                        )
                    } else {
                        HomeScreen(
                            user = u,
                            registration = registration,
                            route = currentRoute,
                            onNavigateHome = { route.value = Routes.HOME },
                            onSignOut = { vm.signOut(context) },
                            modifier = Modifier.padding(innerPadding),
                        )
                    }
                }
            }
        }
    }
}
