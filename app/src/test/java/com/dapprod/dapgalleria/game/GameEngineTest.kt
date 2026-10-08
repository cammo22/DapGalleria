package com.dapprod.dapgalleria.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {

    private val day = 20_000L

    @Test
    fun laComboSaleSeDecidiInFretta() {
        val g = GameEngine(Profile())
        var t = 0L
        repeat(5) { g.swipe(golden = false, nowMs = t, today = day); t += 1_000 }
        assertEquals(5, g.combo)
        assertEquals(2, g.multiplier)
        // troppo lento: si riparte da 1
        g.swipe(golden = false, nowMs = t + GameEngine.COMBO_WINDOW_MS + 1, today = day)
        assertEquals(1, g.combo)
        assertEquals(5, g.profile.bestCombo)
    }

    @Test
    fun laCartaDoroValeCinqueVolte() {
        val g = GameEngine(Profile())
        val out = g.swipe(golden = true, nowMs = 0, today = day)
        assertTrue(out.golden)
        assertEquals(GameEngine.SWIPE_POINTS * GameEngine.GOLDEN_MULTIPLIER, out.points)
        assertEquals(1, g.profile.goldenFound)
    }

    @Test
    fun annullareTogliePuntiESpegneLaCombo() {
        val g = GameEngine(Profile(achievements = Achievement.entries.map { it.name }.toSet()))
        val before = g.profile.score
        val out = g.swipe(golden = false, nowMs = 0, today = day)
        g.undoSwipe(out.points, golden = false)
        assertEquals(before, g.profile.score - out.bonusPoints)
        assertEquals(0, g.combo)
        assertEquals(0, g.profile.reviewedCount)
    }

    @Test
    fun ilPrimoSwipeSbloccaUnTraguardo() {
        val g = GameEngine(Profile())
        val out = g.swipe(golden = false, nowMs = 0, today = day)
        assertTrue(Achievement.FIRST_SWIPE in out.achievements)
        assertEquals(Achievement.FIRST_SWIPE.reward, out.bonusPoints)
        // non si sblocca due volte
        val again = g.swipe(golden = false, nowMs = 100, today = day)
        assertFalse(Achievement.FIRST_SWIPE in again.achievements)
    }

    @Test
    fun leMissioniDelGiornoSonoStabiliEDiverse() {
        assertEquals(Quests.forDay(day), Quests.forDay(day))
        assertEquals(3, Quests.forDay(day).size)
        val kinds = (0 until 30).map { Quests.forDay(day + it)[2].kind }.toSet()
        assertTrue(kinds.size > 1)
    }

    @Test
    fun completareLeMissioniApreIlForziere() {
        val g = GameEngine(Profile())
        val quests = Quests.forDay(day)
        var t = 0L
        var chest = false
        // fa tutto quello che serve per ogni tipo di missione
        repeat(100) { chest = chest || g.swipe(golden = it % 7 == 0, nowMs = t, today = day).chest; t += 500 }
        repeat(3) { chest = chest || g.edited(day).chest }
        chest = chest || g.deleted(40, 600 * MB, 600, day).chest
        assertTrue(quests.all { it.id in g.profile.questsClaimed })
        assertTrue(chest)
        assertEquals(1, g.profile.questDaysCompleted)
    }

    @Test
    fun iGiorniDiFila() {
        val g = GameEngine(Profile())
        g.swipe(false, 0, day)
        assertEquals(1, g.profile.streakDays)
        g.swipe(false, 0, day + 1)
        assertEquals(2, g.profile.streakDays)
        g.swipe(false, 0, day + 1)
        assertEquals(2, g.profile.streakDays)
        // salta un giorno: si riparte
        g.swipe(false, 0, day + 3)
        assertEquals(1, g.profile.streakDays)
        assertEquals(2, g.profile.bestStreak)
    }

    @Test
    fun laSerieBucaSiAzzeraAncheSoloAprendo() {
        val g = GameEngine(Profile(streakDays = 5, lastActiveDay = day, day = day))
        g.refreshDay(day + 1)
        assertEquals(5, g.profile.streakDays)
        g.refreshDay(day + 3)
        assertEquals(0, g.profile.streakDays)
    }

    @Test
    fun eliminareConLaSerieValeDiPiu() {
        val g = GameEngine(Profile(streakDays = 3, lastActiveDay = day - 1, day = day - 1, achievements = Achievement.entries.map { it.name }.toSet()))
        val out = g.deleted(1, 100 * MB, 100, day)
        // la serie sale a 4 solo dopo, il bonus usa quella di partenza (3 giorni = ×1,2)
        assertEquals(120, out.points)
        assertEquals(4, g.profile.streakDays)
        assertEquals(100 * MB, g.profile.freedBytes)
    }

    @Test
    fun fineGiro() {
        val g = GameEngine(Profile(achievements = Achievement.entries.map { it.name }.toSet()))
        repeat(10) { g.swipe(false, it * 100L, day) }
        val (summary, _) = g.completeRound(day)
        assertEquals(1, summary.round)
        assertEquals(10, summary.reviewed)
        assertEquals(GameEngine.ROUND_BONUS + 10, summary.bonus)
        assertEquals(2, g.profile.round)
        assertEquals(0, g.profile.roundReviewed)
        assertEquals(1, g.profile.roundsCompleted)
    }

    @Test
    fun saliDiLivello() {
        val g = GameEngine(Profile(score = Level.TRIMMER.minScore - 1, achievements = Achievement.entries.map { it.name }.toSet()))
        val out = g.swipe(false, 0, day)
        assertNotNull(out.levelUp)
        assertEquals(Level.TRIMMER, out.levelUp)
        assertNull(g.swipe(false, 10, day).levelUp)
    }

    @Test
    fun cartaDoroStabileECirca5PerCento() {
        val keys = (0 until 20_000).map { "content://media/external/images/media/$it" }
        val golden = keys.count { GameEngine.isGolden(it, 1) }
        assertTrue(golden in 600..1_400)
        assertEquals(GameEngine.isGolden(keys[7], 3), GameEngine.isGolden(keys[7], 3))
    }

    @Test
    fun puntiEliminazione() {
        assertEquals(1, GameEngine.pointsForDelete(10))
        assertEquals(12, GameEngine.pointsForDelete(12 * MB + 5))
    }
}
