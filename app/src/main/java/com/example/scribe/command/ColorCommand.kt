package com.example.scribe.command

import android.util.Log
import androidx.compose.ui.graphics.Color
import com.example.scribe.MainActivity
import com.example.scribe.Speaker

class ColorCommand : SpeechCommand("color") {
    val colorByName: MutableMap<String, Color> = mutableMapOf(
        Pair("red", Color.Red),
        Pair("blue", Color.Blue),
        Pair("yellow", Color(0xFFecf002)),
        Pair("green", Color(0xFF04d643)),
        Pair("purple", Color(0xFFab02e3)),
        Pair("pink", Color(0xFFf50a93)),
        Pair("cyan", Color(0xFF02c2b5)))

    override fun execute(mt: MainActivity, speaker: Speaker, args: List<String>): String {
        if(args.isEmpty()) {
            Log.i("ColorCommand", "args empty")
            return "Color Command: Empty args. Command syntax \"Command Color <color>\""
        }
        if(!colorByName.contains(args[0]) || colorByName[args[0]] == null) {
            Log.i("ColorCommand", "Color not found")
            return "Color Command: ${args[0].capitalize()} is not a valid color."
        }

        speaker.color.value = colorByName[args[0]]!!
        return "${speaker.name.value.capitalize()} changed their color to ${args[0].capitalize()}"
    }
}