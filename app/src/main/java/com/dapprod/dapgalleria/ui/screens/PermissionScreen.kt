package com.dapprod.dapgalleria.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dapprod.dapgalleria.ui.components.NeonBackdrop
import com.dapprod.dapgalleria.ui.theme.BrandGradient
import com.dapprod.dapgalleria.ui.theme.Palette

@Composable
fun PermissionScreen(onGrant: () -> Unit, onOpenSettings: () -> Unit) {
    val t = rememberInfiniteTransition(label = "logo")
    val bob by t.animateFloat(-8f, 8f, infiniteRepeatable(tween(1_600), RepeatMode.Reverse), label = "bob")
    val tilt by t.animateFloat(-6f, 6f, infiniteRepeatable(tween(2_300), RepeatMode.Reverse), label = "tilt")
    Box(Modifier.fillMaxSize()) {
        NeonBackdrop()
        Column(
            Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "🃏",
                fontSize = 96.sp,
                modifier = Modifier.graphicsLayer {
                    translationY = bob.dp.toPx()
                    rotationZ = tilt
                },
            )
            Spacer(Modifier.height(16.dp))
            Text("DapGalleria", style = MaterialTheme.typography.headlineLarge.copy(brush = BrandGradient))
            Spacer(Modifier.height(8.dp))
            Text(
                "Scorri, tieni, elimina. Libera spazio, fai combo, trova le carte d'oro e completa il giro.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Per mostrarti foto e video uno alla volta serve l'accesso alla galleria. " +
                    "Resta tutto sul telefono: l'app non ha nemmeno il permesso di andare su internet.",
                style = MaterialTheme.typography.bodyLarge,
                color = Palette.TextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onGrant,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Palette.Magenta, contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) { Text("Entra in sala", fontWeight = FontWeight.Black, fontSize = 18.sp) }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onOpenSettings, shape = RoundedCornerShape(18.dp)) { Text("Apri le impostazioni") }
        }
    }
}
