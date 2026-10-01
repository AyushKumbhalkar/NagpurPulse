//java/com/nagpurpulse/ui/screens/settings/utilis/LockScreen.kt

package com.nagpurpulse.ui.screens.settings.utilis

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity

@Composable
fun LockScreen(
    onAuthenticated: () -> Unit
) {

    val activity =
        LocalContext.current as FragmentActivity

    LaunchedEffect(Unit) {

        BiometricPromptManager.show(
            activity = activity,
            onSuccess = onAuthenticated
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = null,
                modifier = Modifier.size(80.dp)
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text("Unlock NagpurPulse")
        }
    }
}