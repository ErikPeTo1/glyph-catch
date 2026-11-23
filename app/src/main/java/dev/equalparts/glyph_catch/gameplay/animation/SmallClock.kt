package dev.equalparts.glyph_catch.gameplay.animation

/** 3×5 digits, one row per string, `#` lit. */
private val DIGITS_3X5 = listOf(
    listOf("###", "#.#", "#.#", "#.#", "###"),
    listOf(".#.", "##.", ".#.", ".#.", "###"),
    listOf("###", "..#", "###", "#..", "###"),
    listOf("###", "..#", "###", "..#", "###"),
    listOf("#.#", "#.#", "###", "..#", "..#"),
    listOf("###", "#..", "###", "..#", "###"),
    listOf("###", "#..", "###", "#.#", "###"),
    listOf("###", "..#", "..#", "..#", "..#"),
    listOf("###", "#.#", "###", "#.#", "###"),
    listOf("###", "#.#", "###", "..#", "###")
)

private const val DIGIT_WIDTH = 3
private const val DIGIT_HEIGHT = 5
private const val DIGIT_GAP = 1
private const val LINE_GAP = 1

/**
 * The clock for the 13×13 Glyph Matrix of the Phone (4a) Pro, where "12:34" doesn't fit on one
 * line: hours on top, minutes below, each line centered.
 */
internal fun renderSmallClockFrame(hours: String, minutes: String, matrixSize: Int): IntArray {
    val frame = IntArray(matrixSize * matrixSize)
    val top = (matrixSize - (DIGIT_HEIGHT * 2 + LINE_GAP)) / 2
    drawDigits(frame, matrixSize, hours, top)
    drawDigits(frame, matrixSize, minutes, top + DIGIT_HEIGHT + LINE_GAP)
    return frame
}

private fun drawDigits(frame: IntArray, matrixSize: Int, text: String, top: Int) {
    val width = text.length * DIGIT_WIDTH + (text.length - 1).coerceAtLeast(0) * DIGIT_GAP
    var left = (matrixSize - width) / 2
    for (char in text) {
        val glyph = DIGITS_3X5.getOrNull(char - '0') ?: continue
        glyph.forEachIndexed { row, line ->
            line.forEachIndexed { col, cell ->
                val x = left + col
                val y = top + row
                if (cell == '#' && x in 0 until matrixSize && y in 0 until matrixSize) {
                    frame[y * matrixSize + x] = MAX_LED_BRIGHTNESS
                }
            }
        }
        left += DIGIT_WIDTH + DIGIT_GAP
    }
}
