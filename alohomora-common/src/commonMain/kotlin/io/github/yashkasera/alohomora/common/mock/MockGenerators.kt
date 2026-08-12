package io.github.yashkasera.alohomora.common.mock

import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.time.Clock
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

    fun int(min: Int = 0, max: Int = 100): String =
        Random.nextInt(min, max + 1).toString()

    fun float(min: Double = 0.0, max: Double = 1.0): String {
        val value = Random.nextDouble(min, max)
        val scaled = (value * 10_000).roundToInt()
        return "${scaled / 10_000}.${(scaled % 10_000).toString().padStart(4, '0')}"
    }

    fun amount(min: Double = 0.0, max: Double = 1000.0): String {
        val value = Random.nextDouble(min, max)
        val cents = (value * 100).roundToInt()
        return "${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"
    }

    fun bool(): String = Random.nextBoolean().toString()

    fun timestamp(): String = Clock.System.now().toEpochMilliseconds().toString()

    fun date(direction: String = "past", offsetDays: Int = 30): String {
        val now = Clock.System.now()
        val days = Random.nextInt(1, offsetDays + 1)
        val instant = when (direction.lowercase()) {
            "future" -> now.plus(days, DateTimeUnit.DAY, TimeZone.UTC)
            else -> now.minus(days, DateTimeUnit.DAY, TimeZone.UTC)
        }
        val local = instant.toLocalDateTime(TimeZone.UTC)
        @Suppress("DEPRECATION")
        return "${local.year}-${local.monthNumber.toString().padStart(2, '0')}-${local.dayOfMonth.toString().padStart(2, '0')}"
    }

    fun oneOf(options: List<String>): String {
        if (options.isEmpty()) return ""
        return options[Random.nextInt(options.size)].trim()
    }
}
