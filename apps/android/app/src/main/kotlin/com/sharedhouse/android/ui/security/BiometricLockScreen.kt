package com.sharedhouse.android.ui.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sharedhouse.android.R
import com.sharedhouse.android.ui.atmosphere.AmbientBackground
import com.sharedhouse.android.ui.atmosphere.Button
import com.sharedhouse.android.ui.atmosphere.Text
import com.sharedhouse.android.ui.atmosphere.TextButton
import com.sharedhouse.android.ui.theme.AtmosphereTheme
import androidx.compose.foundation.Image

@Composable
fun BiometricLockScreen(onUnlock:()->Unit,onUsePassword:()->Unit) {
    AmbientBackground(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().padding(28.dp),contentAlignment=Alignment.Center) {
            Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(18.dp)) {
                Image(painterResource(R.drawable.sharedhouse_logo_master),null,Modifier.size(160.dp))
                Text(stringResource(R.string.biometric_locked_title),style=AtmosphereTheme.typography.headlineMedium)
                Text(stringResource(R.string.biometric_locked_description),color=AtmosphereTheme.colorScheme.onSurfaceVariant)
                Button(onClick=onUnlock,shape=RoundedCornerShape(20.dp)) { Text(stringResource(R.string.biometric_unlock_action)) }
                TextButton(onClick=onUsePassword) { Text(stringResource(R.string.biometric_use_password)) }
            }
        }
    }
}
