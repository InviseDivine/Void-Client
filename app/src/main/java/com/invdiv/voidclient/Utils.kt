package com.invdiv.voidclient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaDayOfWeek
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.io.File
import java.time.Duration
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale.getDefault
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlin.time.ExperimentalTime
import kotlin.time.Instant.Companion.fromEpochMilliseconds

data class CountryPhoneInfo(
    val name: String,
    val flag: String,
    val phoneCode: String,
    val minDigits: Int,
    val maxDigits: Int,
    val example: String
)

object Utils {
    val maxCodeBase = mutableListOf(
        "RU",
        "BY"
    )

    val countries: Map<String, CountryPhoneInfo> = mapOf(
        "AZ" to CountryPhoneInfo("Азербайджан", "🇦🇿", "994", 9, 9, "501234567"),
        "AM" to CountryPhoneInfo("Армения", "🇦🇲", "374", 8, 8, "91123456"),
        "KZ" to CountryPhoneInfo("Казахстан", "🇰🇿", "7", 10, 10, "7001234567"),
        "KG" to CountryPhoneInfo("Кыргызстан", "🇰🇬", "996", 9, 9, "501234567"),
        "MD" to CountryPhoneInfo("Молдова", "🇲🇩", "373", 8, 8, "69123456"),
        "TJ" to CountryPhoneInfo("Таджикистан", "🇹🇯", "992", 9, 9, "901234567"),
        "UZ" to CountryPhoneInfo("Узбекистан", "🇺🇿", "998", 9, 9, "901234567"),
        "GE" to CountryPhoneInfo("Грузия", "🇬🇪", "995", 9, 9, "599123456"),
        "TH" to CountryPhoneInfo("Таиланд", "🇹🇭", "66", 9, 9, "812345678"),
        "TR" to CountryPhoneInfo("Турция", "🇹🇷", "90", 10, 10, "5321234567"),
        "TM" to CountryPhoneInfo("Туркменистан", "🇹🇲", "993", 8, 8, "65123456"),
        "AE" to CountryPhoneInfo("ОАЭ", "🇦🇪", "971", 9, 9, "501234567"),
        "LA" to CountryPhoneInfo("Лаос", "🇱🇦", "856", 8, 9, "201234567"),
        "MY" to CountryPhoneInfo("Малайзия", "🇲🇾", "60", 9, 10, "123456789"),
        "ID" to CountryPhoneInfo("Индонезия", "🇮🇩", "62", 9, 11, "8123456789"),
        "CU" to CountryPhoneInfo("Куба", "🇨🇺", "53", 8, 8, "51234567"),
        "KH" to CountryPhoneInfo("Камбоджа", "🇰🇭", "855", 8, 9, "12123456"),
        "VN" to CountryPhoneInfo("Вьетнам", "🇻🇳", "84", 9, 9, "912345678"),
        "AF" to CountryPhoneInfo("Афганистан", "🇦🇫", "93", 9, 9, "701234567"),
        "BO" to CountryPhoneInfo("Боливия", "🇧🇴", "591", 8, 8, "71234567"),
        "CD" to CountryPhoneInfo("ДР Конго", "🇨🇩", "243", 9, 9, "812345678"),
        "CG" to CountryPhoneInfo("Республика Конго", "🇨🇬", "242", 9, 9, "061234567"),
        "CO" to CountryPhoneInfo("Колумбия", "🇨🇴", "57", 10, 10, "3001234567"),
        "GD" to CountryPhoneInfo("Гренада", "🇬🇩", "1", 10, 10, "4731234567"),
        "GM" to CountryPhoneInfo("Гамбия", "🇬🇲", "220", 7, 7, "3012345"),
        "IN" to CountryPhoneInfo("Индия", "🇮🇳", "91", 10, 10, "9812345678"),
        "IQ" to CountryPhoneInfo("Ирак", "🇮🇶", "964", 10, 10, "7501234567"),
        "KN" to CountryPhoneInfo("Сент-Китс и Невис", "🇰🇳", "1", 10, 10, "8691234567"),
        "KW" to CountryPhoneInfo("Кувейт", "🇰🇼", "965", 8, 8, "50123456"),
        "LB" to CountryPhoneInfo("Ливан", "🇱🇧", "961", 8, 8, "71123456"),
        "MM" to CountryPhoneInfo("Мьянма", "🇲🇲", "95", 9, 10, "912345678"),
        "NI" to CountryPhoneInfo("Никарагуа", "🇳🇮", "505", 8, 8, "81234567"),
        "PK" to CountryPhoneInfo("Пакистан", "🇵🇰", "92", 10, 10, "3001234567"),
        "PW" to CountryPhoneInfo("Палау", "🇵🇼", "680", 7, 7, "4881234"),
        "QA" to CountryPhoneInfo("Катар", "🇶🇦", "974", 8, 8, "33123456"),
        "SA" to CountryPhoneInfo("Саудовская Аравия", "🇸🇦", "966", 9, 9, "501234567"),
        "VE" to CountryPhoneInfo("Венесуэла", "🇻🇪", "58", 10, 10, "4121234567"),
        "TZ" to CountryPhoneInfo("Танзания", "🇹🇿", "255", 9, 9, "712345678"),
        "EG" to CountryPhoneInfo("Египет", "🇪🇬", "20", 10, 10, "1012345678"),
        "CN" to CountryPhoneInfo("Китай", "🇨🇳", "86", 11, 11, "13812345678"),
        "ZA" to CountryPhoneInfo("ЮАР", "🇿🇦", "27", 9, 9, "821234567"),
        "BR" to CountryPhoneInfo("Бразилия", "🇧🇷", "55", 10, 11, "11987654321"),
        "RU" to CountryPhoneInfo("Россия", "🇷🇺", "7", 10, 10, "9991234567"),
        "BY" to CountryPhoneInfo("Беларусь", "🇧🇾", "375", 9, 9, "291234567")
    )

    val audioExtensions = listOf(
        "mp3", "aac", "m4a", "ogg", "oga", "opus", "wma", "amr", "3gp",
        "flac", "alac", "ape", "wav", "aiff", "aif", "aifc", "wv", "tta", "tak", "shn",
        "dsf", "dff", "dsd", "pcm", "dxp", "pt24",
        "mid", "midi", "rmi", "kar",
        "mod", "xm", "s3m", "it", "mtm", "umx", "mo3",
        "caf", "au", "snd", "ra", "rm", "mka", "weba", "ac3", "eac3", "dts", "m4b",
        "voc", "8svx", "cda", "gsm", "mpc", "spx", "la"
    )

    fun getTimeFromMillis(millis : Long) : String {
        var seconds = millis / 1000
        var minutes = (seconds / 60).toInt()

        if (minutes > 0) {
            seconds -= minutes * 60
        }

        val minutesString = if (minutes < 10) "0$minutes" else "$minutes"
        val secondsString = if (seconds < 10) "0$seconds" else "$seconds"

        return "$minutesString:$secondsString"
    }
    fun getFullName(userID: Long) : String {
        val users = UsersManager.usersList.value

        if (!users.contains(userID)) {
            UsersManager.checkForExisting(userID)
        }

        val user = users[userID]

        return getFullName(user)
    }

    fun getFullName(user : User?) : String {
        if (user == null || user.firstName == "") {
            return ""
        }

        var fullName = user.firstName + if (!user.lastName.isNullOrEmpty()) {
            " " + user.lastName
        } else {
            ""
        }

        return fullName
    }

    fun getIconForFile(file : String) : ImageVector {
        val extension = File(file).extension

        if (extension == "txt") {
            return Icons.AutoMirrored.Filled.Article
        }

        for (i in audioExtensions) {
            if (extension == i) {
                return Icons.Filled.AudioFile
            }
        }

        if (extension == "jpg" || extension == "png") {
            return Icons.Filled.Photo
        }

        return Icons.AutoMirrored.Filled.InsertDriveFile
    }

    fun getSizeFromBytes(bytes : Long) : String {
        if (bytes <= 1000) {
            return "$bytes B"
        }

        if (bytes in 1000..<1000000) {
            val kb = bytes / 1000

            return "$kb KB"
        }

        if (bytes > 1000000) {
            val mb = bytes / 1000000

            return "$mb MB"
        }

        return "$bytes"
    }
    fun getColorForAvatar(avatar: String): Pair<Color, Color> {
        val colors = listOf(
            Pair(Color(0xFFF85858), Color(0xFFFF6696)),
            Pair(Color(0xFFFFCE38), Color(0xFFFFE59F)),
            Pair(Color(0xFF2182FF), Color(0xFF5EA7FF)),
            Pair(Color(0xFF4BFF5A), Color(0xFFA4FFAC)),
            Pair(Color(0xFFA308C4), Color(0xFFE071FC)),
        )

        val index = (avatar.hashCode().absoluteValue) % colors.size

        return colors[index]
    }

    fun getColorFoName(name: String): Color {
        val colors = listOf(
            Color(0xff7aadff),
            Color(0xFFFF6F6F),
            Color(0xFFba52fa),
            Color(0xFFfaca52),
            Color(0xFF52fa79),
            Color(0xFFfcb058),
            Color(0xFFCAFFF9),
            Color(0xFF585bfc),
        )
        val index = (name.hashCode().absoluteValue) % colors.size

        return colors[index]
    }

    fun getEventString(message: Message, chatType : String) : String {
        var text = ""
        val attach = message.attaches?.jsonArray?.last()
        val event = attach?.jsonObject["event"]?.jsonPrimitive?.content
        val users = UsersManager.usersList.value

        val lastUser = users[message.senderID]
        val lastUserStr = lastUser?.firstName + if (!lastUser?.lastName.isNullOrEmpty()) {
            " " + lastUser.lastName
        } else {
            ""
        }

        when (event) {
            "botStarted" -> {
                text += "Вы начали общение с ботом"
            }

            "remove" -> {
                val peoplesRemoved =
                    attach?.jsonObject["userId"]?.jsonPrimitive?.long

                UsersManager.checkForExisting(peoplesRemoved!!)
                UsersManager.checkForExisting(message.senderID!!)

                var whomAdded = users[peoplesRemoved]?.firstName.toString()

                if (users[peoplesRemoved]?.lastName?.isNotEmpty() == true) {
                    whomAdded += " " + users[peoplesRemoved]?.lastName
                }
                if (message.senderID == AccountManager.accountID) {
                    text += "Вы удалили $whomAdded"
                } else {
                    var whoAdded =
                        users[message.senderID]?.firstName.toString()

                    if (users[message.senderID]?.firstName?.isNotEmpty() == true) {
                        whoAdded += " " + users[message.senderID]?.lastName
                    }

                    text += "$whoAdded удалил(-а) $whomAdded"
                }
            }

            "new" -> {
                UsersManager.checkForExisting(message.senderID!!)
                if (chatType != "CHANNEL") {
                    if (message.senderID == AccountManager.accountID) {
                        text += "Вы создали чат"
                    } else {
                        text += "$lastUserStr создал(-а) чат"
                    }
                } else {
                    text += "Канал создан"
                }
            }

            "add" -> {
                val peoplesAdded = attach.jsonObject["userIds"]?.jsonArray

                for (i in peoplesAdded!!) {
                    if (attach.jsonObject["userIds"]?.jsonArray?.isNotEmpty() == true) {
                        UsersManager.checkForExisting(i.jsonPrimitive.long)
                    }
                }

                UsersManager.checkForExisting(message.senderID!!)

                if (message.senderID == AccountManager.accountID) {
                    text += "Вы добавили "
                } else {
                    var whoAdded =
                        users[message.senderID]?.firstName.toString()

                    if (users[message.senderID]?.firstName?.isNotEmpty() == true) {
                        whoAdded += " " + users[message.senderID]?.lastName
                    }

                    text += "$whoAdded добавил(-а) "
                }

                for (i in peoplesAdded) {
                    var whomAdded =
                        users[i.jsonPrimitive.long]?.firstName.toString()

                    if (users[i.jsonPrimitive.long]?.lastName?.isNotEmpty() == true) {
                        whomAdded += " " + users[i.jsonPrimitive.long]?.lastName
                    }

                    text += whomAdded
                    if (i != peoplesAdded.last()) {
                        text += ", "
                    }
                }
            }

            "icon" -> {
                UsersManager.checkForExisting(message.senderID!!)

                if (message.senderID == AccountManager.accountID) {
                    text += "Вы изменили фото чата"
                } else {
                    text += "$lastUserStr изменил(-а) фото чата"
                }
            }

            "title" -> {
                UsersManager.checkForExisting(message.senderID!!)
                val newTitle =
                    attach.jsonObject["title"]?.jsonPrimitive?.content

                if (message.senderID == AccountManager.accountID) {
                    text += "Вы изменили название чата на «$newTitle»"
                } else {
                    text += "$lastUserStr изменил(-а) название чата на «$newTitle»"
                }
            }

            "leave" -> {
                UsersManager.checkForExisting(message.senderID!!)

                if (message.senderID == AccountManager.accountID) {
                    text += "Вы покинули чат"
                } else {
                    text += "$lastUserStr покинул(-а) чат"
                }
            }

            "joinByLink" -> {
                UsersManager.checkForExisting(attach.jsonObject["userId"]?.jsonPrimitive?.long!!)

                if (message.senderID == AccountManager.accountID) {
                    text += "Вы присоединились к чату"
                } else {
                    text += "$lastUserStr присоединился(-ась) к чату"
                }
            }

            "system" -> {
                text += attach.jsonObject["message"]?.jsonPrimitive?.content
            }
        }

        return text
    }

    @Composable
    fun AvatarFromName(name : String, avatarUrl : String?, size : Int, modifier : Modifier = Modifier) {
        if (!avatarUrl.isNullOrEmpty()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "avatarUrl",
                modifier = modifier
                    .size(size.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            val initial =
                name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("")
                    .uppercase(LocalLocale.current.platformLocale)

            Box(
                contentAlignment = Alignment.Center,
                modifier = modifier
                    .size(size.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                getColorForAvatar(name).first,
                                getColorForAvatar(name).second
                            )
                        )
                    ),

                ) {
                Text(
                    text = initial,
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 25.sp
                )
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    fun getTime(time : Long) : LocalDateTime {
        val instantLast = fromEpochMilliseconds(time)
        return instantLast.toLocalDateTime(TimeZone.currentSystemDefault())
    }

    fun formatHourAndMinute(minute : Int = 0, hour : Int = 0) : String {
        if (hour != 0) {
            return when (hour.toString().last().toString().toInt()) {
                1 -> "час"
                2 -> "часа"
                3 -> "часа"
                4 -> "часа"
                else -> "часов"
            }
        } else {
            return if (minute == 11 || minute == 13 || minute == 14) {
                "минут"
            } else {
                when (minute.toString().last().toString().toInt()) {
                    1 -> "минуту"
                    2 -> "минуты"
                    3 -> "минуты"
                    4 -> "минуты"
                    else -> "минут"
                }
            }
        }
    }

    fun formatMembersCount(membersCount : Int, membersType : Int) : String {
        if (membersType == 0) {
            return if (membersCount == 11 || membersCount == 12 || membersCount == 13 || membersCount == 14) {
                "участников"
            } else {
                when (membersCount.toString().last().toString().toInt()) {
                    1 -> "участник"
                    2, 3, 4 -> "участника"
                    5, 6, 7, 8, 9, 0 -> "участников"
                    else -> "участников"
                }
            }
        } else {
            return if (membersCount == 11 || membersCount == 12 || membersCount == 13 || membersCount == 14) {
                "подписчиков"
            } else {
                when (membersCount.toString().last().toString().toInt()) {
                    1 -> "подписчик"
                    2, 3, 4 -> "подписчика"
                    5, 6, 7, 8, 9, 0 -> "подписчиков"
                    else -> "подписчиков"
                }
            }
        }
    }

    fun getStatusString(status : Pair<Long, Int>) : String {
        if (status.second != 0) {
            return when (status.second) {
                1 -> "В сети"
                2 -> "Был(-а) недавно"
                3 -> "Был(-а) давно"
                else -> ""
            }
        } else if (status.first != 0L) {
            val timeNow = getTime(Date().time)
            val time = getTime(status.first * 1000)

            return if (time.date == timeNow.date) {
                if (timeNow.minute == time.minute) {
                    "Был(-а) только что"
                } else if (time.hour == timeNow.hour) {
                    "Был(-а) ${timeNow.minute - time.minute} ${formatHourAndMinute(minute = timeNow.minute - time.minute)} назад"
                } else {
                    "Был(-а) ${timeNow.hour - time.hour} ${formatHourAndMinute(hour = timeNow.hour - time.hour)} назад"
                }
            } else {
                "Был(-а) ${time.date}"
            }
        } else {
            return ""
        }
    }
    @OptIn(ExperimentalTime::class)
    fun getTimeString(time : Long, onlyHours : Boolean = true, styleShort : Boolean = true) : String {
        val currentTime = Date().time

        val duration = Duration.ofSeconds(currentTime / 1000 - time / 1000)

        val localDateTime = getTime(time)

        val time = if (duration.toHours() < 24 || onlyHours) {
            val hours = if (localDateTime.hour < 10) "0${localDateTime.hour}" else localDateTime.hour
            val minutes = if (localDateTime.minute < 10) "0${localDateTime.minute}" else localDateTime.minute

            "${hours}:${minutes}"
        } else if (duration.toHours() >= 24 && duration.toDays() < 7) {
            val dayOfWeek = localDateTime.dayOfWeek.toJavaDayOfWeek().getDisplayName(
                if (styleShort) TextStyle.SHORT else TextStyle.FULL, getDefault()
            )

            dayOfWeek.toString().replaceFirstChar { it.titlecase(getDefault()) }
        } else {
            val day = if (localDateTime.day < 10) "0${localDateTime.day}" else localDateTime.day
            val month = if (localDateTime.month.ordinal < 9) "0${localDateTime.month.ordinal + 1}" else localDateTime.month.ordinal + 1
            "$day.$month.${localDateTime.year}"
        }

        return time
    }
}