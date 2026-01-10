package com.example.scribe.command

import android.util.Log
import androidx.compose.ui.graphics.Color
import com.example.scribe.MainActivity
import com.example.scribe.Speaker

class ColorCommand : SpeechCommand("color") {
    val colorByName: MutableMap<String, Color> = mutableMapOf(
        Pair("red", Color.Red),
        Pair("blue", Color.Blue),
        Pair("yellow", Color.Yellow),
        Pair("green", Color.Green),
        Pair("purple", Color(0xFFab02e3)),
        Pair("pink", Color(0xFFe05596)),
        Pair("cyan", Color(0xFF02c2b5)))

    override fun execute(mt: MainActivity, speaker: Speaker, args: List<String>) {
        Log.i("ColorCommand", "Color Command Executed!")
        if(args.isEmpty()) {
            Log.i("ColorCommand", "args empty")
            return
        }

        if(colorByName.contains(args[0]) && colorByName[args[0]] != null)
            speaker.color.value = colorByName[args[0]]!!
        else
            Log.i("ColorCommand", "Color not found")

    }
}