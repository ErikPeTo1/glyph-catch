package dev.equalparts.glyph_catch.screens.garden

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.data.CaughtPokemon
import dev.equalparts.glyph_catch.data.DayTime
import dev.equalparts.glyph_catch.data.EvolutionRequirement
import dev.equalparts.glyph_catch.data.FriendshipRules
import dev.equalparts.glyph_catch.data.Pokemon
import dev.equalparts.glyph_catch.data.PokemonSpecies
import dev.equalparts.glyph_catch.data.displayName

/** Five hearts, filled as friendship grows. */
@Composable
fun FriendshipHearts(friendship: Int, modifier: Modifier = Modifier, size: Dp = 16.dp) {
    val filled = FriendshipRules.hearts(friendship)
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(5) { index ->
            Icon(
                imageVector = if (index < filled) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = null,
                tint = if (index < filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(size)
            )
        }
    }
}

/** The evolutions this Pokémon gets through friendship, with their time of day. */
fun friendshipEvolutions(pokemon: CaughtPokemon): List<Pair<PokemonSpecies, DayTime?>> =
    Pokemon[pokemon.speciesId]?.evolvesTo.orEmpty().mapNotNull { id ->
        val target = Pokemon[id] ?: return@mapNotNull null
        val requirement = target.evolutionRequirement as? EvolutionRequirement.Friendship ?: return@mapNotNull null
        target to requirement.time
    }

@Composable
fun FriendshipEvolutionHint(pokemon: CaughtPokemon) {
    val evolutions = friendshipEvolutions(pokemon)
    if (evolutions.isEmpty()) return
    val names = evolutions.map { (target, time) ->
        when (time) {
            DayTime.DAY -> stringResource(R.string.friendship_by_day, target.displayName)
            DayTime.NIGHT -> stringResource(R.string.friendship_at_night, target.displayName)
            null -> target.displayName
        }
    }
    Text(
        text = stringResource(R.string.friendship_evolution_hint, names.joinToString(" · ")),
        style = MaterialTheme.typography.bodyMedium
    )
}
