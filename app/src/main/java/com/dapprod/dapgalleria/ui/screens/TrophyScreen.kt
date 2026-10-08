package com.dapprod.dapgalleria.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dapprod.dapgalleria.game.Achievement
import com.dapprod.dapgalleria.game.Level
import com.dapprod.dapgalleria.game.Profile
import com.dapprod.dapgalleria.game.Quest
import com.dapprod.dapgalleria.game.Quests
import com.dapprod.dapgalleria.ui.GalleryUiState
import com.dapprod.dapgalleria.ui.components.AnimatedNumber
import com.dapprod.dapgalleria.ui.components.ProgressRing
import com.dapprod.dapgalleria.ui.components.StorageBar
import com.dapprod.dapgalleria.ui.components.formatSize
import com.dapprod.dapgalleria.ui.components.formatThousands
import com.dapprod.dapgalleria.ui.components.shimmerBorder
import com.dapprod.dapgalleria.ui.components.storageStats
import com.dapprod.dapgalleria.ui.theme.ArcadeNumber
import com.dapprod.dapgalleria.ui.theme.BrandGradient
import com.dapprod.dapgalleria.ui.theme.GoldGradient
import com.dapprod.dapgalleria.ui.theme.Palette

/** La bacheca: livello, missioni del giorno, giro in corso, record e traguardi. */
@Composable
fun TrophyScreen(state: GalleryUiState, onBack: () -> Unit) {
    val p = state.profile
    val quests = remember(p.day) { Quests.forDay(p.day) }
    LazyColumn(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro", tint = Palette.TextPrimary)
                }
                Text("Bacheca", style = MaterialTheme.typography.headlineSmall.copy(brush = BrandGradient))
            }
        }
        item { LevelCard(p) }
        item { QuestsCard(p, quests) }
        item { RoundCard(state) }
        item { StorageCard(p) }
        item { RecordsCard(p) }
        item { SectionTitle("Traguardi", "${p.achievements.size}/${Achievement.entries.size}") }
        items(Achievement.entries.chunked(3)) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { a -> AchievementTile(a, unlocked = a.name in p.achievements, modifier = Modifier.weight(1f)) }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        item { SectionTitle("Livelli", "") }
        items(Level.entries) { level -> LevelRow(level, p) }
        item { Spacer(Modifier.navigationBarsPadding()) }
    }
}

@Composable
private fun Card(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Palette.Surface.copy(alpha = 0.92f))
            .border(1.dp, Palette.Outline, RoundedCornerShape(24.dp))
            .padding(18.dp),
    ) { content() }
}

@Composable
private fun SectionTitle(title: String, right: String) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title.uppercase(), fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Palette.TextSecondary, modifier = Modifier.weight(1f))
        Text(right, color = Palette.TextSecondary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LevelCard(p: Profile) {
    val next = p.nextLevel
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(p.levelProgress, 96.dp, stroke = 7.dp) { Text(p.level.emoji, fontSize = 42.sp) }
            Spacer(Modifier.width(18.dp))
            Column {
                Text(p.level.title, style = MaterialTheme.typography.titleLarge.copy(brush = BrandGradient, fontWeight = FontWeight.Black))
                AnimatedNumber(p.score, style = ArcadeNumber.copy(fontSize = 36.sp), color = Palette.Gold)
                Text(
                    if (next != null) "mancano ${formatThousands(next.minScore - p.score)} punti per ${next.emoji} ${next.title}" else "Sei al livello massimo. Cosmico.",
                    color = Palette.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun QuestsCard(p: Profile, quests: List<Quest>) {
    val allDone = quests.all { it.id in p.questsClaimed }
    Card(if (allDone) Modifier.shimmerBorder(listOf(Palette.Gold, Color.White, Palette.GoldDeep), 2.dp, 24.dp) else Modifier) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🎯 Missioni di oggi", fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                Text("si rinnovano a mezzanotte", color = Palette.TextDim, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(12.dp))
            quests.forEach { q ->
                QuestRow(q, p.questProgress(q), done = q.id in p.questsClaimed)
                Spacer(Modifier.height(10.dp))
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (allDone) GoldGradient else BrandGradient, alpha = if (allDone) 1f else 0.18f)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (allDone) "🎁" else "🔒", fontSize = 26.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (allDone) "Forziere aperto!" else "Forziere del giorno",
                        fontWeight = FontWeight.ExtraBold,
                        color = if (allDone) Color(0xFF2A1A00) else Palette.TextPrimary,
                    )
                    Text(
                        if (allDone) "Torna domani per nuove missioni" else "Completa le tre missioni per aprirlo",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (allDone) Color(0xFF2A1A00) else Palette.TextSecondary,
                    )
                }
                Text("+${Quests.CHEST_REWARD}", fontWeight = FontWeight.Black, color = if (allDone) Color(0xFF2A1A00) else Palette.Gold)
            }
        }
    }
}

@Composable
private fun QuestRow(q: Quest, progress: Long, done: Boolean) {
    val fraction by animateFloatAsState((progress.toFloat() / q.target).coerceIn(0f, 1f), tween(800, easing = FastOutSlowInEasing), label = "quest")
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (done) "✅" else q.emoji, fontSize = 18.sp)
            Spacer(Modifier.width(8.dp))
            Text(q.title, modifier = Modifier.weight(1f), color = if (done) Palette.TextSecondary else Palette.TextPrimary, fontWeight = FontWeight.SemiBold)
            Text("${progress.coerceAtMost(q.target)}/${q.target}", color = Palette.TextSecondary, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.width(8.dp))
            Text("+${q.reward}", color = Palette.Gold, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(Palette.SurfaceHigh),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(RoundedCornerShape(50))
                    .background(if (done) GoldGradient else BrandGradient),
            )
        }
    }
}

@Composable
private fun RoundCard(state: GalleryUiState) {
    val context = LocalContext.current
    val p = state.profile
    Card {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔁 Giro ${p.round}", fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                Text("${(state.roundProgress * 100).toInt()}%", fontWeight = FontWeight.Black, color = Palette.Cyan)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Quando hai visto tutte le foto e tutti i video il giro si chiude con un premio, la galleria si ricarica e si ricomincia da capo.",
                color = Palette.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                MiniStat(formatThousands(state.reviewedItems.toLong()), "visti")
                MiniStat(formatThousands((state.totalItems - state.reviewedItems).toLong()), "mancano")
                MiniStat(formatThousands(p.roundDeleted.toLong()), "eliminati")
                MiniStat(formatSize(context, p.roundFreed), "liberati")
            }
        }
    }
}

@Composable
private fun StorageCard(p: Profile) {
    val context = LocalContext.current
    val (free, total) = remember { storageStats() }
    Card {
        Column {
            Text("💾 Memoria del telefono", fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(10.dp))
            if (total > 0) StorageBar(free, total, 0L)
            Spacer(Modifier.height(10.dp))
            Text(
                "Con DapGalleria hai liberato ${formatSize(context, p.freedBytes)} in tutto.",
                color = Palette.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun RecordsCard(p: Profile) {
    val context = LocalContext.current
    Card {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("📊 I tuoi numeri", fontWeight = FontWeight.ExtraBold)
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                MiniStat(formatThousands(p.reviewedCount.toLong()), "valutati")
                MiniStat(formatThousands(p.deletedCount.toLong()), "eliminati")
                MiniStat(formatSize(context, p.freedBytes), "liberati")
            }
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                MiniStat("🔥 ${p.bestCombo}", "combo record")
                MiniStat("📅 ${p.streakDays}", "giorni di fila")
                MiniStat("🏅 ${p.bestStreak}", "serie record")
            }
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                MiniStat("✨ ${p.goldenFound}", "carte d'oro")
                MiniStat("🔁 ${p.roundsCompleted}", "giri finiti")
                MiniStat("🎨 ${p.edits}", "modifiche")
            }
        }
    }
}

@Composable
private fun MiniStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(96.dp)) {
        Text(value, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, color = Palette.TextSecondary, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
    }
}

@Composable
private fun AchievementTile(a: Achievement, unlocked: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (unlocked) Palette.SurfaceHigh else Palette.Surface)
            .then(if (unlocked) Modifier.border(1.dp, GoldGradient, RoundedCornerShape(18.dp)) else Modifier)
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            if (unlocked) a.emoji else "🔒",
            fontSize = 28.sp,
            modifier = Modifier.graphicsLayer { alpha = if (unlocked) 1f else 0.5f },
        )
        Spacer(Modifier.height(4.dp))
        Text(
            a.title,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            color = if (unlocked) Palette.TextPrimary else Palette.TextDim,
            maxLines = 2,
        )
        Text(
            a.description,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = Palette.TextDim,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text("+${formatThousands(a.reward)}", color = if (unlocked) Palette.Gold else Palette.TextDim, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun LevelRow(level: Level, p: Profile) {
    val reached = p.score >= level.minScore
    val current = p.level == level
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (current) Palette.SurfaceHigh else Color.Transparent)
            .then(if (current) Modifier.border(1.dp, BrandGradient, RoundedCornerShape(16.dp)) else Modifier)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(level.emoji, fontSize = 22.sp, modifier = Modifier.graphicsLayer { alpha = if (reached) 1f else 0.35f })
        Spacer(Modifier.width(12.dp))
        Text(level.title, modifier = Modifier.weight(1f), fontWeight = if (current) FontWeight.Black else FontWeight.Medium, color = if (reached) Palette.TextPrimary else Palette.TextDim)
        Text(formatThousands(level.minScore), color = if (reached) Palette.Gold else Palette.TextDim, fontWeight = FontWeight.Bold)
        if (current) {
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(Palette.Magenta))
        }
    }
}
