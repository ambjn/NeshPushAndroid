package com.ambjn.neshpushandroid

import android.Manifest
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ambjn.neshpushandroid.auth.AuthViewModel
import com.ambjn.neshpushandroid.auth.SignInScreen
import com.ambjn.neshpushandroid.push.DeviceRegistrar
import com.ambjn.neshpushandroid.push.PushService
import com.ambjn.neshpushandroid.ui.theme.NeshPushAndroidTheme

class MainActivity : ComponentActivity() {
    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        PushService.ensureChannel(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            NeshPushAndroidTheme {
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
                        HomeScreen(user = u, registration = registration, onSignOut = { vm.signOut(context) }, modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }
}
