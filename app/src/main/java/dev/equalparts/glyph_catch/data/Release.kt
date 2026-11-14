package dev.equalparts.glyph_catch.data

/**
 * Lets these Pokémon go, from the PC or from their details: they stay in the Pokédex, the team
 * and the companion forget them, and each one leaves a Rare Candy behind.
 */
suspend fun releasePokemon(db: PokemonDatabase, preferences: PreferencesManager, ids: List<String>) {
    if (ids.isEmpty()) return
    db.pokemonDao().deleteCaught(ids)
    preferences.forgetMissingPokemon(db)
    preferences.releasedCount += ids.size
    val inventoryDao = db.inventoryDao()
    if (inventoryDao.getItem(Item.RARE_CANDY.ordinal) == null) {
        inventoryDao.insertItem(InventoryItem(itemId = Item.RARE_CANDY.ordinal, quantity = ids.size))
    } else {
        inventoryDao.addItems(Item.RARE_CANDY.ordinal, ids.size)
    }
}
