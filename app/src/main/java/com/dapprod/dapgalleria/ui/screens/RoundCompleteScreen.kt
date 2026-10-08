package com.dapprod.dapgalleria.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dapprod.dapgalleria.game.RoundSummary
import com.dapprod.dapgalleria.ui.components.formatSize
import com.dapprod.dapgalleria.ui.components.formatThousands
import com.dapprod.dapgalleria.ui.theme.ArcadeNumber
import com.dapprod.dapgalleria.ui.theme.BrandGradient
import com.dapprod.dapgalleria.ui.theme.GoldGradient
import com.dapprod.dapgalleria.ui.theme.Palette
import kotlinx.coroutines.delay

/** Tutta la galleria vista: la festa del giro, i suoi numeri e il pulsante per ricominciare. */
@Composable
fun RoundCompleteScreen(
    summary: RoundSummary,
    pendingCount: Int,
    onNewRound: () -> Unit,
    onReview: () -> Unit,
) {
    val context = LocalContext.current
    BackHandler(onBack = onNewRound)
    val t = rememberInfiniteTransition(label = "trophy")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(9_000, easing = LinearEasing)), label = "spin")
    val float by t.animateFloat(-6f, 6f, infiniteRepeatable(tween(1_400, easing = FastOutSlowInEasing), androidx.compose.animation.core.RepeatMode.Reverse), label = "float")
    val trophy = remember { Animatable(0f) }
    LaunchedEffect(summary) { trophy.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessVeryLow)) }

    // le righe dei numeri entrano una dopo l'altra
    val rows = remember { List(5) { Animatable(0f) } }
    LaunchedEffect(summary) {
        delay(400)
        rows.forEach { a ->
            a.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow))
            delay(60)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.86f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    "✦",
                    fontSize = 180.sp,
                    style = MaterialTheme.typography.displayLarge.copy(brush = BrandGradient),
                    modifier = Modifier.graphicsLayer { rotationZ = spin; alpha = 0.35f },
                )
                Text(
                    "🏆",
                    fontSize = 96.sp,
                    modifier = Modifier.graphicsLayer {
                        val s = trophy.value
                        scaleX = s
                        scaleY = s
                        translationY = float.dp.toPx()
                    },
                )
            }
            Text("GIRO ${summary.round} COMPLETATO", style = MaterialTheme.typography.titleMedium.copy(brush = GoldGradient), fontWeight = FontWeight.Black, letterSpacing = 3.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                "Hai visto tutta la galleria!",
                style = MaterialTheme.typography.headlineSmall.copy(brush = BrandGradient),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Palette.Surface)
                    .border(1.dp, Palette.Outline, RoundedCornerShape(24.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SummaryRow("👀 Valutati", formatThousands(summary.reviewed.toLong()), rows[0].value)
                SummaryRow("🗑️ Eliminati", formatThousands(summary.deleted.toLong()), rows[1].value)
                SummaryRow("💾 Spazio liberato", formatSize(context, summary.freedBytes), rows[2].value)
                SummaryRow("🔥 Combo migliore", "×${summary.bestCombo}", rows[3].value)
                SummaryRow("🎁 Premio del giro", "+${formatThousands(summary.bonus)}", rows[4].value, gold = true)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Punti del giro: ${formatThousands(summary.points)}",
                color = Palette.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onNewRound,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Palette.Magenta, contentColor = Color.White),
            ) {
                Text("🔁  Ricomincia: giro ${summary.round + 1}", fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
            Text(
                "La galleria si ricarica (anche le foto nuove) e tutto torna nel mazzo, mescolato.",
                color = Palette.TextDim,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (pendingCount > 0) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onReview, shape = RoundedCornerShape(16.dp)) {
                    Text("Prima elimina i $pendingCount marchiati")
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, appear: Float, gold: Boolean = false) {
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = appear.coerceIn(0f, 1f)
                translationX = (1f - appear) * 60.dp.toPx()
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Palette.TextSecondary, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Text(
            value,
            style = if (gold) ArcadeNumber.copy(fontSize = 22.sp) else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = if (gold) Palette.Gold else Palette.TextPrimary,
        )
    }
}
