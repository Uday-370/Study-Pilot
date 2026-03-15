package com.example.studysmart.data.local

import androidx.room.TypeConverter

class ColorListConverter {

    // Converts the List into a single String to save in the database
    @TypeConverter
    fun fromColorList(colors: List<Int>): String {
        return colors.joinToString(",")
    }

    // Converts the String back into a List when reading from the database
    @TypeConverter
    fun toColorList(colorString: String): List<Int> {
        if (colorString.isBlank()) return emptyList()
        return colorString.split(",").map { it.toInt() }
    }
}