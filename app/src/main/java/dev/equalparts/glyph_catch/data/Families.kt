package dev.equalparts.glyph_catch.data

private val preEvolutions: Map<Int, Int> by lazy {
    Pokemon.all.values.flatMap { species -> species.evolvesTo.map { it to species.id } }.toMap()
}

/** Every species in the same evolution family, babies included: Pichu, Pikachu and Raichu. */
fun evolutionFamily(speciesId: Int): Set<Int> {
    var base = speciesId
    while (true) base = preEvolutions[base] ?: break
    val family = mutableSetOf<Int>()
    val next = ArrayDeque(listOf(base))
    while (next.isNotEmpty()) {
        val id = next.removeFirst()
        if (family.add(id)) Pokemon[id]?.evolvesTo?.let(next::addAll)
    }
    return family
}
