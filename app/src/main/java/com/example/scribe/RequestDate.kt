package com.example.scribe

class RequestDate {
    var year: Int
    var month: Int
    var day: Int
    var hours: Int
    var minutes: Int
    var seconds: Int
    var milliseconds: Int
    var timeZoneOffset: Int

    constructor(year: Int, month: Int, day: Int, hours: Int, minutes: Int, seconds: Int, milliseconds: Int, timeZoneOffset: Int) {
        this.year = year
        this.month = month
        this.day = day
        this.hours = hours
        this.minutes = minutes
        this.seconds = seconds
        this.milliseconds = milliseconds
        this.timeZoneOffset = timeZoneOffset
    }

    fun getDateQueryParam(): String {
        val monthParam = if (month >= 10) month.toString() else addLeadingZero(month, 1)
        val dayParam = if (day >= 10) day.toString() else addLeadingZero(day, 1)
        val hoursParam = if (hours >= 10) hours.toString() else addLeadingZero(hours, 1)
        val minutesParam = if (minutes >= 10) minutes.toString() else addLeadingZero(minutes, 1)
        val secondsParam = if (seconds >= 10) seconds.toString() else addLeadingZero(seconds, 1)
        val millisecondsParam = if (milliseconds >= 100) milliseconds.toString() else addLeadingZero(milliseconds, 3 - milliseconds.toString().length)

        return "${year}-${monthParam}-${dayParam}T${hoursParam}%3A${minutesParam}%3A${secondsParam}.${millisecondsParam}${getUTCOffset()}"
    }

    fun addLeadingZero(value: Int, leadingZerosCount: Int): String {
        var result = ""
        for(i in 1 .. leadingZerosCount)
            result += "0"
        result += value.toString()

        return result
    }

    fun getUTCOffset(): String {
        val hoursOffset = timeZoneOffset / 60
        val hoursOffsetParam = if(hoursOffset >= 10) hoursOffset.toString() else addLeadingZero(hoursOffset, 1)
        val minutesOffset = timeZoneOffset % 60
        val minutesOffsetParam = if(minutesOffset >= 10) minutesOffset.toString() else addLeadingZero(minutesOffset, 1)
        val offsetSign = if(timeZoneOffset >= 0) "%2D" else "%2B"

        return "${offsetSign}${hoursOffsetParam}%3A${minutesOffsetParam}"
    }
}