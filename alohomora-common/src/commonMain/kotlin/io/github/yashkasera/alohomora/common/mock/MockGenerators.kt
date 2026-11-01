package io.github.yashkasera.alohomora.common.mock

import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

internal object MockGenerators {

    private val firstNames = listOf(
        "Aarav", "Aditi", "Aditya", "Ananya", "Arjun", "Avni", "Deepa", "Dev",
        "Diya", "Gaurav", "Isha", "Kabir", "Kavya", "Kiran", "Krishna", "Lakshmi",
        "Manish", "Meera", "Neha", "Nikhil", "Pooja", "Priya", "Rahul", "Raj",
        "Riya", "Rohit", "Sakshi", "Sanjay", "Shreya", "Simran", "Sneha", "Tanvi",
        "Varun", "Vidya", "Vikram", "Yash",
    )

    private val lastNames = listOf(
        "Agarwal", "Bhat", "Choudhury", "Desai", "Ghosh", "Gupta", "Iyer", "Jain",
        "Joshi", "Kapoor", "Kasera", "Khan", "Kumar", "Malhotra", "Mehta", "Menon",
        "Mishra", "Nair", "Pandey", "Patel", "Rao", "Reddy", "Shah", "Sharma",
        "Shetty", "Singh", "Sinha", "Tiwari", "Varma", "Verma",
    )

    @OptIn(ExperimentalUuidApi::class)
    fun uuid(): String = Uuid.random().toString()

    fun firstName(): String = firstNames.random()

    fun lastName(): String = lastNames.random()

    fun name(): String = "${firstName()} ${lastName()}"

    fun email(): String {
        val first = firstName().lowercase()
        val last = lastName().lowercase()
        return "$first.$last@example.com"
    }

    // Bounds come from a hand-written template, so they are normalised rather than trusted:
    // reversed bounds are swapped and equal bounds yield that value. Random throws on all of
    // these, and the throw happened inside the host app's request (see TemplateEngine.resolve).
    // Long arithmetic, because `max + 1` overflows at Int.MAX_VALUE.
    fun int(min: Int = 0, max: Int = 100): String {
        val lo = minOf(min, max).toLong()
        val hi = maxOf(min, max).toLong()
        return Random.nextLong(lo, hi + 1).toString()
    }

    fun float(min: Double = 0.0, max: Double = 1.0): String =
        fixed(between(min, max), decimals = 4)

    fun amount(min: Double = 0.0, max: Double = 1000.0): String =
        fixed(between(min, max), decimals = 2)

    fun bool(): String = Random.nextBoolean().toString()

    fun timestamp(): String = Clock.System.now().toEpochMilliseconds().toString()

    fun date(direction: String = "past", offsetDays: Int = 30): String {
        val now = Clock.System.now()
        // 1..offsetDays as before, but an offset of zero or less means today. `nextInt(n) + 1`
        // rather than `nextInt(1, n + 1)`, which threw for 0 and overflowed at Int.MAX_VALUE.
        val days = if (offsetDays <= 0) 0 else Random.nextInt(offsetDays) + 1
        val instant = when (direction.lowercase()) {
            "future" -> now.plus(days, DateTimeUnit.DAY, TimeZone.UTC)
            else -> now.minus(days, DateTimeUnit.DAY, TimeZone.UTC)
        }
        val local = instant.toLocalDateTime(TimeZone.UTC)
        @Suppress("DEPRECATION")
        return "${local.year}-${
            local.monthNumber.toString().padStart(2, '0')
        }-${local.dayOfMonth.toString().padStart(2, '0')}"
    }

    fun oneOf(options: List<String>): String {
        if (options.isEmpty()) return ""
        return options[Random.nextInt(options.size)].trim()
    }

    private fun between(min: Double, max: Double): Double {
        val lo = minOf(min, max)
        val hi = maxOf(min, max)
        return if (lo == hi) lo else Random.nextDouble(lo, hi)
    }

    /**
     * Formats [value] with exactly [decimals] places and no exponent, so it drops into a JSON
     * body as a plain number.
     *
     * The sign is handled apart from the digits. Splitting a negative scaled value with `/` and
     * `%` gave `0.-5000` for -0.5. Magnitudes past what a Long can hold after scaling fall back
     * to `toString()`, which is still a valid JSON number, rather than clamping to a wrong one.
     */
    private fun fixed(value: Double, decimals: Int): String {
        var factor = 1L
        repeat(decimals) { factor *= 10 }
        val magnitude = abs(value) * factor
        if (magnitude >= Long.MAX_VALUE.toDouble()) return value.toString()
        val scaled = magnitude.roundToLong()
        val sign = if (value < 0 && scaled != 0L) "-" else ""
        val fraction = (scaled % factor).toString().padStart(decimals, '0')
        return "$sign${scaled / factor}.$fraction"
    }
}
