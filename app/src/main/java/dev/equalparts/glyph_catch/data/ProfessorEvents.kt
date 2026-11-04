package dev.equalparts.glyph_catch.data

/**
 * Conversations with the professors that show up when the app is opened: Oak's welcome, each
 * regional Pokédex, the Town Map and the key items for legendary Pokémon.
 */
sealed class ProfessorEvent(val professor: Professor, val gifts: List<Item>) {
    data object Intro : ProfessorEvent(Professor.OAK, listOf(Item.POKEDEX_KANTO))

    /** For players who met Oak before he started handing out the Kanto Pokédex. */
    data object KantoPokedex : ProfessorEvent(Professor.OAK, listOf(Item.POKEDEX_KANTO))

    data class RegionPokedex(val region: Region) : ProfessorEvent(region.professor, listOfNotNull(region.pokedex))

    data object TownMap : ProfessorEvent(Professor.OAK, listOf(Item.TOWN_MAP))

    /** The reward for completing the national Pokédex. */
    data object ShinyCharm : ProfessorEvent(Professor.OAK, listOf(Item.SHINY_CHARM))

    data class LegendaryGifts(val by: Professor, val items: List<Item>) : ProfessorEvent(by, items)
}

object ProfessorEvents {
    /**
     * Everything the player has earned but not received yet, in the order it's told.
     */
    fun pending(introSeen: Boolean, pokedexCount: Int, owned: Set<Item>): List<ProfessorEvent> {
        val events = mutableListOf<ProfessorEvent>()
        val willOwn = owned.toMutableSet()
        if (!introSeen) {
            events += ProfessorEvent.Intro
        } else if (Item.POKEDEX_KANTO !in owned) {
            events += ProfessorEvent.KantoPokedex
        }

        Region.entries.forEach { region ->
            val pokedex = region.pokedex ?: return@forEach
            if (pokedex !in willOwn && pokedexCount >= region.unlockAt) {
                events += ProfessorEvent.RegionPokedex(region)
                willOwn += pokedex
            }
        }
        if (Item.TOWN_MAP !in willOwn && willOwn.containsAll(REGION_POKEDEXES)) {
            events += ProfessorEvent.TownMap
        }

        if (Item.SHINY_CHARM !in willOwn && pokedexCount >= Pokemon.POKEDEX_SIZE) {
            events += ProfessorEvent.ShinyCharm
        }

        val unlockedRegions = Region.entries.filter { it.pokedex == null || it.pokedex in willOwn }
        LegendaryItems.species.keys
            .filter { it !in willOwn && pokedexCount >= LegendaryItems.unlockAt(it) }
            .filter { LegendaryItems.region(it) in unlockedRegions }
            .groupBy { LegendaryItems.region(it).professor }
            .toSortedMap()
            .forEach { (professor, items) ->
                events += ProfessorEvent.LegendaryGifts(professor, items.sortedBy { LegendaryItems.unlockAt(it) })
            }
        return events
    }

    /**
     * Professors whose number is saved in the Pokégear.
     */
    fun contacts(introSeen: Boolean, owned: Set<Item>): List<Professor> = Professor.entries.filter { professor ->
        val pokedex = Region.ofProfessor(professor).pokedex
        if (pokedex == null) introSeen else pokedex in owned
    }

    suspend fun complete(event: ProfessorEvent, db: PokemonDatabase, preferences: PreferencesManager) {
        val dao = db.inventoryDao()
        event.gifts.forEach { item ->
            if ((dao.getItem(item.ordinal)?.quantity ?: 0) > 0) return@forEach
            if (dao.getItem(item.ordinal) == null) {
                dao.insertItem(InventoryItem(itemId = item.ordinal, quantity = 1))
            } else {
                dao.addItems(item.ordinal, 1)
            }
        }
        if (event is ProfessorEvent.Intro) preferences.professorIntroSeen = true
    }
}
