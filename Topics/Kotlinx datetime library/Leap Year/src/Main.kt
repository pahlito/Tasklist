import kotlinx.datetime.*
import java.time.DateTimeException
import java.time.format.DateTimeParseException

fun isLeapYear(year: String): Boolean =
    try {
        "$year-02-29T00:00:00Z".toInstant()
        true
    } catch(e: RuntimeException ) {
        false
    }

fun main() {
    val year = readln()
    println(isLeapYear(year))
}
