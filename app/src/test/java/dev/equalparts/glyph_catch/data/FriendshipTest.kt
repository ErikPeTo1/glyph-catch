package dev.equalparts.glyph_catch.data

import dev.equalparts.glyph_catch.screens.garden.isAsleep
import dev.equalparts.glyph_catch.screens.garden.residentsOf
import dev.equalparts.glyph_catch.util.CandyEvolution
import dev.equalparts.glyph_catch.util.candyEvolution
import dev.equalparts.glyph_catch.util.findLevelEvolutionTarget
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FriendshipTest {

    private fun pokemon(
        speciesId: Int,
        level: Int = 30,
        friendship: Int = FriendshipRules.CAUGHT,
        id: String = "$speciesId"
    ) = CaughtPokemon(id = id, speciesId = speciesId, level = level, friendship = friendship)

    @Test
    fun `hearts fill every 50 points and the fifth only at the top`() {
        assertEquals(0, FriendshipRules.hearts(0))
        assertEquals(1, FriendshipRules.hearts(70))
        assertEquals(2, FriendshipRules.hearts(120))
        assertEquals(4, FriendshipRules.hearts(220))
        assertEquals(4, FriendshipRules.hearts(254))
        assertEquals(5, FriendshipRules.hearts(255))
    }

    @Test
    fun `level ups count less once a Pokémon loves you and never pass the top`() {
        assertEquals(76, FriendshipRules.afterLevelUps(70, 2))
        assertEquals(203, FriendshipRules.afterLevelUps(198, 2))
        assertEquals(255, FriendshipRules.afterLevelUps(250, 10))
    }

    @Test
    fun `training is a little faster with high friendship`() {
        assertEquals(30, FriendshipRules.withExpBonus(199, 30))
        assertEquals(33, FriendshipRules.withExpBonus(200, 30))
    }

    @Test
    fun `friendship evolutions need 220`() {
        assertNull(findLevelEvolutionTarget(pokemon(Pokemon.GOLBAT.id, level = 60, friendship = 219)))
        assertEquals(Pokemon.CROBAT, findLevelEvolutionTarget(pokemon(Pokemon.GOLBAT.id, friendship = 220)))
        assertEquals(Pokemon.PIKACHU, findLevelEvolutionTarget(pokemon(Pokemon.PICHU.id, level = 5, friendship = 230)))
    }

    @Test
    fun `Eevee becomes Espeon by day and Umbreon at night`() {
        val eevee = pokemon(Pokemon.EEVEE.id, friendship = 230)
        assertEquals(Pokemon.ESPEON, findLevelEvolutionTarget(eevee, DayTime.at(LocalTime.NOON)))
        assertEquals(Pokemon.UMBREON, findLevelEvolutionTarget(eevee, DayTime.at(LocalTime.of(23, 0))))
    }

    @Test
    fun `Rare Candies raise friendship on the way and can evolve with it`() {
        // 214 → 216 → 218 → 220: two points a level from 200 on
        val pichu = pokemon(Pokemon.PICHU.id, level = 5, friendship = 214)
        assertEquals(CandyEvolution.None, candyEvolution(pichu, 2))
        assertEquals(CandyEvolution.Evolves(listOf(Pokemon.PIKACHU)), candyEvolution(pichu, 3))
    }

    @Test
    fun `the Garden holds the team and the training partner, seven at most`() {
        val team = (1..6).map { pokemon(it, id = "t$it") }
        val partner = pokemon(25, id = "p").copy(isTraining = true)
        val residents = residentsOf(team + partner, team.map { it.id })
        assertEquals(team.map { it.id } + "p", residents.map { it.id })
        assertEquals(6, residentsOf(team + partner.copy(id = "t1"), team.map { it.id }).size)
    }

    @Test
    fun `the Garden sleeps for eight hours from your bedtime`() {
        val bedtime = 23 * 60
        assertEquals(false, isAsleep(bedtime, LocalTime.of(22, 59)))
        assertEquals(true, isAsleep(bedtime, LocalTime.of(23, 0)))
        assertEquals(true, isAsleep(bedtime, LocalTime.of(6, 59)))
        assertEquals(false, isAsleep(bedtime, LocalTime.of(7, 0)))
    }
}
