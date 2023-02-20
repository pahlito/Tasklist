import kotlinx.datetime.*

fun nextMonth(date: String): String = date.toInstant().plus(1, DateTimeUnit.MONTH, TimeZone.UTC).toString()


fun main() {
    val date = readln()
    println(nextMonth(date))
}