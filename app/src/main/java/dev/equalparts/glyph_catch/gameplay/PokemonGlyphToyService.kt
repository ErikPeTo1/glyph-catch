package dev.equalparts.glyph_catch.gameplay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import android.text.format.DateFormat
import android.util.Log
import androidx.core.content.edit
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixObject
import dev.equalparts.glyph_catch.Features
import dev.equalparts.glyph_catch.data.CaughtPokemon
import dev.equalparts.glyph_catch.data.Eggs
import dev.equalparts.glyph_catch.data.Forms
import dev.equalparts.glyph_catch.data.FriendshipDaily
import dev.equalparts.glyph_catch.data.FriendshipRules
import dev.equalparts.glyph_catch.data.GardenCatalog
import dev.equalparts.glyph_catch.data.GardenStore
import dev.equalparts.glyph_catch.data.GardenZone
import dev.equalparts.glyph_catch.data.Item
import dev.equalparts.glyph_catch.data.PokedexData
import dev.equalparts.glyph_catch.data.Pokemon
import dev.equalparts.glyph_catch.data.PokemonDatabase
import dev.equalparts.glyph_catch.data.PreferencesManager
import dev.equalparts.glyph_catch.data.Stat
import dev.equalparts.glyph_catch.data.Type
import dev.equalparts.glyph_catch.data.creditTeamFriendship
import dev.equalparts.glyph_catch.data.types
import dev.equalparts.glyph_catch.debug.DebugCaptureManager
import dev.equalparts.glyph_catch.debug.DebugExceptionTracker
import dev.equalparts.glyph_catch.debug.DebugSnapshot
import dev.equalparts.glyph_catch.gameplay.animation.AnimationCoordinator
import dev.equalparts.glyph_catch.gameplay.animation.GlyphMatrixHelper
import dev.equalparts.glyph_catch.gameplay.spawner.GameplayContext
import dev.equalparts.glyph_catch.gameplay.spawner.SpawnCadenceController
import dev.equalparts.glyph_catch.gameplay.spawner.SpawnContext
import dev.equalparts.glyph_catch.gameplay.spawner.SpawnHistoryTracker
import dev.equalparts.glyph_catch.gameplay.spawner.SpawnResult
import dev.equalparts.glyph_catch.gameplay.spawner.SpawnRulesEngine
import dev.equalparts.glyph_catch.gameplay.spawner.createSpawnRules
import dev.equalparts.glyph_catch.util.EventHelpers
import dev.equalparts.glyph_catch.util.GlyphDevice
import dev.equalparts.glyph_catch.util.GlyphMatrixService
import dev.equalparts.glyph_catch.util.findLevelEvolutionTarget
import dev.equalparts.glyph_catch.widget.WidgetUpdater
import dev.equalparts.glyph_catch.widget.decodeQueue
import dev.equalparts.glyph_catch.widget.widgetJson
import java.util.Calendar
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Simplified spawn data for persistence.
 */
@Serializable
data class PersistentSpawn(
    val pokemonId: Int,
    val poolName: String,
    val isSpecial: Boolean,
    val isConditional: Boolean,
    val screenOffDurationMinutes: Int,
    val spawnedAtMillis: Long = System.currentTimeMillis(),
    val isShiny: Boolean = false,
    val form: String? = null
)

/**
 * The interactive Glyph Toy.
 */
class PokemonGlyphToyService : GlyphMatrixService("Pokemon-Glyph-Toy") {

    private var coroutineScope: CoroutineScope? = null

    /** Blinks a shiny's dot around the Poké Ball; stopped whenever the matrix changes. */
    private var dotsJob: Job? = null
    private val lifecycleLogScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private lateinit var db: PokemonDatabase
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var frameFactory: GlyphMatrixHelper
    private lateinit var animationCoordinator: AnimationCoordinator

    private lateinit var gameplayContext: GameplayContext
    private lateinit var spawnEngine: SpawnRulesEngine
    private lateinit var spawnHistory: SpawnHistoryTracker
    private lateinit var cadenceController: SpawnCadenceController

    private var localClockJob: Job? = null
    private var aodActive = false
    private var lastTickMinute = -1L

    private val animationWakeLockMutex = Any()
    private var animationWakeLock: PowerManager.WakeLock? = null
    private var activeAnimationWakeLockHolders = 0

    private val spawnQueue = mutableListOf<SpawnResult>()
    private var displayedSpawn: SpawnResult? = null

    private lateinit var debugCapture: DebugCaptureManager
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        handleCoroutineException(throwable)
    }

    /**
     * Called by the system when the service is first created.
     */
    override fun onCreate() {
        super.onCreate()
        DebugExceptionTracker.install(applicationContext)
        // Genders, for evolutions only one gender takes
        PokedexData.init(applicationContext)

        coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob() + coroutineExceptionHandler)

        db = PokemonDatabase.getInstance(applicationContext)
        preferencesManager = PreferencesManager(applicationContext)
        frameFactory = GlyphMatrixHelper(applicationContext, GLYPH_MATRIX_SIZE)
        animationCoordinator = AnimationCoordinator(
            glyphFrameHelper = frameFactory,
            glyphMatrixManagerProvider = { glyphMatrixManager ?: error("GlyphMatrixManager not connected") },
            animationDispatcher = Dispatchers.Main.immediate,
            onAnimationStart = {
                dotsJob?.cancel()
                acquireAnimationWakeLock()
            },
            onAnimationEnd = { releaseAnimationWakeLock() }
        )

        val weatherProvider = WeatherProviderFactory.create(applicationContext)
        gameplayContext = GameplayContext(applicationContext, weatherProvider, spawnQueue)
        spawnEngine = SpawnRulesEngine(
            rules = createSpawnRules(gameplayContext),
            speciesFilter = { gameplayContext.trainer.canSpawn(it.id) },
            shinyMultiplier = { EventHelpers.shinyMultiplier(it.id, gameplayContext.trainer.catchCounts[it.id] ?: 0) }
        )
        spawnHistory = SpawnHistoryTracker(preferences = preferencesManager, pokemonDao = db.pokemonDao())
        cadenceController = SpawnCadenceController(spawnEngine = spawnEngine, history = spawnHistory)

        val powerManager = applicationContext.getSystemService(PowerManager::class.java)
        animationWakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            WAKE_LOCK_TAG
        )

        debugCapture = DebugCaptureManager.shared(applicationContext)

        registerReceiver(unlockReceiver, IntentFilter(Intent.ACTION_USER_PRESENT), RECEIVER_NOT_EXPORTED)
        instance = this
        logLifecycle("toy_created")
    }

    /**
     * Notes when the system starts, connects, disconnects or stops the toy. Runs on its own
     * scope so it still gets written while the service is being destroyed.
     */
    private fun logLifecycle(event: String) {
        if (!::gameplayContext.isInitialized) return
        val snapshot = runCatching { currentDebugSnapshot() }.getOrElse { DebugSnapshot.EMPTY }
        val aod = aodActive
        lifecycleLogScope.launch {
            debugCapture.log(event, snapshot) { buildJsonObject { put("aodActive", JsonPrimitive(aod)) } }
        }
    }

    /**
     * Notices every unlock, even ones that happen between two minute ticks.
     */
    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (::gameplayContext.isInitialized) {
                gameplayContext.phone.unlockedSinceLastTick = true
            }
        }
    }

    /**
     * Called by the system when the service is no longer used and is being removed.
     */
    override fun onDestroy() {
        logLifecycle("toy_destroyed")
        super.onDestroy()
        if (instance === this) instance = null
        runCatching { unregisterReceiver(unlockReceiver) }
        if (::animationCoordinator.isInitialized) {
            animationCoordinator.cancelActive()
        }
        localClockJob?.cancel()
        localClockJob = null
        coroutineScope?.cancel()
        coroutineScope = null
        synchronized(animationWakeLockMutex) {
            animationWakeLock?.let { wakeLock ->
                if (wakeLock.isHeld) {
                    runCatching { wakeLock.release() }
                        .onFailure { error -> Log.w(TAG, "Unable to release animation wake lock on destroy", error) }
                }
            }
            animationWakeLock = null
            activeAnimationWakeLockHolders = 0
        }
    }

    /**
     * Called by the system when the Glyph Toy is activated.
     */
    override fun performOnServiceConnected(context: Context, glyphMatrixManager: GlyphMatrixManager) {
        logLifecycle("toy_connected")
        runBlocking { HiddenCatchUp.run(applicationContext, fromToy = true) }
        restoreSpawnQueue()
        aodActive = false
        tick()
        startLocalClock()
    }

    /**
     * Called by the system when the Glyph Toy is deactivated.
     */
    override fun performOnServiceDisconnected(context: Context) {
        logLifecycle("toy_disconnected")
        super.performOnServiceDisconnected(context)
        localClockJob?.cancel()
        localClockJob = null
        saveSpawnQueue()
    }

    /**
     * Called by the system at every full minute mark: 10:30:00, 10:31:00, etc.
     */
    override fun onAodTick() {
        if (!aodActive) {
            aodActive = true
            logLifecycle("aod_started")
        }
        tick()
    }

    /**
     * Starts a local clock that ticks every minute. It stays on with AOD too, as a backup for the
     * system's minute ticks.
     */
    private fun startLocalClock() {
        localClockJob?.cancel()
        localClockJob = coroutineScope?.launch(Dispatchers.Main) {
            while (true) {
                val calendar = Calendar.getInstance()
                val secondsUntilNextMinute = 60 - calendar.get(Calendar.SECOND)
                val millisUntilNextMinute = secondsUntilNextMinute * 1000L - calendar.get(Calendar.MILLISECOND)

                delay(millisUntilNextMinute)
                tick()
            }
        }
    }

    /**
     * Updates the game / display state. Might spawn a new Pokémon.
     */
    private fun tick() {
        // The AOD tick and the local clock can both fire in the same minute
        val minute = System.currentTimeMillis() / 60_000L
        if (minute == lastTickMinute) return
        lastTickMinute = minute
        preferencesManager.lastToyTickAt = System.currentTimeMillis()

        gameplayContext.phone.refresh()
        gameplayContext.trainer.refresh()
        // Stats only count real time without unlocking, even with "play while in use" on
        if (!gameplayContext.phone.isInteractive) {
            preferencesManager.dailyStats.add(Stat.MINUTES)
        }
        if (gameplayContext.phone.countsAsAway) {
            coroutineScope?.launch { advanceEggs() }
            // Berries in the Garden grow like Eggs hatch: while the phone is put away
            if (Features.TOWN) coroutineScope?.launch { growGarden() }
        } else {
            spawnHistory.resetAfterScreenOn()
        }

        if (gameplayContext.sleep.hasSleepBonus) {
            val now = System.currentTimeMillis()
            if (preferencesManager.sleepBonusExpiresAt <= now) {
                preferencesManager.sleepBonusExpiresAt = now + SLEEP_BONUS_DURATION_MILLIS
            }
        } else if (preferencesManager.sleepBonusExpiresAt != 0L) {
            preferencesManager.sleepBonusExpiresAt = 0L
        }

        coroutineScope?.launch {
            db.activeItemDao().cleanupExpiredItems()
        }

        if (!preferencesManager.glyphToyHasTicked) {
            preferencesManager.glyphToyHasTicked = true
        }

        val now = System.currentTimeMillis()
        val spawnContext = SpawnContext(
            hasQueuedSpawns = spawnQueue.isNotEmpty(),
            phoneIsInteractive = !gameplayContext.phone.countsAsAway,
            phoneMinutesOff = gameplayContext.phone.minutesOff,
            trainerPokedexCount = gameplayContext.trainer.pokedexCount,
            isBedtime = gameplayContext.sleep.isBedtime,
            fastSpawns = preferencesManager.playWhileInUse,
            incense = gameplayContext.trainer.isUsingItem(Item.INCENSE)
        )

        if (!spawnContext.phoneIsInteractive) {
            val minutesOffSnapshot = gameplayContext.phone.minutesOff
            val isBedtimeSnapshot = gameplayContext.sleep.isBedtime
            coroutineScope?.launch {
                applyTrainingExp(minutesOffSnapshot, isBedtimeSnapshot)
            }
        }

        val decision = cadenceController.maybeSpawn(now, spawnContext)
        var spawned = decision.spawn
        if (spawned != null && runBlocking { isRepelled(spawned, preferencesManager, db) }) {
            spawned = null
        }

        if (spawned != null) {
            addToQueue(spawned.copy(form = Forms.formFor(spawned.pokemon.id, gameplayContext.formConditions())))
        }

        coroutineScope?.launch {
            debugCapture.log("tick", currentDebugSnapshot()) {
                buildJsonObject {
                    put(
                        "decision",
                        buildJsonObject {
                            put("reason", JsonPrimitive(decision.debug.reason.name.lowercase(Locale.US)))
                            put("minutesAccumulated", JsonPrimitive(decision.debug.minutesAccumulated))
                            put("baseChance", JsonPrimitive(decision.debug.baseChance))
                            decision.debug.roll?.let { put("roll", JsonPrimitive(it)) }
                            decision.debug.initialPool?.let { put("initialPool", JsonPrimitive(it)) }
                            decision.debug.finalPool?.let { put("finalPool", JsonPrimitive(it)) }
                            decision.debug.rerollTargetPool?.let { put("rerollTargetPool", JsonPrimitive(it)) }
                            put("rerollsRequested", JsonPrimitive(decision.debug.rerollsRequested))
                            put("rerollsUsed", JsonPrimitive(decision.debug.rerollsUsed))
                        }
                    )
                    spawned?.let { spawn ->
                        put(
                            "spawn",
                            buildJsonObject {
                                put("pokemonId", JsonPrimitive(spawn.pokemon.id))
                                put("pokemonName", JsonPrimitive(spawn.pokemon.name))
                                put("pool", JsonPrimitive(spawn.pool.name))
                                put("screenOffMinutes", JsonPrimitive(spawn.screenOffDurationMinutes))
                            }
                        )
                    }
                }
            }
        }

        updateGlyphMatrix()
    }

    /**
     * Runs during ticks to level and/or evolve Pokémon.
     */
    private val friendshipDaily by lazy { FriendshipDaily(applicationContext) }

    private suspend fun applyTrainingExp(minutesOff: Int, isBedtime: Boolean) {
        val dao = db.pokemonDao()
        val active = dao.getActiveTrainingPartner()
        val intervalBonus = if (!isBedtime && minutesOff > 0 && minutesOff % TRAINING_EXP_BONUS_INTERVAL_MINUTES == 0) {
            TRAINING_EXP_BONUS_AMOUNT
        } else {
            0
        }
        val gainedExp = TRAINING_EXP_PER_MINUTE + intervalBonus
        active?.let {
            giveExp(it, gainedExp)
            if (friendshipDaily.trainMinute(it.id)) dao.addFriendship(it.id, 1)
        }
        creditTeamFriendship(applicationContext, db)

        // With the Exp. Share on, the rest of the team earns a bit less than the training partner
        val expShare = Item.EXP_SHARE in gameplayContext.trainer.ownedItems && preferencesManager.expShareEnabled
        if (!expShare) return
        preferencesManager.team
            .filter { it != active?.id }
            .mapNotNull { dao.getCaughtPokemon(it) }
            .forEach { giveExp(it, gainedExp * EXP_SHARE_PERCENT / 100) }
    }

    private suspend fun giveExp(pokemon: CaughtPokemon, amount: Int) {
        val exp = FriendshipRules.withExpBonus(pokemon.friendship, amount)
        val result = LevelCalculator.expResult(pokemon.level, pokemon.exp, exp) ?: return
        db.pokemonDao().updateTrainingProgress(pokemon.id, result.exp, result.level)
        if (result.leveledUp) {
            val levels = result.level - pokemon.level
            val friendship = FriendshipRules.afterLevelUps(pokemon.friendship, levels)
            db.pokemonDao().updateFriendship(pokemon.id, friendship)
            preferencesManager.dailyStats.add(Stat.LEVELS, levels)
            maybeTriggerEvolution(pokemon.copy(level = result.level, exp = result.exp, friendship = friendship))
        }
    }

    /**
     * Called when a Pokémon levels up to trigger an evolution when needed.
     */
    private suspend fun maybeTriggerEvolution(pokemon: CaughtPokemon) {
        val species = Pokemon[pokemon.speciesId] ?: return
        val target = findLevelEvolutionTarget(pokemon) ?: return
        db.pokemonDao().evolveCaught(pokemon, target.id, pokemon.level, pokemon.exp)
        preferencesManager.dailyStats.add(Stat.EVOLUTIONS)
        preferencesManager.enqueueEvolutionNotification(
            previousSpeciesId = species.id,
            newSpeciesId = target.id,
            shiny = pokemon.isShiny
        )
    }

    /**
     * The town works while the phone is locked: berries grow in the Orchard (watered by its
     * Water-types, faster with a Grass-type) and the Hot Springs warm up their residents'
     * friendship and, with anyone there, Eggs hatch half as fast again.
     */
    private suspend fun growGarden() {
        val store = GardenStore.get(this)
        val state = store.state.value
        val dao = db.pokemonDao()
        val minute = System.currentTimeMillis() / 60_000L
        val orchard = state.residents(GardenZone.ORCHARD).mapNotNull { dao.getCaughtPokemon(it) }
        store.grow(
            waterer = orchard.any(GardenCatalog::waters),
            fast = orchard.any { Pokemon[it.speciesId]?.types?.contains(Type.GRASS) == true },
            minute = minute
        )
        val springs = state.residents(GardenZone.SPRINGS).mapNotNull { dao.getCaughtPokemon(it) }
        if (springs.isEmpty()) return
        val daily = FriendshipDaily(this)
        springs.forEach { if (daily.springsMinute(it.id)) dao.addFriendship(it.id, 1) }
        if (minute % 2 == 0L) db.eggDao().advanceEggs()
    }

    /**
     * Eggs advance one minute per locked minute; hatched ones appear on the Glyph Matrix.
     */
    private suspend fun advanceEggs() {
        val eggDao = db.eggDao()
        eggDao.advanceEggs()
        val hatched = eggDao.getHatched()
        if (hatched.isEmpty()) return
        val eggPool = spawnEngine.rules.pools.first { it.name == Eggs.POOL_NAME }
        hatched.forEach { egg ->
            eggDao.deleteEgg(egg.id)
            val species = Pokemon[egg.speciesId] ?: return@forEach
            addToQueue(
                SpawnResult(
                    pokemon = species,
                    pool = eggPool,
                    screenOffDurationMinutes = gameplayContext.phone.minutesOff,
                    spawnedAtMillis = System.currentTimeMillis(),
                    isShiny = Random.nextInt(Eggs.shinyOdds()) == 0
                )
            )
        }
        withContext(Dispatchers.Main) { updateGlyphMatrix() }
    }

    /**
     * Adds a spawn to the queue, removing oldest non-special spawns if needed.
     */
    private fun addToQueue(spawn: SpawnResult) {
        var removed: SpawnResult? = null

        synchronized(spawnQueue) {
            if (spawnQueue.size >= maxQueueSize(preferencesManager)) {
                // Legendaries, shinies and hatched eggs are never pushed out
                val indexToRemove = spawnQueue.indexOfFirst {
                    !it.pool.isSpecial && !it.isShiny && it.pool.name != Eggs.POOL_NAME
                }
                if (indexToRemove != -1) {
                    removed = spawnQueue.removeAt(indexToRemove)
                    Log.d(TAG, "Removed ${removed.pokemon.name} from queue (not special)")
                } else {
                    Log.d(TAG, "Queue full of special spawns, not adding ${spawn.pokemon.name}")
                    return
                }
            }

            spawnQueue.add(spawn)
            Log.d(TAG, "Spawned ${spawn.pokemon.name} from ${spawn.pool.name} pool")
            sortQueueByRarity()
            spawnHistory.updateActiveQueue(spawnQueue, newSpawn = spawn)
        }

        saveSpawnQueue()

        val snapshot = currentDebugSnapshot()

        coroutineScope?.launch {
            removed?.let { removedSpawn ->
                debugCapture.log("queue_evicted", snapshot) {
                    buildJsonObject {
                        put("reason", JsonPrimitive("fifo_overflow"))
                        put("removedPokemonId", JsonPrimitive(removedSpawn.pokemon.id))
                        put("removedPokemonName", JsonPrimitive(removedSpawn.pokemon.name))
                        put("removedPool", JsonPrimitive(removedSpawn.pool.name))
                    }
                }
            }

            debugCapture.log("spawn_enqueued", snapshot) {
                buildJsonObject {
                    put("pokemonId", JsonPrimitive(spawn.pokemon.id))
                    put("pokemonName", JsonPrimitive(spawn.pokemon.name))
                    put("pool", JsonPrimitive(spawn.pool.name))
                }
            }
        }
    }

    /**
     * Sorts the spawn queue by rarity. Ensures the most interesting Pokémon is visible right away.
     */
    private fun sortQueueByRarity() {
        spawnQueue.sortWith(
            compareBy(
                { !it.pool.isSpecial }, // Special spawns first (false < true)
                { !it.pool.isConditional }, // Then conditional spawns
                { !it.isShiny }, // Then shinies
                { it.pool.basePercentage }, // Then rarest pools first
                { -it.pokemon.id } // Then by descending Pokédex number
            )
        )
    }

    /**
     * Shows the current Pokémon or digital clock on the Glyph Matrix.
     */
    private fun updateGlyphMatrix() {
        dotsJob?.cancel()
        dotsJob = null
        val currentSpawn = synchronized(spawnQueue) {
            spawnQueue.firstOrNull()
        }

        if (::animationCoordinator.isInitialized && animationCoordinator.isAnimating) {
            return
        }

        val lowerBrightness = preferencesManager.glyphToyLowerBrightness
        val brightnessFactor = if (lowerBrightness) LOWER_BRIGHTNESS_FACTOR else null

        if (GlyphDevice.pokeBallMode(preferencesManager) && showPokeBall(brightnessFactor)) return

        if (currentSpawn != null) {
            if (displayedSpawn !== currentSpawn) {
                val scope = coroutineScope
                // No flashing on the back of the phone while the player sleeps
                if (scope != null && !gameplayContext.sleep.isBedtime) {
                    animationCoordinator.playSpawn(
                        scope = scope,
                        spawn = currentSpawn,
                        brightnessFactor = brightnessFactor,
                        onDisplayed = { displayedSpawn = it }
                    )
                } else {
                    showPokemon(currentSpawn, brightnessFactor)
                }
            } else {
                showPokemon(currentSpawn, brightnessFactor)
            }
        } else {
            displayedSpawn = null
            val companion = gameplayContext.trainer.companion
            val idle = preferencesManager.glyphIdleDisplay
            when {
                idle == PreferencesManager.IDLE_CLOCK -> showDigitalWatch()
                // Dimmer than wild Pokémon, so it's never mistaken for one
                idle == PreferencesManager.IDLE_COMPANION && companion != null -> animationCoordinator.showPokemon(
                    pokemonId = companion.speciesId,
                    brightnessFactor = COMPANION_BRIGHTNESS * (brightnessFactor ?: 1f),
                    isShiny = companion.isShiny,
                    form = companion.form
                )
                else -> clearGlyphMatrix()
            }
        }
    }

    /**
     * Without a button to catch with, a Poké Ball tells that someone is waiting (negative for a
     * shiny); they're caught from the app or the widgets. Returns whether it drew anything.
     */
    private fun showPokeBall(brightnessFactor: Float?): Boolean {
        val waiting = synchronized(spawnQueue) { spawnQueue.toList() }
        if (waiting.isEmpty()) {
            // Nothing fits the small matrix when idle; elsewhere the usual clock or companion
            if (!GlyphDevice.isPhone4aPro) return false
            displayedSpawn = null
            clearGlyphMatrix()
            return true
        }
        displayedSpawn = waiting.first()
        val ball = frameFactory.renderPokeBallFrame(inverted = waiting.any { it.isShiny })
        // A dot per Pokémon waiting; a shiny's dot blinks
        val shiny = waiting.take(8).indexOfFirst { it.isShiny }.takeIf { it >= 0 }
        fun draw(blinkOn: Boolean) {
            val frame = frameFactory.withWaitingDots(ball, waiting.size, shiny, blinkOn)
            glyphMatrixManager?.setMatrixFrame(
                brightnessFactor?.let { frameFactory.adjustBrightness(frame, it) } ?: frame
            )
        }
        draw(blinkOn = true)
        if (shiny != null) {
            dotsJob = coroutineScope?.launch {
                var on = true
                while (isActive) {
                    delay(DOT_BLINK_MS)
                    on = !on
                    draw(on)
                }
            }
        }
        return true
    }

    /**
     * Show the sprite of a spawned Pokémon on the Glyph Matrix.
     */
    private fun showPokemon(spawn: SpawnResult, brightnessFactor: Float?) {
        animationCoordinator.showPokemon(spawn.pokemon.id, brightnessFactor, spawn.isShiny, spawn.form)
        displayedSpawn = spawn
    }

    /**
     * Show a digital clock on the Glyph Matrix.
     */
    private fun showDigitalWatch() {
        val charHeight = 5
        val charSpacing = 1

        glyphMatrixManager?.let { gmm ->
            val currentTime = Calendar.getInstance()
            val timeText = if (DateFormat.is24HourFormat(applicationContext)) {
                val hour = currentTime.get(Calendar.HOUR_OF_DAY)
                val minute = currentTime.get(Calendar.MINUTE)
                String.format(Locale.US, "%02d:%02d", hour, minute)
            } else {
                val hour = currentTime.get(Calendar.HOUR)
                val minute = currentTime.get(Calendar.MINUTE)
                val displayHour = if (hour == 0) 12 else hour
                String.format(Locale.US, "%d:%02d", displayHour, minute)
            }

            fun getTextPixelWidth(text: String): Int {
                var width = 0
                for (i in text.indices) {
                    width += when (text[i]) {
                        ':' -> 1
                        '1' -> 3
                        else -> 4
                    }
                    if (i < text.length - 1) {
                        width += charSpacing
                    }
                }
                return width
            }

            val timeX = GLYPH_MATRIX_CENTER - getTextPixelWidth(timeText) / 2
            val timeY = GLYPH_MATRIX_CENTER - charHeight / 2

            val text = GlyphMatrixObject.Builder()
                .setText(timeText)
                .setPosition(timeX, timeY)
                .setBrightness(255)
                .build()

            val frame = GlyphMatrixFrame.Builder()
                .addTop(text)
                .build(applicationContext)

            gmm.setMatrixFrame(frame.render())
        }
    }

    /**
     * Renders a fully black frame.
     */
    private fun clearGlyphMatrix() {
        glyphMatrixManager?.setMatrixFrame(frameFactory.renderBlankFrame())
    }

    /**
     * Called by the system when the user presses the touch button.
     */
    override fun onTouchPointLongPress() {
        Log.d(TAG, "Touch point long press detected")
        coroutineScope?.launch {
            val currentSpawn = synchronized(spawnQueue) {
                spawnQueue.firstOrNull()
            }
            debugCapture.log("input_long_press", currentDebugSnapshot()) {
                buildJsonObject {
                    put("hadSpawn", JsonPrimitive(currentSpawn != null))
                    currentSpawn?.let { spawn ->
                        put("pokemonId", JsonPrimitive(spawn.pokemon.id))
                        put("pokemonName", JsonPrimitive(spawn.pokemon.name))
                        put("pool", JsonPrimitive(spawn.pool.name))
                    }
                }
            }
            if (currentSpawn != null) {
                catchPokemon(currentSpawn)
            }
        }
    }

    /**
     * Catches the currently spawned Pokémon.
     */
    private suspend fun catchPokemon(spawn: SpawnResult, animate: Boolean = true) {
        Log.d(TAG, "Catching ${spawn.pokemon.name}!")

        try {
            val caughtPokemon = CatchRecorder.record(
                db = db,
                preferences = preferencesManager,
                spawn = PersistentSpawn(
                    pokemonId = spawn.pokemon.id,
                    poolName = spawn.pool.name,
                    isSpecial = spawn.pool.isSpecial,
                    isConditional = spawn.pool.isConditional,
                    screenOffDurationMinutes = spawn.screenOffDurationMinutes,
                    spawnedAtMillis = spawn.spawnedAtMillis,
                    isShiny = spawn.isShiny,
                    form = spawn.form
                ),
                canSpawn = { gameplayContext.trainer.canSpawn(it) },
                onAward = { item, reason -> logItemAward(item, reason, spawn.pokemon.id) }
            )
            Log.d(TAG, "Successfully saved ${spawn.pokemon.name} to database")

            val queueSnapshot = synchronized(spawnQueue) {
                spawnQueue.remove(spawn)
                spawnQueue.toList()
            }
            spawnHistory.updateActiveQueue(queueSnapshot)
            saveSpawnQueue()

            val snapshot = currentDebugSnapshot()
            debugCapture.log("catch_success", snapshot) {
                buildJsonObject {
                    put("pokemonId", JsonPrimitive(spawn.pokemon.id))
                    put("pokemonName", JsonPrimitive(spawn.pokemon.name))
                    put("pool", JsonPrimitive(spawn.pool.name))
                    put("caughtAt", JsonPrimitive(caughtPokemon.caughtAt))
                }
            }

            animationCoordinator.cancelActive()
            displayedSpawn = null

            if (animate) showCatchAnimation(faster = queueSnapshot.isNotEmpty())
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Error saving caught Pokémon", e)
            DebugExceptionTracker.log(applicationContext, e, currentDebugSnapshot(), "catch_pokemon")
        }
    }

    private suspend fun logItemAward(item: Item, reason: String, speciesId: Int) {
        val snapshot = currentDebugSnapshot()
        debugCapture.log("item_award", snapshot) {
            buildJsonObject {
                put("item", JsonPrimitive(item.name.lowercase(Locale.US)))
                put("reason", JsonPrimitive(reason))
                put("speciesId", JsonPrimitive(speciesId))
            }
        }
    }

    /**
     * Plays a brief animation on the Glyph Matrix to confirm a successful catch.
     */
    private suspend fun showCatchAnimation(faster: Boolean) {
        animationCoordinator.playCatch(faster)
        updateGlyphMatrix()
    }

    /**
     * Acquires a wake lock to prevent animation freezes.
     */
    private fun acquireAnimationWakeLock() {
        val wakeLock = animationWakeLock ?: return
        synchronized(animationWakeLockMutex) {
            if (activeAnimationWakeLockHolders == 0) {
                val acquired = runCatching { wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS) }
                    .onFailure { error -> Log.w(TAG, "Unable to acquire animation wake lock", error) }
                    .isSuccess
                if (!acquired) {
                    return
                }
            }
            activeAnimationWakeLockHolders++
        }
    }

    /**
     * Releases the wake lock to preserve battery.
     */
    private fun releaseAnimationWakeLock() {
        val wakeLock = animationWakeLock ?: return
        synchronized(animationWakeLockMutex) {
            if (activeAnimationWakeLockHolders == 0) {
                return
            }
            activeAnimationWakeLockHolders--
            if (activeAnimationWakeLockHolders == 0 && wakeLock.isHeld) {
                runCatching { wakeLock.release() }
                    .onFailure { error -> Log.w(TAG, "Unable to release animation wake lock", error) }
            }
        }
    }

    /**
     * Saves the current spawn queue to SharedPreferences.
     */
    private fun saveSpawnQueue() {
        synchronized(spawnQueue) {
            val persistentSpawns = spawnQueue.map { spawn ->
                PersistentSpawn(
                    pokemonId = spawn.pokemon.id,
                    poolName = spawn.pool.name,
                    isSpecial = spawn.pool.isSpecial,
                    isConditional = spawn.pool.isConditional,
                    screenOffDurationMinutes = spawn.screenOffDurationMinutes,
                    spawnedAtMillis = spawn.spawnedAtMillis,
                    isShiny = spawn.isShiny,
                    form = spawn.form
                )
            }

            val json = Json.encodeToString(persistentSpawns)
            applicationContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit {
                    putString(PREFS_KEY_SPAWN_QUEUE, json)
                }
            Log.d(TAG, "Saved ${persistentSpawns.size} spawns to storage")
        }
        WidgetUpdater.refresh(applicationContext)
    }

    /**
     * Catches every waiting Pokémon at once, from the queue widget.
     */
    private suspend fun catchFirst() {
        val first = synchronized(spawnQueue) { spawnQueue.firstOrNull() } ?: return
        catchPokemon(first)
    }

    private suspend fun catchAll() {
        val waiting = synchronized(spawnQueue) { spawnQueue.toList() }
        if (waiting.isEmpty()) return
        // One after another, then a single catch animation for the lot
        waiting.forEach { catchPokemon(it, animate = false) }
        withContext(Dispatchers.Main) { showCatchAnimation(faster = false) }
    }

    /**
     * Restores the spawn queue from SharedPreferences.
     */
    private fun restoreSpawnQueue() {
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val json = prefs.getString(PREFS_KEY_SPAWN_QUEUE, null) ?: run {
            spawnHistory.updateActiveQueue(emptyList())
            return
        }

        var snapshot: List<SpawnResult>? = null
        synchronized(spawnQueue) {
            spawnQueue.clear()
            displayedSpawn = null

            try {
                val persistentSpawns: List<PersistentSpawn> = Json.decodeFromString(json)
                persistentSpawns.forEach { persistent ->
                    val pokemon = Pokemon.all[persistent.pokemonId]
                    if (pokemon != null) {
                        val pool = spawnEngine.rules.pools.find { it.name == persistent.poolName }!!
                        val spawn = SpawnResult(
                            pokemon = pokemon,
                            pool = pool,
                            screenOffDurationMinutes = persistent.screenOffDurationMinutes,
                            spawnedAtMillis = persistent.spawnedAtMillis,
                            isShiny = persistent.isShiny,
                            form = persistent.form
                        )
                        spawnQueue.add(spawn)
                    }
                }
                Log.d(TAG, "Restored ${spawnQueue.size} spawns from storage")
                sortQueueByRarity()
                snapshot = spawnQueue.toList()
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring spawn queue", e)
                prefs.edit { remove(PREFS_KEY_SPAWN_QUEUE) }
                DebugExceptionTracker.log(applicationContext, e, currentDebugSnapshot(), "restore_spawn_queue")
            }
        }

        snapshot?.let { spawnHistory.updateActiveQueue(it) }
            ?: spawnHistory.updateActiveQueue(emptyList())
    }

    /**
     * Captures a game state snapshot for debug logging.
     */
    private fun currentDebugSnapshot(): DebugSnapshot = DebugSnapshot(
        phoneBattery = gameplayContext.phone.battery,
        phoneIsInteractive = gameplayContext.phone.isInteractive,
        phoneMinutesOff = gameplayContext.phone.minutesOff,
        phoneMinutesOffOutsideBedtime = gameplayContext.phone.minutesOffOutsideBedtime,
        queueSize = synchronized(spawnQueue) { spawnQueue.size },
        hasSleepBonus = gameplayContext.sleep.hasSleepBonus,
        isBedtime = gameplayContext.sleep.isBedtime
    )

    /**
     * Invoked when an unhandled exception occurs in a coroutine.
     */
    private fun handleCoroutineException(throwable: Throwable) {
        Log.e(TAG, "Unhandled coroutine exception", throwable)
        val snapshot = runCatching {
            if (::gameplayContext.isInitialized) {
                currentDebugSnapshot()
            } else {
                DebugSnapshot.EMPTY
            }
        }.getOrElse { DebugSnapshot.EMPTY }

        DebugExceptionTracker.log(
            context = applicationContext,
            throwable = throwable,
            snapshot = snapshot,
            source = "coroutine"
        )
    }

    companion object {
        private const val TAG = "PokemonGlyphToy"

        private const val MAX_QUEUE_SIZE = 9
        private const val HIDDEN_MAX_QUEUE_SIZE = 15

        /** The hidden "play while in use" setting brings more Pokémon, so more can wait. */
        fun maxQueueSize(preferences: PreferencesManager): Int =
            if (preferences.playWhileInUse) HIDDEN_MAX_QUEUE_SIZE else MAX_QUEUE_SIZE
        private const val PREFS_NAME = "pokemon_glyph_toy_prefs"
        private const val PREFS_KEY_SPAWN_QUEUE = "spawn_queue"

        @Volatile
        private var instance: PokemonGlyphToyService? = null

        /** Whether the Glyph Toy is switched on right now. */
        val isRunning: Boolean get() = instance != null

        fun writeSavedQueue(context: Context, queue: List<PersistentSpawn>) {
            context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit {
                putString(PREFS_KEY_SPAWN_QUEUE, widgetJson.encodeToString(queue))
            }
        }

        /** The Pokémon waiting on the Glyph Matrix, as last saved. */
        fun readSavedQueue(context: Context): List<PersistentSpawn> =
            decodeQueue(context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(PREFS_KEY_SPAWN_QUEUE, null))

        /**
         * Only works while the Glyph Toy is running, which is when Pokémon are waiting. Returns
         * right away: a widget button that takes too long makes Android report the app as frozen.
         */
        fun catchAllFromWidget(context: Context) {
            val service = instance
            if (service != null) {
                service.coroutineScope?.launch { service.catchAll() }
                return
            }
            // The toy is off (the phone is in use): catch straight from the saved queue
            CatchRecorder.scope.launch {
                while (CatchRecorder.catchFromSavedQueue(context) != null) Unit
                WidgetUpdater.refresh(context)
            }
        }

        /** True while a Poké Ball thrown from the app or a widget is still in the air. */
        val throwing = MutableStateFlow(false)

        /** The last Pokémon caught with a Poké Ball from the app or a widget. */
        val lastThrowCatch = MutableStateFlow<CaughtPokemon?>(null)

        /**
         * Throws a Poké Ball at the first Pokémon waiting, from the app or a widget. With the phone
         * in use the Glyph Toy is off, so the catch plays on the Glyph Matrix from here. Throws made
         * while one is still in the air are ignored.
         */
        fun catchOne(context: Context) {
            val service = instance
            if (service != null) {
                service.coroutineScope?.launch { service.catchFirst() }
                return
            }
            if (!throwing.compareAndSet(expect = false, update = true)) return
            CatchRecorder.scope.launch {
                try {
                    val caught = CatchRecorder.catchFromSavedQueue(context) ?: return@launch
                    lastThrowCatch.value = caught
                    WidgetUpdater.refresh(context)
                    AppMatrix.playCatch(context, caught)
                } finally {
                    throwing.value = false
                }
            }
        }

        /**
         * Redraws the Glyph Matrix after the companion or what to show without Pokémon changes,
         * instead of waiting for the next minute.
         */
        fun refreshIdleDisplay() {
            val service = instance ?: return
            service.coroutineScope?.launch {
                service.gameplayContext.trainer.refresh()
                withContext(Dispatchers.Main) { service.updateGlyphMatrix() }
            }
        }
        private const val SLEEP_BONUS_DURATION_MILLIS = 24L * 60 * 60 * 1000

        // 25×25 on the Phone (3), 13×13 on the Phone (4a) Pro
        private val GLYPH_MATRIX_SIZE = GlyphDevice.matrixSize
        private val GLYPH_MATRIX_CENTER = GLYPH_MATRIX_SIZE / 2
        private const val LOWER_BRIGHTNESS_FACTOR = 0.6f
        private const val COMPANION_BRIGHTNESS = 0.45f
        private const val DOT_BLINK_MS = 600L
        private const val WAKE_LOCK_TIMEOUT_MS = 6000L
        private const val WAKE_LOCK_TAG = "GlyphCatch:Animation"

        private const val TRAINING_EXP_PER_MINUTE = 3
        private const val TRAINING_EXP_BONUS_INTERVAL_MINUTES = 20
        private const val TRAINING_EXP_BONUS_AMOUNT = 90
        private const val EXP_SHARE_PERCENT = 67
    }
}
