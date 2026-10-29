package dev.equalparts.glyph_catch.util

import java.time.Duration
import java.time.LocalDate
import java.time.Month
import java.time.MonthDay
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Calendar
import kotlin.math.abs

object EventHelpers {
    const val BASE_SHINY_ODDS = 512

    /**
     * One in this many wild Pokémon is shiny: 1/512 normally, doubled during the Halloween
     * and Christmas weeks and quadrupled on the big days.
     */
    fun shinyOdds(date: LocalDate = LocalDate.now()): Int {
        val day = MonthDay.from(date)
        val bigDays = setOf(
            MonthDay.of(Month.OCTOBER, 31), // Halloween
            MonthDay.of(Month.DECEMBER, 24),
            MonthDay.of(Month.DECEMBER, 25), // Christmas
            MonthDay.of(Month.JANUARY, 1), // New Year
            MonthDay.of(Month.FEBRUARY, 27) // Pokémon Day
        )
        val isEventWeek = (date.month == Month.OCTOBER && date.dayOfMonth in 25..31) ||
            (date.month == Month.DECEMBER && date.dayOfMonth in 19..26)
        return when {
            day in bigDays -> BASE_SHINY_ODDS / 4
            isEventWeek -> BASE_SHINY_ODDS / 2
            else -> BASE_SHINY_ODDS
        }
    }

    /**
     * How many times more likely shinies are today, or 1 when there's no boost.
     */
    fun shinyBoost(): Int = BASE_SHINY_ODDS / shinyOdds()

    fun isHalloween(): Boolean {
        val cal = Calendar.getInstance()
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        return month == Calendar.OCTOBER && day in 25..31
    }

    fun isChristmas(): Boolean {
        val cal = Calendar.getInstance()
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        return month == Calendar.DECEMBER && day in 19..26
    }

    fun isValentines(date: LocalDate = LocalDate.now()): Boolean = date.month == Month.FEBRUARY && date.dayOfMonth == 14

    fun isPokemonDay(date: LocalDate = LocalDate.now()): Boolean = date.month == Month.FEBRUARY && date.dayOfMonth == 27

    fun isNewYear(date: LocalDate = LocalDate.now()): Boolean =
        (date.month == Month.DECEMBER && date.dayOfMonth == 31) || (date.month == Month.JANUARY && date.dayOfMonth == 1)

    fun isSanJuan(date: LocalDate = LocalDate.now()): Boolean = date.month == Month.JUNE && date.dayOfMonth in 23..24

    fun isFullMoon(): Boolean {
        val synodicMonthDays = 29.530588853
        val millisPerDay = 86_400_000.0
        val fullMoonThresholdDays = 0.9
        val referenceNewMoon = ZonedDateTime.of(2025, 9, 21, 19, 54, 0, 0, ZoneOffset.UTC)

        val nowUtc = ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC)
        val daysSinceReference = Duration.between(referenceNewMoon, nowUtc).toMillis() / millisPerDay
        val moonAge = ((daysSinceReference % synodicMonthDays) + synodicMonthDays) % synodicMonthDays
        val delta = abs(moonAge - synodicMonthDays / 2.0)
        return delta <= fullMoonThresholdDays
    }
}
