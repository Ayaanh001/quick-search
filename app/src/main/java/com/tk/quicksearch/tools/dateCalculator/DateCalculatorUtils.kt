package com.tk.quicksearch.tools.dateCalculator

import java.time.LocalDate
import java.time.Period
import java.util.Locale

object DateCalculatorUtils {
    private val monthNames: Map<String, Int> = buildMap {
        put("january", 1); put("jan", 1)
        put("february", 2); put("feb", 2)
        put("march", 3); put("mar", 3)
        put("april", 4); put("apr", 4)
        put("may", 5)
        put("june", 6); put("jun", 6)
        put("july", 7); put("jul", 7)
        put("august", 8); put("aug", 8)
        put("september", 9); put("sep", 9)
        put("october", 10); put("oct", 10)
        put("november", 11); put("nov", 11)
        put("december", 12); put("dec", 12)
    }

    // Matches month names (full or abbreviated, case-insensitive)
    private val monthPattern = Regex(
        "\\b(january|jan|february|feb|march|mar|april|apr|may|june|jun|july|jul|august|aug|september|sep|october|oct|november|nov|december|dec)\\b",
        RegexOption.IGNORE_CASE,
    )
    private val yearPattern = Regex("\\b(\\d{4})\\b")
    private val dayPattern = Regex("\\b(\\d{1,2})\\b")

    /**
     * Parses a relative date expression and returns the resulting [LocalDate] relative to today.
     *
     * Supported formats (case-insensitive):
     *   2 years ago | 1 year 3 months ago | 6 months ago | 10 days ago | 2 weeks ago
     *   in 2 years | in 1 year 6 months | in 3 months | in 10 days | in 2 weeks
     */
    fun parseRelativeDateQuery(query: String): LocalDate? {
        val lower = query.trim().lowercase(Locale.US)

        val isFuture = lower.startsWith("in ") ||
            lower.endsWith(" from now") ||
            lower.endsWith(" from today")
        val isPast = lower.endsWith(" ago")
        if (!isFuture && !isPast) return null

        val core = when {
            lower.startsWith("in ") -> lower.removePrefix("in ").trim()
            lower.endsWith(" from now") -> lower.removeSuffix(" from now").trim()
            lower.endsWith(" from today") -> lower.removeSuffix(" from today").trim()
            else -> lower.removeSuffix(" ago").trim()
        }

        val unitPattern = Regex("""(\d+)\s*(years?|months?|weeks?|days?)""")
        var years = 0
        var months = 0
        var weeks = 0
        var days = 0

        var remaining = core
        for (match in unitPattern.findAll(core)) {
            val value = match.groupValues[1].toIntOrNull() ?: continue
            when {
                match.groupValues[2].startsWith("year") -> years = value
                match.groupValues[2].startsWith("month") -> months = value
                match.groupValues[2].startsWith("week") -> weeks = value
                match.groupValues[2].startsWith("day") -> days = value
            }
            remaining = remaining.replace(match.value, " ")
        }

        if (remaining.trim().any { it.isLetterOrDigit() }) return null
        if (years == 0 && months == 0 && weeks == 0 && days == 0) return null

        val today = LocalDate.now()
        return if (isFuture) {
            today.plusYears(years.toLong())
                .plusMonths(months.toLong())
                .plusWeeks(weeks.toLong())
                .plusDays(days.toLong())
        } else {
            today.minusYears(years.toLong())
                .minusMonths(months.toLong())
                .minusWeeks(weeks.toLong())
                .minusDays(days.toLong())
        }
    }

    /**
     * Parses an offset-from-date query such as "5 days from march 2" or "3 months before march 30"
     * and returns the resulting [LocalDate].
     *
     * Supported separators:
     *   <units> from <date>   → adds units to the base date
     *   <units> after <date>  → adds units to the base date
     *   <units> before <date> → subtracts units from the base date
     */
    fun parseOffsetFromDateQuery(query: String): LocalDate? {
        val lower = query.trim().lowercase(Locale.US)

        val separators = listOf(" from " to true, " after " to true, " before " to false)

        for ((sep, isFuture) in separators) {
            val sepIndex = lower.indexOf(sep)
            if (sepIndex < 0) continue

            val unitsPart = lower.substring(0, sepIndex).trim()
            val datePart = query.substring(sepIndex + sep.length).trim()

            val baseDate = parseDateQuery(datePart) ?: continue

            val unitPattern = Regex("""(\d+)\s*(years?|months?|weeks?|days?)""")
            var years = 0; var months = 0; var weeks = 0; var days = 0
            var remaining = unitsPart
            for (match in unitPattern.findAll(unitsPart)) {
                val value = match.groupValues[1].toIntOrNull() ?: continue
                when {
                    match.groupValues[2].startsWith("year") -> years = value
                    match.groupValues[2].startsWith("month") -> months = value
                    match.groupValues[2].startsWith("week") -> weeks = value
                    match.groupValues[2].startsWith("day") -> days = value
                }
                remaining = remaining.replace(match.value, " ")
            }

            if (remaining.trim().any { it.isLetterOrDigit() }) continue
            if (years == 0 && months == 0 && weeks == 0 && days == 0) continue

            return if (isFuture) {
                baseDate.plusYears(years.toLong()).plusMonths(months.toLong())
                    .plusWeeks(weeks.toLong()).plusDays(days.toLong())
            } else {
                baseDate.minusYears(years.toLong()).minusMonths(months.toLong())
                    .minusWeeks(weeks.toLong()).minusDays(days.toLong())
            }
        }
        return null
    }

    /**
     * Parses a date-difference query of the form "<date1> to <date2>" and returns the two dates.
     * Each part is parsed with [parseDateQuery].
     */
    fun parseDateDiffQuery(query: String): Pair<LocalDate, LocalDate>? {
        val lower = query.trim().lowercase(Locale.US)
        val toIndex = lower.indexOf(" to ")
        if (toIndex < 0) return null
        val part1 = query.substring(0, toIndex).trim()
        val part2 = query.substring(toIndex + 4).trim()
        val date1 = parseDateQuery(part1) ?: return null
        val date2 = parseDateQuery(part2) ?: return null
        return date1 to date2
    }

    /** Computes a human-readable difference label between two dates, e.g. "1 year 2 months 3 days". */
    fun diffLabel(date1: LocalDate, date2: LocalDate): String {
        val (start, end) = if (!date1.isAfter(date2)) date1 to date2 else date2 to date1
        val period = Period.between(start, end)
        val parts = buildList {
            if (period.years > 0) add("${period.years} ${if (period.years == 1) "year" else "years"}")
            if (period.months > 0) add("${period.months} ${if (period.months == 1) "month" else "months"}")
            if (period.days > 0) add("${period.days} ${if (period.days == 1) "day" else "days"}")
        }
        return if (parts.isEmpty()) "0 days" else parts.joinToString(" ")
    }

    /**
     * Parses a date query containing a month name, a 4-digit year, and a 1-2 digit day in any order.
     *
     * Supported formats:
     *   March 12 2025 | 12 March 2025 | 2025 March 12 | 2025 12 March | Mar 12 2025 | etc.
     */
    fun parseDateQuery(query: String): LocalDate? {
        val lower = query.trim().lowercase(Locale.US)

        // Must contain a recognizable month name
        val monthMatch = monthPattern.find(lower) ?: return null
        val monthNum = monthNames[monthMatch.value.lowercase(Locale.US)] ?: return null

        // Remove the month name to isolate numbers
        val withoutMonth = lower.replace(monthMatch.value, " ")

        // Find 4-digit year, defaulting to current year if not provided
        val yearMatch = yearPattern.find(withoutMonth)
        val year = yearMatch?.groupValues?.get(1)?.toIntOrNull() ?: LocalDate.now().year

        // Remove year to isolate the day
        val withoutYear = if (yearMatch != null) withoutMonth.replace(yearMatch.value, " ") else withoutMonth

        // Remaining numeric token is the day
        val dayMatch = dayPattern.find(withoutYear.trim()) ?: return null
        val day = dayMatch.groupValues[1].toIntOrNull() ?: return null

        // Ensure no unexpected extra tokens remain
        val remaining = withoutYear.replace(dayMatch.value, " ").trim()
        if (remaining.any { it.isLetterOrDigit() }) return null

        return try {
            LocalDate.of(year, monthNum, day)
        } catch (_: Exception) {
            null
        }
    }
}
