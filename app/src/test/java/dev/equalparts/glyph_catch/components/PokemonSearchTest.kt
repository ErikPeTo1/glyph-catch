package dev.equalparts.glyph_catch.components

import dev.equalparts.glyph_catch.data.CaughtPokemon
import dev.equalparts.glyph_catch.data.Eggs
import org.junit.Assert.assertEquals
import org.junit.Test

class PokemonSearchTest {
    private val pc = listOf(
        CaughtPokemon(id = "pikachu", speciesId = 25, level = 12, isShiny = true),
        CaughtPokemon(id = "raichu", speciesId = 26, level = 40, nickname = "Sparky"),
        CaughtPokemon(id = "squirtle", speciesId = 7, level = 5, isFavorite = true),
        CaughtPokemon(id = "gyarados", speciesId = 130, level = 55),
        CaughtPokemon(id = "charmander", speciesId = 4, level = 20),
        CaughtPokemon(id = "togepi", speciesId = 175, level = 1, spawnPoolName = Eggs.POOL_NAME),
        CaughtPokemon(id = "mew", speciesId = 151, level = 35),
        CaughtPokemon(id = "lugia", speciesId = 249, level = 75),
        CaughtPokemon(id = "oshawott", speciesId = 501, level = 9, isTraining = true)
    )

    private fun search(query: String) = pc.filter(PokemonSearch.matcher(query)).map { it.id }.toSet()

    @Test
    fun `blank shows everything`() {
        assertEquals(pc.size, search("  ").size)
    }

    @Test
    fun `names and nicknames, ignoring case`() {
        assertEquals(setOf("pikachu"), search("PIKA"))
        assertEquals(setOf("raichu"), search("spark"))
    }

    @Test
    fun `Pokédex numbers and ranges`() {
        assertEquals(setOf("pikachu"), search("25"))
        assertEquals(setOf("pikachu", "raichu", "squirtle", "gyarados", "charmander", "mew"), search("1-151"))
        assertEquals(setOf("lugia", "oshawott"), search("200-"))
        assertEquals(setOf("charmander", "squirtle"), search("-10"))
    }

    @Test
    fun `types, regions and keywords`() {
        assertEquals(setOf("squirtle", "gyarados", "oshawott"), search("water"))
        assertEquals(setOf("gyarados", "lugia"), search("flying"))
        assertEquals(setOf("togepi", "lugia"), search("johto"))
        assertEquals(setOf("pikachu"), search("shiny"))
        assertEquals(setOf("squirtle"), search("favorite"))
        assertEquals(setOf("togepi"), search("egg"))
        assertEquals(setOf("oshawott"), search("training"))
        assertEquals(setOf("mew", "lugia"), search("legendary"))
        assertEquals(setOf("mew"), search("mythical"))
    }

    @Test
    fun `levels`() {
        assertEquals(setOf("gyarados", "lugia"), search("lv50-"))
        assertEquals(setOf("raichu"), search("lv40"))
        assertEquals(setOf("pikachu", "charmander"), search("level10-20"))
    }

    @Test
    fun `whole families`() {
        assertEquals(setOf("pikachu", "raichu"), search("+pichu"))
        assertEquals(setOf("pikachu", "raichu"), search("+raichu"))
    }

    @Test
    fun `or, and, not`() {
        assertEquals(setOf("squirtle", "gyarados", "oshawott", "charmander"), search("water,fire"))
        assertEquals(setOf("gyarados"), search("water&flying"))
        assertEquals(setOf("squirtle", "gyarados"), search("water & !training & 1-151"))
        assertEquals(pc.size - 3, search("!water").size)
    }

    @Test
    fun `accents don't matter`() {
        assertEquals(PokemonSearch.normalize("electrico"), PokemonSearch.normalize("Eléctrico"))
        assertEquals("oeuf", PokemonSearch.normalize("Œuf"))
    }
}
