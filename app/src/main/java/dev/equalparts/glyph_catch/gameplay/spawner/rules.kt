package dev.equalparts.glyph_catch.gameplay.spawner

import dev.equalparts.glyph_catch.data.Item
import dev.equalparts.glyph_catch.data.Pokemon
import dev.equalparts.glyph_catch.data.Type
import dev.equalparts.glyph_catch.gameplay.spawner.models.SpawnRules
import dev.equalparts.glyph_catch.gameplay.spawner.models.SpawnScaling
import dev.equalparts.glyph_catch.gameplay.spawner.models.increaseBy
import dev.equalparts.glyph_catch.gameplay.spawner.models.percent
import kotlin.time.Duration.Companion.minutes

/**
 * This defines the spawn rules for the game using a custom DSL.
 */
fun createSpawnRules(context: GameplayContext): SpawnRules {
    val dsl = PokemonSpawnDsl(context)

    val time = context.time
    val weather = context.weather
    val season = context.season
    val events = context.events
    val phone = context.phone
    val trainer = context.trainer
    val sleep = context.sleep
    val scaling = SpawnScaling(context)

    return dsl.pools {
        // Global modifiers
        // ================
        //
        // These affect all spawn pools, boosting certain types and suppressing
        // others based on weather conditions and time of day.

        modifiers {
            during(weather::rain) {
                boost(Type.WATER) by 5.0f
                boost(Type.GRASS) by 3.0f
            }

            during(weather::thunderstorm) {
                boost(Type.ELECTRIC) by 5.0f
                boost(Type.STEEL) by 3.0f
            }

            during(weather::snow) {
                boost(Type.ICE) by 5.0f
                suppress(Type.GRASS) by 0.5f
                suppress(Type.FIRE) by 0.5f
            }

            during(time::night) {
                suppress(Type.NORMAL) by 0.5f
            }
        }

        // Starter events
        // ==============
        //
        // The player can catch one of the three starter Pokémon shortly after
        // starting the game. The other two starters will be available from
        // the rare spawn pool.

        val starters = listOf(
            Pokemon.BULBASAUR,
            Pokemon.CHARMANDER,
            Pokemon.SQUIRTLE
        )

        starters.forEach {
            special(it) {
                activate(33.percent) given { trainer.hasNotFoundAny(starters) }
            }
        }

        // Common Pokémon
        // ==============
        //
        // As long as the player keeps their phone screen off, this percentage will
        // slowly be reallocated to other groups.

        pool("Common", 86.8.percent) {
            Pokemon.CATERPIE at 2.0f
            Pokemon.WEEDLE at 2.0f
            Pokemon.PIDGEY at 2.0f
            Pokemon.RATTATA at 2.0f
            Pokemon.SPEAROW at 1.0f
            Pokemon.EKANS at 1.0f
            Pokemon.SANDSHREW at 1.0f
            Pokemon.NIDORAN_F at 1.0f
            Pokemon.NIDORAN_M at 1.0f
            Pokemon.ODDISH at 1.0f
            Pokemon.PARAS at 1.0f
            Pokemon.DIGLETT at 1.0f
            Pokemon.MEOWTH at 1.0f
            Pokemon.PSYDUCK at 1.0f
            Pokemon.MANKEY at 1.0f
            Pokemon.POLIWAG at 1.0f
            Pokemon.MACHOP at 1.0f
            Pokemon.BELLSPROUT at 1.0f
            Pokemon.GEODUDE at 1.0f
            Pokemon.DODUO at 1.0f
            Pokemon.GRIMER at 1.0f
            Pokemon.KOFFING at 1.0f
            Pokemon.DROWZEE at 1.0f
            Pokemon.KRABBY at 1.0f
            Pokemon.GASTLY at 0.3f during time::night
            Pokemon.VENONAT at 3.0f during time::night
            Pokemon.ZUBAT at 4.0f during time::night

            // Johto - Unova
            Pokemon.SENTRET at 1.0f
            Pokemon.HOOTHOOT at 2.0f during time::night
            Pokemon.LEDYBA at 1.0f
            Pokemon.SPINARAK at 1.0f
            Pokemon.NATU at 1.0f
            Pokemon.MAREEP at 1.0f
            Pokemon.HOPPIP at 1.0f
            Pokemon.SUNKERN at 1.0f
            Pokemon.WOOPER at 1.0f
            Pokemon.UNOWN at 1.0f
            Pokemon.PINECO at 1.0f
            Pokemon.DUNSPARCE at 1.0f
            Pokemon.SNUBBULL at 1.0f
            Pokemon.SHUCKLE at 1.0f
            Pokemon.SLUGMA at 1.0f
            Pokemon.SWINUB at 1.0f
            Pokemon.POOCHYENA at 1.0f
            Pokemon.ZIGZAGOON at 1.0f
            Pokemon.WURMPLE at 1.0f
            Pokemon.LOTAD at 1.0f
            Pokemon.SEEDOT at 1.0f
            Pokemon.TAILLOW at 1.0f
            Pokemon.WINGULL at 1.0f
            Pokemon.RALTS at 1.0f
            Pokemon.SURSKIT at 1.0f
            Pokemon.SHROOMISH at 1.0f
            Pokemon.SLAKOTH at 1.0f
            Pokemon.NINCADA at 1.0f
            Pokemon.WHISMUR at 1.0f
            Pokemon.MAKUHITA at 1.0f
            Pokemon.NOSEPASS at 1.0f
            Pokemon.SKITTY at 1.0f
            Pokemon.ARON at 1.0f
            Pokemon.MEDITITE at 1.0f
            Pokemon.PLUSLE at 1.0f
            Pokemon.MINUN at 1.0f
            Pokemon.VOLBEAT at 1.0f
            Pokemon.ILLUMISE at 1.0f
            Pokemon.GULPIN at 1.0f
            Pokemon.NUMEL at 1.0f
            Pokemon.SPOINK at 1.0f
            Pokemon.SPINDA at 1.0f
            Pokemon.TRAPINCH at 1.0f
            Pokemon.CACNEA at 1.0f
            Pokemon.SWABLU at 1.0f
            Pokemon.BALTOY at 1.0f
            Pokemon.KECLEON at 1.0f
            Pokemon.SHUPPET at 2.0f during time::night
            Pokemon.DUSKULL at 2.0f during time::night
            Pokemon.TROPIUS at 1.0f
            Pokemon.SNORUNT at 1.0f
            Pokemon.SPHEAL at 1.0f
            Pokemon.STARLY at 1.0f
            Pokemon.BIDOOF at 1.0f
            Pokemon.KRICKETOT at 1.0f
            Pokemon.SHINX at 1.0f
            Pokemon.PACHIRISU at 1.0f
            Pokemon.BUIZEL at 1.0f
            Pokemon.CHERUBI at 1.0f
            Pokemon.SHELLOS at 1.0f
            Pokemon.BUNEARY at 1.0f
            Pokemon.GLAMEOW at 1.0f
            Pokemon.STUNKY at 1.0f
            Pokemon.BRONZOR at 1.0f
            Pokemon.CARNIVINE at 1.0f
            Pokemon.PATRAT at 1.0f
            Pokemon.LILLIPUP at 1.0f
            Pokemon.PURRLOIN at 1.0f
            Pokemon.PANSAGE at 1.0f
            Pokemon.PANSEAR at 1.0f
            Pokemon.PANPOUR at 1.0f
            Pokemon.MUNNA at 1.0f
            Pokemon.PIDOVE at 1.0f
            Pokemon.BLITZLE at 1.0f
            Pokemon.ROGGENROLA at 1.0f
            Pokemon.WOOBAT at 2.0f during time::night
            Pokemon.AUDINO at 1.0f
            Pokemon.TIMBURR at 1.0f
            Pokemon.TYMPOLE at 1.0f
            Pokemon.SEWADDLE at 1.0f
            Pokemon.VENIPEDE at 1.0f
            Pokemon.COTTONEE at 1.0f
            Pokemon.PETILIL at 1.0f
            Pokemon.SANDILE at 1.0f
            Pokemon.MARACTUS at 1.0f
            Pokemon.DWEBBLE at 1.0f
            Pokemon.SCRAGGY at 1.0f
            Pokemon.YAMASK at 1.0f
            Pokemon.TRUBBISH at 1.0f
            Pokemon.MINCCINO at 1.0f
            Pokemon.GOTHITA at 1.0f
            Pokemon.SOLOSIS at 1.0f
            Pokemon.DUCKLETT at 1.0f
            Pokemon.VANILLITE at 1.0f
            Pokemon.DEERLING at 1.0f
            Pokemon.EMOLGA at 1.0f
            Pokemon.KARRABLAST at 1.0f
            Pokemon.FOONGUS at 1.0f
            Pokemon.JOLTIK at 1.0f
            Pokemon.FERROSEED at 1.0f
            Pokemon.TYNAMO at 1.0f
            Pokemon.ELGYEM at 1.0f
            Pokemon.LITWICK at 2.0f during time::night
            Pokemon.SHELMET at 1.0f
            Pokemon.MIENFOO at 1.0f
            Pokemon.GOLETT at 1.0f
            Pokemon.RUFFLET at 1.0f
            Pokemon.VULLABY at 1.0f
        }

        // Fishing
        // =======
        //
        // Water-bound Pokémon only appear while a fishing rod is in use. Each rod
        // hooks its own set of species, and only one rod can be used at a time.

        pool("Old Rod") {
            activate(70.percent) given { trainer.isUsingItem(Item.OLD_ROD) }

            Pokemon.MAGIKARP at 4.0f
            Pokemon.TENTACOOL at 1.5f
            Pokemon.GOLDEEN at 1.5f
            Pokemon.BARBOACH at 1.0f
            Pokemon.LUVDISC at 1.0f
            Pokemon.FINNEON at 1.0f
            Pokemon.FEEBAS at 0.5f
        }

        pool("Good Rod") {
            activate(70.percent) given { trainer.isUsingItem(Item.GOOD_ROD) }

            Pokemon.SHELLDER at 1.0f
            Pokemon.HORSEA at 1.0f
            Pokemon.CHINCHOU at 1.0f
            Pokemon.REMORAID at 1.0f
            Pokemon.CORPHISH at 1.0f
            Pokemon.CARVANHA at 1.0f
            Pokemon.FRILLISH at 1.0f
            Pokemon.QWILFISH at 0.7f
            Pokemon.CORSOLA at 0.7f
        }

        pool("Super Rod") {
            activate(70.percent) given { trainer.isUsingItem(Item.SUPER_ROD) }

            Pokemon.STARYU at 1.0f
            Pokemon.WAILMER at 1.0f
            Pokemon.CLAMPERL at 1.0f
            Pokemon.ALOMOMOLA at 0.7f
            Pokemon.BASCULIN at 0.7f
            Pokemon.MANTYKE at 0.5f
            Pokemon.RELICANTH at 0.3f
            Pokemon.DRATINI at 0.3f
        }

        // Uncommon & Rare
        // ===============
        //
        // These are rarer spawns with time-based scaling rules. The longer the
        // phone screen stays turned off, the higher the percentage gets.

        pool(
            "Uncommon",
            12.percent increaseBy {
                scaling.timeBoost(
                    gain = 30.percent,
                    over = 60.minutes
                ) + scaling.sleepBonus(5.percent) + (if (trainer.hasRarityBoost) 10.percent else 0f)
            }
        ) {
            Pokemon.PIKACHU at 1.0f
            Pokemon.VULPIX at 1.0f
            Pokemon.JIGGLYPUFF at 1.0f
            Pokemon.PONYTA at 1.0f
            Pokemon.SLOWPOKE at 1.0f
            Pokemon.FARFETCHD at 1.0f
            Pokemon.SEEL at 1.0f
            Pokemon.ONIX at 1.0f
            Pokemon.EXEGGCUTE at 1.0f
            Pokemon.TANGELA at 1.0f
            Pokemon.GROWLITHE at 1.0f
            Pokemon.CUBONE at 1.0f
            Pokemon.LICKITUNG at 1.0f
            Pokemon.RHYHORN at 1.0f
            Pokemon.KANGASKHAN at 1.0f
            Pokemon.CHANSEY at 0.5f
            Pokemon.MR_MIME at 0.5f
            Pokemon.SCYTHER at 0.5f
            Pokemon.JYNX at 0.5f
            Pokemon.ELECTABUZZ at 0.5f
            Pokemon.MAGMAR at 0.5f
            Pokemon.PINSIR at 0.5f
            Pokemon.TAUROS at 1.0f
            Pokemon.EEVEE at 1.0f

            // Johto - Unova
            Pokemon.PICHU at 0.5f
            Pokemon.CLEFFA at 0.5f
            Pokemon.IGGLYBUFF at 0.5f
            Pokemon.TOGEPI at 0.5f
            Pokemon.YANMA at 1.0f
            Pokemon.GIRAFARIG at 1.0f
            Pokemon.GLIGAR at 1.0f
            Pokemon.SNEASEL at 1.0f
            Pokemon.TEDDIURSA at 1.0f
            Pokemon.HOUNDOUR at 1.0f
            Pokemon.PHANPY at 1.0f
            Pokemon.TYROGUE at 0.5f
            Pokemon.SMOOCHUM at 0.5f
            Pokemon.ELEKID at 0.5f
            Pokemon.MAGBY at 0.5f
            Pokemon.AZURILL at 0.5f
            Pokemon.ELECTRIKE at 1.0f
            Pokemon.TORKOAL at 1.0f
            Pokemon.ZANGOOSE at 1.0f
            Pokemon.SEVIPER at 1.0f
            Pokemon.WYNAUT at 0.5f
            Pokemon.BUDEW at 0.5f
            Pokemon.BURMY at 1.0f
            Pokemon.COMBEE at 1.0f
            Pokemon.DRIFLOON at 2.0f during time::night
            Pokemon.CHINGLING at 0.5f
            Pokemon.BONSLY at 0.5f
            Pokemon.MIME_JR at 0.5f
            Pokemon.HAPPINY at 0.5f
            Pokemon.MUNCHLAX at 0.5f
            Pokemon.RIOLU at 0.5f
            Pokemon.HIPPOPOTAS at 1.0f
            Pokemon.SKORUPI at 1.0f
            Pokemon.CROAGUNK at 1.0f
            Pokemon.SNOVER at 1.0f
            Pokemon.DRILBUR at 1.0f
            Pokemon.DARUMAKA at 1.0f
            Pokemon.ZORUA at 2.0f during time::night
            Pokemon.KLINK at 1.0f
            Pokemon.AXEW at 1.0f
            Pokemon.CUBCHOO at 1.0f
            Pokemon.STUNFISK at 1.0f
            Pokemon.PAWNIARD at 1.0f
            Pokemon.HEATMOR at 1.0f
            Pokemon.DURANT at 1.0f
        }

        pool(
            "Rare",
            1.percent increaseBy {
                scaling.timeBoost(
                    gain = 4.percent,
                    over = 90.minutes
                ) + scaling.sleepBonus(3.percent) + (if (events.newYear) 3.percent else 0f) +
                    (if (trainer.hasRarityBoost) 3.percent else 0f)
            }
        ) {
            Pokemon.BULBASAUR at 1f
            Pokemon.CHARMANDER at 1f
            Pokemon.SQUIRTLE at 1f

            Pokemon.ABRA at 1.0f
            Pokemon.DITTO at 1.0f

            // Johto - Unova
            Pokemon.AIPOM at 1.0f
            Pokemon.MURKROW at 1.0f during time::night
            Pokemon.MISDREAVUS at 1.0f during time::night
            Pokemon.HERACROSS at 1.0f
            Pokemon.SKARMORY at 0.5f
            Pokemon.SMEARGLE at 1.0f
            Pokemon.MILTANK at 1.0f
            Pokemon.SABLEYE at 1.0f during time::night
            Pokemon.MAWILE at 1.0f
            Pokemon.LUNATONE at 1.0f
            Pokemon.SOLROCK at 1.0f
            Pokemon.CASTFORM at 1.0f
            Pokemon.CHATOT at 0.5f
            Pokemon.THROH at 1.0f
            Pokemon.SAWK at 1.0f
            Pokemon.SIGILYPH at 1.0f
            Pokemon.CRYOGONAL at 0.5f
            Pokemon.DRUDDIGON at 1.0f
            Pokemon.BOUFFALANT at 1.0f
        }

        // Very rare
        // =========
        //
        // Pseudo-legendaries, starters from other regions and a few signature
        // Pokémon. Maxes out after 2 hours of not unlocking the phone.

        pool(
            "Very Rare",
            0.2.percent increaseBy {
                scaling.timeBoost(
                    gain = 1.3.percent,
                    over = 120.minutes
                ) + scaling.sleepBonus(1.percent) + (if (trainer.hasRarityBoost) 0.5.percent else 0f)
            }
        ) {
            Pokemon.DRATINI at 1.0f
            Pokemon.LARVITAR at 1.0f
            Pokemon.BAGON at 1.0f
            Pokemon.BELDUM at 1.0f
            Pokemon.GIBLE at 1.0f
            Pokemon.DEINO at 1.0f
            Pokemon.LARVESTA at 1.0f

            Pokemon.CHIKORITA at 1.0f
            Pokemon.CYNDAQUIL at 1.0f
            Pokemon.TOTODILE at 1.0f
            Pokemon.TREECKO at 1.0f
            Pokemon.TORCHIC at 1.0f
            Pokemon.MUDKIP at 1.0f
            Pokemon.TURTWIG at 1.0f
            Pokemon.CHIMCHAR at 1.0f
            Pokemon.PIPLUP at 1.0f
            Pokemon.SNIVY at 1.0f
            Pokemon.TEPIG at 1.0f
            Pokemon.OSHAWOTT at 1.0f

            Pokemon.LAPRAS at 1.0f
            Pokemon.PORYGON at 1.0f
            Pokemon.ABSOL at 1.0f
            Pokemon.ROTOM at 1.0f
            Pokemon.SPIRITOMB at 1.0f
        }

        // Fossil events
        // =============
        //
        // As the player progresses their Pokédex, fossil Pokémon appear at
        // various milestones, with either Omanyte or Kabuto appearing first.

        val fossils = listOf(
            Pokemon.OMANYTE, // ༼ つ ◕_◕ ༽つ
            Pokemon.KABUTO
        )

        fossils.forEach { fossil ->
            special(fossil) {
                activate(30.percent) given { trainer.hasNotFoundAny(fossils) && trainer.pokedexCount >= 30 }
                activate(30.percent) given { trainer.hasNotFound(it) && trainer.pokedexCount >= 60 }
            }
        }

        special(Pokemon.AERODACTYL) {
            activate(30.percent) given { trainer.hasNotFound(it) && trainer.pokedexCount >= 90 }
        }

        // Halloween event
        // ===============
        //
        // The Gastly and Zubat lines appear all day during the spooky season,
        // including evolved forms at night.

        pool("Halloween") {
            activate(20.percent) during events::halloween during time::night
            activate(15.percent) during events::halloween

            Pokemon.GASTLY at 1.0f

            Pokemon.HAUNTER at 2.0f during time::night
            Pokemon.GENGAR at 1.0f during time::night
            Pokemon.GOLBAT at 2.0f during time::night
        }

        // Christmas event
        // ===============
        //
        // During the holiday season, Ice-type Pokémon will appear more commonly.
        // Additionally, the Johto Pokémon Stantler and Delibird can each be
        // caught once.

        pool("Christmas") {
            activate(10.percent) during events::christmas

            Pokemon.JYNX at 2.0f
            Pokemon.SEEL at 1.5f
            Pokemon.DEWGONG at 1.0f
            Pokemon.LAPRAS at 0.25f
        }

        special(Pokemon.DELIBIRD) {
            activate(10.percent) during events::christmas given { trainer.hasNotFound(it) }
        }

        special(Pokemon.STANTLER) {
            activate(10.percent) during events::christmas given { trainer.hasNotFound(it) }
        }

        // Valentine's Day
        // ===============
        //
        // Heart-shaped and Fairy-type Pokémon, plus couples like Volbeat and Illumise.

        pool("Valentine's Day") {
            activate(25.percent) during events::valentines

            Pokemon.LUVDISC at 3.0f
            Pokemon.VOLBEAT at 1.0f
            Pokemon.ILLUMISE at 1.0f
            Pokemon.NIDORAN_F at 1.0f
            Pokemon.NIDORAN_M at 1.0f
            Pokemon.CLEFAIRY at 1.0f
            Pokemon.SNUBBULL at 1.0f
            Pokemon.SKITTY at 1.0f
            Pokemon.TOGEPI at 0.5f
            Pokemon.ALOMOMOLA at 0.5f
        }

        // Pokémon Day
        // ===========
        //
        // On the anniversary of the first games, starters from every region show up.

        pool("Pokémon Day") {
            activate(25.percent) during events::pokemonDay

            Pokemon.PIKACHU at 2.0f
            Pokemon.BULBASAUR at 1.0f
            Pokemon.CHARMANDER at 1.0f
            Pokemon.SQUIRTLE at 1.0f
            Pokemon.CHIKORITA at 1.0f
            Pokemon.CYNDAQUIL at 1.0f
            Pokemon.TOTODILE at 1.0f
            Pokemon.TREECKO at 1.0f
            Pokemon.TORCHIC at 1.0f
            Pokemon.MUDKIP at 1.0f
            Pokemon.TURTWIG at 1.0f
            Pokemon.CHIMCHAR at 1.0f
            Pokemon.PIPLUP at 1.0f
            Pokemon.SNIVY at 1.0f
            Pokemon.TEPIG at 1.0f
            Pokemon.OSHAWOTT at 1.0f
        }

        // New Year
        // ========
        //
        // Fire-types and electric "fireworks" on New Year's Eve and Day. Rare Pokémon are
        // also more common (see the Rare pool).

        pool("New Year") {
            activate(20.percent) during events::newYear

            Pokemon.VULPIX at 1.0f
            Pokemon.GROWLITHE at 1.0f
            Pokemon.PONYTA at 1.0f
            Pokemon.SLUGMA at 1.0f
            Pokemon.NUMEL at 1.0f
            Pokemon.LITWICK at 1.0f
            Pokemon.DARUMAKA at 1.0f
            Pokemon.VOLTORB at 1.0f
            Pokemon.ELEKID at 1.0f
            Pokemon.PLUSLE at 1.0f
            Pokemon.MINUN at 1.0f
            Pokemon.EMOLGA at 1.0f
            Pokemon.JOLTIK at 1.0f
        }

        // San Juan
        // ========
        //
        // Midsummer: beach Pokémon during the day, bonfires and the sea at night.

        pool("San Juan") {
            activate(25.percent) during events::sanJuan

            Pokemon.KRABBY at 1.0f during time::day
            Pokemon.STARYU at 1.0f during time::day
            Pokemon.CORPHISH at 1.0f during time::day
            Pokemon.WINGULL at 1.0f during time::day
            Pokemon.SHELLOS at 1.0f during time::day
            Pokemon.PSYDUCK at 1.0f during time::day
            Pokemon.VULPIX at 1.0f during time::night
            Pokemon.GROWLITHE at 1.0f during time::night
            Pokemon.PONYTA at 1.0f during time::night
            Pokemon.TORKOAL at 1.0f during time::night
            Pokemon.CHINCHOU at 1.0f during time::night
            Pokemon.TENTACOOL at 1.0f during time::night
            Pokemon.FRILLISH at 1.0f during time::night
        }

        // Thunderstorm event
        // ==================
        //
        // During thunderstorms, certain Electric-type Pokémon will frequently
        // appear, including evolved forms.

        pool("Thunderstorm") {
            activate(30.percent) during weather::thunderstorm

            Pokemon.PIKACHU at 3.0f
            Pokemon.RAICHU at 1.0f
            Pokemon.MAGNEMITE at 2.0f
            Pokemon.MAGNETON at 1.0f
        }

        // Low battery event
        // =================
        //
        // When the phone battery runs low, certain Electric-type Pokémon will
        // appear, including a guaranteed Electrode spawn below 10% battery.

        special(Pokemon.VOLTORB) {
            activate(20.percent) given { phone.battery < 15 }
        }

        special(Pokemon.ELECTRODE) {
            activate(100.percent) given { trainer.hasNotFound(it) && phone.battery < 10 }
        }

        // Fighters
        // =======
        //
        // Hitmonchan and Hitmonlee spawn based on training dedication (consecutive days
        // with a training partner).

        special(Pokemon.HITMONCHAN) {
            activate(100.percent) given { trainer.currentPartnerDays >= 3 && trainer.hasNotFound(it) }
        }

        special(Pokemon.HITMONLEE) {
            activate(100.percent) given {
                val daysSinceHitmonchan = trainer.daysSinceLastCaught(Pokemon.HITMONCHAN.id)
                trainer.currentPartnerDays >= 3 &&
                    (daysSinceHitmonchan != null && daysSinceHitmonchan >= 3) &&
                    trainer.hasNotFound(it)
            }
        }

        // Full moon event
        // ===============
        //
        // During the monthly full moon, various Fairy-type Pokémon will be common
        // spawns during both day and night.

        pool("Full moon") {
            activate(30.percent) during events::fullMoon

            Pokemon.CLEFAIRY at 5.0f during time::night
            Pokemon.CLEFABLE at 1.0f during time::night
            Pokemon.JIGGLYPUFF at 2.0f during time::day
            Pokemon.WIGGLYTUFF at 2.0f during time::day
        }

        // Snorlax event
        // =============
        //
        // Snorlax appears after N hours of screen off time. A one-time guaranteed
        // spawn occurs after 8 hours of sleep.

        special(Pokemon.SNORLAX) {
            val threshold = 8 * 60
            activate(100.percent) given { trainer.hasNotFound(it) && phone.minutesOff > threshold }
            activate(1.percent) given { phone.minutesOff > threshold }
        }

        // Legendary bird events
        // =====================
        //
        // The legendary birds are one-time spawns with special conditions:
        //
        // - Zapdos spawns during weather::thunderstorms after the player has caught
        //   a number of other Pokémon already. For areas that get few or no
        //   thunderstorms, there will be a fallback item.
        //
        // - Articuno and Moltres have a tiny chance of appearing during season::winter
        //   and summer respectively, after the phone screen has been off for
        //   at least one hour. Chances increase with screen off time.

        special(Pokemon.ZAPDOS) {
            activate(50.percent) during weather::thunderstorm given {
                trainer.hasNotFound(it) && trainer.pokedexCount > 40
            }
        }

        special(Pokemon.ARTICUNO) {
            activate(1.percent increaseBy { scaling.minutes / 60f }) during season::winter given {
                trainer.hasNotFound(it) && scaling.minutes > 30
            }
        }

        special(Pokemon.MOLTRES) {
            activate(1.percent increaseBy { scaling.minutes / 60f }) during season::summer given {
                trainer.hasNotFound(it) && scaling.minutes > 30
            }
        }

        // Mewtwo event
        // ============
        //
        // Mewtwo appears after the player has trained six Pokémon to level 70 or higher.

        special(Pokemon.MEWTWO) {
            activate(10.percent) given {
                trainer.hasNotFound(it) && trainer.countPokemonAtLevel(70) >= 6
            }
        }

        // Mew event
        // =========
        //
        // Mew is a one-time spawn once the Pokédex reaches 300 entries.

        special(Pokemon.MEW) {
            activate(10.percent) given { trainer.hasNotFound(it) && trainer.pokedexCount >= 300 }
        }

        // Hoenn, Sinnoh & Unova fossils
        // =============================
        //
        // One fossil from each pair appears at higher Pokédex milestones, the
        // other one a bit later.

        listOf(
            listOf(Pokemon.LILEEP, Pokemon.ANORITH) to 160,
            listOf(Pokemon.CRANIDOS, Pokemon.SHIELDON) to 260,
            listOf(Pokemon.TIRTOUGA, Pokemon.ARCHEN) to 360
        ).forEach { (pair, milestone) ->
            pair.forEach { fossil ->
                special(fossil) {
                    activate(30.percent) given { trainer.pokedexCount >= milestone && trainer.hasNotFoundAny(pair) }
                    activate(30.percent) given { trainer.pokedexCount >= milestone + 30 && trainer.hasNotFound(it) }
                }
            }
        }

        // Shedinja
        // ========
        //
        // An empty shell is left behind after Nincada evolves into Ninjask.

        special(Pokemon.SHEDINJA) {
            activate(20.percent) given { trainer.hasNotFound(it) && !trainer.hasNotFound(Pokemon.NINJASK) }
        }

        // Extra spooky & stormy spawns
        // ============================

        pool("Halloween (Johto-Unova)") {
            activate(10.percent) during events::halloween

            Pokemon.MISDREAVUS at 1.0f
            Pokemon.SHUPPET at 1.0f
            Pokemon.DUSKULL at 1.0f
            Pokemon.DRIFLOON at 1.0f
            Pokemon.LITWICK at 1.0f
            Pokemon.SPIRITOMB at 0.2f during time::night
        }

        pool("Thunderstorm (Johto-Unova)") {
            activate(15.percent) during weather::thunderstorm

            Pokemon.MAREEP at 2.0f
            Pokemon.ELECTRIKE at 2.0f
            Pokemon.SHINX at 2.0f
            Pokemon.BLITZLE at 2.0f
            Pokemon.JOLTIK at 1.0f
            Pokemon.ROTOM at 0.3f
        }

        // Johto legendaries
        // =================

        special(Pokemon.RAIKOU) {
            activate(30.percent) during weather::thunderstorm given {
                trainer.pokedexCount >= 100 && trainer.hasNotFound(it)
            }
        }

        special(Pokemon.ENTEI) {
            activate(5.percent) during weather::clear during time::day given {
                trainer.pokedexCount >= 120 && phone.minutesOff > 60 && trainer.hasNotFound(it)
            }
        }

        special(Pokemon.SUICUNE) {
            activate(10.percent) during weather::rain given {
                trainer.pokedexCount >= 120 && trainer.hasNotFound(it)
            }
        }

        special(Pokemon.LUGIA) {
            activate(20.percent) during weather::rain during time::night given {
                trainer.pokedexCount >= 180 && phone.minutesOff > 120 && trainer.hasNotFound(it)
            }
        }

        special(Pokemon.HO_OH) {
            activate(30.percent) during weather::clear during time::day given {
                trainer.hasNotFound(it) &&
                    !trainer.hasNotFound(Pokemon.RAIKOU) &&
                    !trainer.hasNotFound(Pokemon.ENTEI) &&
                    !trainer.hasNotFound(Pokemon.SUICUNE)
            }
        }

        special(Pokemon.CELEBI) {
            activate(10.percent) during season::spring given {
                trainer.pokedexCount >= 240 && trainer.hasNotFound(it)
            }
            activate(2.percent) given { trainer.pokedexCount >= 250 && trainer.hasNotFound(it) }
        }

        // Hoenn legendaries
        // =================

        special(Pokemon.REGIROCK) {
            activate(5.percent) during weather::clear given {
                trainer.pokedexCount >= 200 && phone.minutesOff > 60 && trainer.hasNotFound(it)
            }
        }

        special(Pokemon.REGICE) {
            activate(10.percent) during weather::snow given {
                trainer.pokedexCount >= 200 && trainer.hasNotFound(it)
            }
            activate(2.percent) during season::winter given {
                trainer.pokedexCount >= 200 && phone.minutesOff > 60 && trainer.hasNotFound(it)
            }
        }

        special(Pokemon.REGISTEEL) {
            activate(5.percent) during weather::cloudy given {
                trainer.pokedexCount >= 200 && phone.minutesOff > 60 && trainer.hasNotFound(it)
            }
        }

        special(Pokemon.LATIAS) {
            activate(10.percent) during time::day given {
                trainer.pokedexCount >= 220 && trainer.currentPartnerDays >= 5 && trainer.hasNotFound(it)
            }
        }

        special(Pokemon.LATIOS) {
            activate(10.percent) during time::night given {
                trainer.pokedexCount >= 220 && trainer.currentPartnerDays >= 5 && trainer.hasNotFound(it)
            }
        }

        special(Pokemon.KYOGRE) {
            activate(20.percent) during weather::rain given {
                trainer.pokedexCount >= 300 && phone.minutesOff > 90 && trainer.hasNotFound(it)
            }
        }

        special(Pokemon.GROUDON) {
            activate(20.percent) during weather::clear during time::day given {
                trainer.pokedexCount >= 300 && phone.minutesOff > 90 && trainer.hasNotFound(it)
            }
        }

        special(Pokemon.RAYQUAZA) {
            activate(20.percent) given {
                trainer.hasNotFound(it) &&
                    !trainer.hasNotFound(Pokemon.KYOGRE) &&
                    !trainer.hasNotFound(Pokemon.GROUDON)
            }
        }

        special(Pokemon.JIRACHI) {
            activate(10.percent) during weather::clear during time::night given {
                trainer.pokedexCount >= 250 && trainer.hasNotFound(it)
            }
        }

        special(Pokemon.DEOXYS) {
            activate(5.percent) during weather::clear during time::night given {
                trainer.pokedexCount >= 380 && trainer.hasNotFound(it)
            }
        }

        // Sinnoh legendaries
        // ==================

        special(Pokemon.UXIE) {
            activate(20.percent) given {
                trainer.pokedexCount >= 250 && trainer.countPokemonAtLevel(50) >= 3 && trainer.hasNotFound(it)
            }
        }

        special(Pokemon.MESPRIT) {
            activate(20.percent) during events::fullMoon given {
                trainer.pokedexCount >= 250 && trainer.hasNotFound(it)
            }
        }

        special(Pokemon.AZELF) {
            activate(20.percent) given {
                trainer.pokedexCount >= 250 && trainer.currentPartnerDays >= 7 && trainer.hasNotFound(it)
            }
        }

        val lakeTrio = listOf(Pokemon.UXIE, Pokemon.MESPRIT, Pokemon.AZELF)

        special(Pokemon.DIALGA) {
            activate(20.percent) during time::day given {
                phone.minutesOff > 120 && trainer.hasNotFound(it) && lakeTrio.none { trainer.hasNotFound(it) }
            }
        }

        special(Pokemon.PALKIA) {
            activate(20.percent) during time::night given {
                phone.minutesOff > 120 && trainer.hasNotFound(it) && lakeTrio.none { trainer.hasNotFound(it) }
            }
        }

        special(Pokemon.GIRATINA) {
            activate(30.percent) during events::halloween during time::night given {
                trainer.hasNotFound(it) &&
                    !trainer.hasNotFound(Pokemon.DIALGA) &&
                    !trainer.hasNotFound(Pokemon.PALKIA)
            }
            activate(3.percent) during time::night given {
                trainer.pokedexCount >= 420 && trainer.hasNotFound(it)
            }
        }

        special(Pokemon.HEATRAN) {
            activate(10.percent) during season::summer during weather::clear given {
                trainer.pokedexCount >= 350 && trainer.hasNotFound(it)
            }
            activate(1.percent) during weather::clear given {
                trainer.pokedexCount >= 400 && trainer.hasNotFound(it)
            }
        }

        special(Pokemon.REGIGIGAS) {
            activate(20.percent) given {
                trainer.hasNotFound(it) &&
                    listOf(Pokemon.REGIROCK, Pokemon.REGICE, Pokemon.REGISTEEL).none { trainer.hasNotFound(it) }
            }
        }

        special(Pokemon.CRESSELIA) {
            activate(20.percent) during events::fullMoon during time::night given {
                trainer.pokedexCount >= 380 && trainer.hasNotFound(it)
            }
        }

        special(Pokemon.DARKRAI) {
            activate(10.percent) during sleep::isBedtime given {
                trainer.hasNotFound(it) && !trainer.hasNotFound(Pokemon.CRESSELIA)
            }
        }

        special(Pokemon.MANAPHY) {
            activate(10.percent) given {
                trainer.pokedexCount >= 400 && trainer.isUsingItem(Item.SUPER_ROD) && trainer.hasNotFound(it)
            }
        }

        special(Pokemon.PHIONE) {
            activate(5.percent) given { trainer.isUsingItem(Item.SUPER_ROD) && !trainer.hasNotFound(Pokemon.MANAPHY) }
        }

        special(Pokemon.SHAYMIN) {
            activate(10.percent) during season::spring during weather::clear given {
                trainer.pokedexCount >= 400 && trainer.hasNotFound(it)
            }
            activate(1.percent) during weather::clear during time::day given {
                trainer.pokedexCount >= 450 && trainer.hasNotFound(it)
            }
        }

        special(Pokemon.ARCEUS) {
            activate(10.percent) given { trainer.pokedexCount >= 490 && trainer.hasNotFound(it) }
        }

        // Unova legendaries
        // =================

        special(Pokemon.VICTINI) {
            activate(20.percent) given { trainer.countPokemonAtLevel(100) >= 1 && trainer.hasNotFound(it) }
        }

        special(Pokemon.COBALION) {
            activate(10.percent) during weather::cloudy given {
                trainer.pokedexCount >= 450 && trainer.hasNotFound(it)
            }
        }

        special(Pokemon.TERRAKION) {
            activate(10.percent) during weather::clear given {
                trainer.pokedexCount >= 450 && trainer.hasNotFound(it)
            }
        }

        special(Pokemon.VIRIZION) {
            activate(10.percent) during weather::rain given {
                trainer.pokedexCount >= 450 && trainer.hasNotFound(it)
            }
        }

        special(Pokemon.TORNADUS) {
            activate(10.percent) during weather::cloudy given {
                trainer.pokedexCount >= 470 && phone.minutesOff > 120 && trainer.hasNotFound(it)
            }
        }

        special(Pokemon.THUNDURUS) {
            activate(30.percent) during weather::thunderstorm given {
                trainer.pokedexCount >= 470 && trainer.hasNotFound(it)
            }
            activate(1.percent) during weather::rain given {
                trainer.pokedexCount >= 520 && trainer.hasNotFound(it)
            }
        }

        special(Pokemon.LANDORUS) {
            activate(20.percent) given {
                trainer.hasNotFound(it) &&
                    !trainer.hasNotFound(Pokemon.TORNADUS) &&
                    !trainer.hasNotFound(Pokemon.THUNDURUS)
            }
        }

        special(Pokemon.RESHIRAM) {
            activate(10.percent) during time::day given {
                trainer.pokedexCount >= 500 && trainer.countPokemonAtLevel(70) >= 3 && trainer.hasNotFound(it)
            }
        }

        special(Pokemon.ZEKROM) {
            activate(10.percent) during time::night given {
                trainer.pokedexCount >= 500 && trainer.countPokemonAtLevel(70) >= 3 && trainer.hasNotFound(it)
            }
        }

        special(Pokemon.KYUREM) {
            activate(30.percent) during weather::snow given {
                trainer.hasNotFound(it) &&
                    !trainer.hasNotFound(Pokemon.RESHIRAM) &&
                    !trainer.hasNotFound(Pokemon.ZEKROM)
            }
            activate(5.percent) during season::winter given {
                trainer.hasNotFound(it) &&
                    !trainer.hasNotFound(Pokemon.RESHIRAM) &&
                    !trainer.hasNotFound(Pokemon.ZEKROM)
            }
        }

        special(Pokemon.KELDEO) {
            activate(20.percent) during weather::rain given {
                trainer.hasNotFound(it) &&
                    listOf(Pokemon.COBALION, Pokemon.TERRAKION, Pokemon.VIRIZION).none { trainer.hasNotFound(it) }
            }
        }

        special(Pokemon.MELOETTA) {
            activate(10.percent) during events::fullMoon given {
                trainer.pokedexCount >= 550 && trainer.hasNotFound(it)
            }
        }

        special(Pokemon.GENESECT) {
            activate(10.percent) given { trainer.pokedexCount >= 600 && trainer.hasNotFound(it) }
        }
    }
}
