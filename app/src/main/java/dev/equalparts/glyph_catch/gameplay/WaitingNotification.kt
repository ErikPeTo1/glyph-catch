package dev.equalparts.glyph_catch.gameplay

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.scale
import dev.equalparts.glyph_catch.MainActivity
import dev.equalparts.glyph_catch.R
import dev.equalparts.glyph_catch.data.Pokemon
import dev.equalparts.glyph_catch.data.PreferencesManager
import dev.equalparts.glyph_catch.data.displayName
import dev.equalparts.glyph_catch.util.PokemonSpriteUtils

/**
 * A quiet notification while Pokémon are waiting, with a button to throw a Poké Ball right from
 * the lock screen. Unlocking resets the time with the screen off, so catching without unlocking
 * keeps it going. On by default on the Phone (4a) Pro, whose Glyph button can't catch.
 */
object WaitingNotification {
    private const val CHANNEL_ID = "waiting_pokemon"
    private const val NOTIFICATION_ID = 1001
    private const val ACTION_THROW = "dev.equalparts.glyph_catch.THROW_POKE_BALL"

    /** What was last shown, so the same queue isn't posted again (and dismissing it sticks). */
    @Volatile
    private var shown: List<String> = emptyList()

    fun canPost(context: Context): Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED

    /** Shows, updates or removes the notification to match the Pokémon waiting. */
    fun update(context: Context) {
        val app = context.applicationContext
        val queue = PokemonGlyphToyService.readSavedQueue(app)
        val manager = NotificationManagerCompat.from(app)
        if (queue.isEmpty() || !PreferencesManager(app).waitingNotification || !canPost(app)) {
            manager.cancel(NOTIFICATION_ID)
            shown = emptyList()
            return
        }
        val signature = queue.map { "${it.pokemonId}:${it.spawnedAtMillis}" }
        if (signature == shown) return
        shown = signature

        createChannel(app)
        val first = queue.first()
        val name = Pokemon[first.pokemonId]?.displayName.orEmpty()
        val sprite = BitmapFactory.decodeResource(
            app.resources,
            PokemonSpriteUtils.getColorResourceId(app, first.pokemonId, first.isShiny, first.form),
            BitmapFactory.Options().apply { inScaled = false }
        )?.let { it.scale(it.width * 2, it.height * 2, false) }
        val more = queue.size - 1
        val moreText = if (more >
            0
        ) {
            app.resources.getQuantityString(R.plurals.home_encounter_more, more, more)
        } else {
            null
        }

        val open = PendingIntent.getActivity(
            app,
            0,
            Intent(app, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val throwBall = PendingIntent.getBroadcast(
            app,
            0,
            Intent(app, ThrowPokeBallReceiver::class.java).setAction(ACTION_THROW),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(app, CHANNEL_ID)
            .setSmallIcon(R.drawable.icon_pokeball)
            .setLargeIcon(sprite)
            .setContentTitle(app.getString(R.string.home_encounter_wild, name) + if (first.isShiny) " ✨" else "")
            .setContentText(moreText)
            .setContentIntent(open)
            .addAction(0, app.getString(R.string.home_throw_ball), throwBall)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setAutoCancel(false)
            .build()
        runCatching { manager.notify(NOTIFICATION_ID, notification) }
    }

    private fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_waiting),
            NotificationManager.IMPORTANCE_LOW
        ).apply { lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}

/** The notification's "Throw a Poké Ball": catches the first one, no unlocking needed. */
class ThrowPokeBallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        PokemonGlyphToyService.catchOne(context)
    }
}
