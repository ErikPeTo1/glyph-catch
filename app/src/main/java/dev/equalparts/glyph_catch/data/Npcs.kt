package dev.equalparts.glyph_catch.data

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dev.equalparts.glyph_catch.R

/**
 * People who show up once to hand over an item. The clerk and the breeder are a man or a woman
 * depending on the save.
 */
enum class Npc(@param:DrawableRes val sprite: Int, @param:StringRes val nameRes: Int) {
    FISHERMAN(R.drawable.npc_fisherman, R.string.npc_fisherman),
    CLERK_M(R.drawable.npc_vendor_m, R.string.npc_clerk_m),
    CLERK_F(R.drawable.npc_vendor_f, R.string.npc_clerk_f),
    BREEDER_M(R.drawable.npc_breeder_m, R.string.npc_breeder_m),
    BREEDER_F(R.drawable.npc_breeder_f, R.string.npc_breeder_f)
}

sealed class NpcEvent(val gift: Item?) {
    /** The Fisherman's rods, at 15, 45 and 100 catches. */
    data class Rod(val rod: Item) : NpcEvent(rod)

    /** A clerk gives away the Repel once the Pokédex has 50 entries. */
    data object Repel : NpcEvent(Item.REPEL)

    /** With the Johto Pokédex, a breeder explains how Eggs work and gives the first one. */
    data object Breeder : NpcEvent(null)
}

object NpcEvents {
    val ROD_UNLOCK_COUNTS = listOf(Item.OLD_ROD to 15, Item.GOOD_ROD to 45, Item.SUPER_ROD to 100)
    const val REPEL_UNLOCK_SPECIES_COUNT = 50

    /**
     * Encounters the player has earned, in order. [inBag] holds every item with a Bag entry,
     * even used up ones, so nothing is handed over twice.
     */
    fun pending(caughtEver: Int, pokedexCount: Int, inBag: Set<Item>, breederMet: Boolean): List<NpcEvent> = buildList {
        ROD_UNLOCK_COUNTS.forEach { (rod, count) ->
            if (rod !in inBag && caughtEver >= count) add(NpcEvent.Rod(rod))
        }
        if (Item.REPEL !in inBag && pokedexCount >= REPEL_UNLOCK_SPECIES_COUNT) add(NpcEvent.Repel)
        if (!breederMet && Item.POKEDEX_JOHTO in inBag) add(NpcEvent.Breeder)
    }

    fun npcFor(event: NpcEvent, preferences: PreferencesManager): Npc = when (event) {
        is NpcEvent.Rod -> Npc.FISHERMAN
        NpcEvent.Repel -> if (preferences.npcVariantFemale(CLERK)) Npc.CLERK_F else Npc.CLERK_M
        NpcEvent.Breeder -> if (preferences.npcVariantFemale(BREEDER)) Npc.BREEDER_F else Npc.BREEDER_M
    }

    suspend fun complete(event: NpcEvent, db: PokemonDatabase, preferences: PreferencesManager) {
        event.gift?.let { item ->
            if (db.inventoryDao().getItem(item.ordinal) == null) {
                db.inventoryDao().insertItem(InventoryItem(itemId = item.ordinal, quantity = 1))
            }
        }
        when (event) {
            is NpcEvent.Rod -> preferences.markSuperRodDiscovered()
            NpcEvent.Repel -> preferences.markRepelDiscovered()
            NpcEvent.Breeder -> {
                preferences.breederMet = true
                if (db.eggDao().countEggs() < Eggs.MAX_EGGS) {
                    val owned = db.inventoryDao().getAllItems()
                        .filter { it.quantity > 0 }
                        .mapNotNull { Item.entries.getOrNull(it.itemId) }
                        .toSet()
                    val regions = Region.active(owned, preferences.disabledRegions)
                    Eggs.newEgg { canSpawnSpecies(it, owned, regions) }?.let { db.eggDao().insertEgg(it) }
                }
            }
        }
    }

    private const val CLERK = "clerk"
    private const val BREEDER = "breeder"
}
