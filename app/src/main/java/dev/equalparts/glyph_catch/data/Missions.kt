package dev.equalparts.glyph_catch.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.random.Random

/**
 * A goal based on things the player controls (never on luck), measured with [DailyStats].
 */
data class Mission(
    val id: String,
    val stat: Stat,
    val target: Int,
    val weekly: Boolean,
    val reward: Item,
    val rewardAmount: Int
)

data class MissionProgress(val mission: Mission, val progress: Int, val claimed: Boolean) {
    val isComplete: Boolean get() = progress >= mission.target
    val canClaim: Boolean get() = isComplete && !claimed
}

/**
 * Three daily missions and one weekly mission, picked deterministically from the date so they
 * stay the same all day (or week). Completing one gives items and a one-hour rarity boost.
 */
object Missions {
    const val BOOST_MINUTES = 60
    private const val WEEKLY_BOOST_MINUTES = 180

    private val dailyTargets = mapOf(
        Stat.MINUTES to listOf(60, 120, 180),
        Stat.CATCHES to listOf(3, 5, 8),
        Stat.NEW_SPECIES to listOf(1, 2),
        Stat.LEVELS to listOf(3, 5),
        Stat.EVOLUTIONS to listOf(1),
        Stat.ITEMS_USED to listOf(1),
        Stat.ROD_CATCHES to listOf(1, 2)
    )

    private val weeklyTargets = mapOf(
        Stat.MINUTES to listOf(900, 1200),
        Stat.CATCHES to listOf(30, 40),
        Stat.NEW_SPECIES to listOf(5, 8),
        Stat.LEVELS to listOf(15, 20),
        Stat.EVOLUTIONS to listOf(3)
    )

    private val dailyRewards = listOf(
        Item.RARE_CANDY to 2,
        Item.FIRE_STONE to 1,
        Item.WATER_STONE to 1,
        Item.THUNDER_STONE to 1,
        Item.LEAF_STONE to 1,
        Item.MOON_STONE to 1,
        Item.SUN_STONE to 1
    )

    private val weeklyRewards = listOf(
        Item.LINKING_CORD to 1,
        Item.SHINY_STONE to 1,
        Item.DUSK_STONE to 1,
        Item.DAWN_STONE to 1,
        Item.ICE_STONE to 1,
        Item.RARE_CANDY to 5
    )

    fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /**
     * Today's missions: three daily ones, then the weekly one. Rod missions only show up once
     * the player owns a rod.
     */
    fun current(date: LocalDate = LocalDate.now(), hasRod: Boolean): List<Mission> {
        val dailyRandom = Random(date.toEpochDay())
        val dailyStats = dailyTargets.keys.filter { hasRod || it != Stat.ROD_CATCHES }.shuffled(dailyRandom).take(3)
        val daily = dailyStats.mapIndexed { index, stat ->
            val (reward, amount) = dailyRewards.random(dailyRandom)
            Mission("d:$date:$index", stat, dailyTargets.getValue(stat).random(dailyRandom), false, reward, amount)
        }

        val week = weekStart(date)
        val weeklyRandom = Random(week.toEpochDay() * 31)
        val weeklyStat = weeklyTargets.keys.random(weeklyRandom)
        val (reward, amount) = weeklyRewards.random(weeklyRandom)
        val weekly =
            Mission(
                "w:$week",
                weeklyStat,
                weeklyTargets.getValue(weeklyStat).random(weeklyRandom),
                true,
                reward,
                amount
            )

        return daily + weekly
    }

    fun progress(mission: Mission, stats: DailyStats, claimed: Set<String>, date: LocalDate = LocalDate.now()) =
        MissionProgress(
            mission = mission,
            progress = if (mission.weekly) {
                stats.sum(mission.stat, weekStart(date), date)
            } else {
                stats.get(mission.stat, date)
            },
            claimed = mission.id in claimed
        )

    fun boostMinutes(mission: Mission): Int = if (mission.weekly) WEEKLY_BOOST_MINUTES else BOOST_MINUTES

    /**
     * Gives the reward of a completed mission: its items plus the rarity boost.
     */
    suspend fun claim(db: PokemonDatabase, preferencesManager: PreferencesManager, progress: MissionProgress): Boolean {
        if (!progress.canClaim) return false
        val mission = progress.mission
        preferencesManager.claimedMissions = (preferencesManager.claimedMissions + mission.id)
            .filter { it.startsWith("w:") || it.startsWith("d:${LocalDate.now()}") || isRecent(it) }
            .toSet()

        val inventoryDao = db.inventoryDao()
        if (inventoryDao.getItem(mission.reward.ordinal) == null) {
            inventoryDao.insertItem(InventoryItem(itemId = mission.reward.ordinal, quantity = mission.rewardAmount))
        } else {
            inventoryDao.addItems(mission.reward.ordinal, mission.rewardAmount)
        }

        val now = System.currentTimeMillis()
        val start = maxOf(now, preferencesManager.rarityBoostUntil)
        preferencesManager.rarityBoostUntil = start + boostMinutes(mission) * 60_000L
        return true
    }

    private fun isRecent(id: String): Boolean {
        val date = id.split(":").getOrNull(1)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return false
        return date.isAfter(LocalDate.now().minusDays(14))
    }
}
