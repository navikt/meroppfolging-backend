package no.nav.syfo.utils

import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

private const val BREV_DATE_FORMAT_PATTERN = "dd. MMMM yyyy"
fun parseDate(dateString: String): LocalDate {
    val formatter = DateTimeFormatter.ofPattern(BREV_DATE_FORMAT_PATTERN)
    return LocalDate.parse(dateString, formatter)
}

fun formatDateForLetter(date: LocalDate): String {
    return date.format(DateTimeFormatter.ofPattern(BREV_DATE_FORMAT_PATTERN))
}

fun parsePDLDate(date: String): LocalDate {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    return LocalDate.parse(date, formatter)
}

fun isAlderMindreEnnGittAr(fodselsdato: String, maxAlder: Int): Boolean {
    val parsedFodselsdato = fodselsdato.let { parsePDLDate(it) }

    return Period.between(parsedFodselsdato, LocalDate.now()).years < maxAlder
}
