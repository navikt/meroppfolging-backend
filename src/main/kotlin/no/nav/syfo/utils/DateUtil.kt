package no.nav.syfo.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val DISPLAY_DATE_FORMAT_PATTERN = "dd. MMMM yyyy"
fun parseDate(dateString: String): LocalDate {
    val formatter = DateTimeFormatter.ofPattern(DISPLAY_DATE_FORMAT_PATTERN)
    return LocalDate.parse(dateString, formatter)
}

fun formatDateForDisplayAndPdf(date: LocalDate): String {

    return date.format(
        DateTimeFormatter.ofPattern(DISPLAY_DATE_FORMAT_PATTERN).withLocale(
            Locale.forLanguageTag("nb-NO"),
        ),
    )
}
