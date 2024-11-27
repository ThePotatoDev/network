package gg.tater.shared

import net.kyori.adventure.text.minimessage.MiniMessage
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

const val ARROW_TEXT = "&7&l\uD83E\uDC7A"

val UUID_REGEX =
    Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\$").toRegex()

val DECIMAL_FORMAT = DecimalFormat("#,###.##")

val MINI_MESSAGE = MiniMessage.miniMessage()

private val DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yy")
fun getFormattedDate(): String {
    return DATE_FORMAT.format(LocalDate.now())
}
