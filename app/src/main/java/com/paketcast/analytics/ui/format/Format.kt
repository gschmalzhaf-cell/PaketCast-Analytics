package com.paketcast.analytics.ui.format

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Format {

    fun duration(sec: Long): String {
        if (sec <= 0) return "0 min"
        val hours = sec / 3600
        val minutes = (sec % 3600) / 60
        return when {
            hours > 0 && minutes > 0 -> "%d h %d min".format(Locale.GERMANY, hours, minutes)
            hours > 0 -> "%d h".format(Locale.GERMANY, hours)
            else -> "%d min".format(Locale.GERMANY, minutes)
        }
    }

    fun secondsShort(sec: Long): String {
        if (sec <= 0) return "0 s"
        val hours = sec / 3600
        val minutes = (sec % 3600) / 60
        val seconds = sec % 60
        return when {
            hours > 0 -> "%d:%02d:%02d".format(Locale.GERMANY, hours, minutes, seconds)
            minutes > 0 -> "%d:%02d".format(Locale.GERMANY, minutes, seconds)
            else -> "%d s".format(Locale.GERMANY, seconds)
        }
    }

    fun date(epochSec: Long?): String {
        if (epochSec == null || epochSec <= 0) return "–"
        val millis = if (epochSec > 10_000_000_000L) epochSec else epochSec * 1000L
        return SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(Date(millis))
    }

    fun percent(ratio: Double): String = "%d %%".format(Locale.GERMANY, (ratio * 100).toInt())
}
