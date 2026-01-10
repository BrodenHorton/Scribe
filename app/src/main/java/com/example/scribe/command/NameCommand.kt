package com.example.scribe.command

import android.util.Log
import com.example.scribe.MainActivity
import com.example.scribe.Speaker

class NameCommand : SpeechCommand("name") {
    val nameCorrections: MutableMap<String, String> = mutableMapOf(
        Pair("brodie", "brody"),
        Pair("vicky", "vicki"))

    override fun execute(mt: MainActivity, speaker: Speaker, args: List<String>): String {
        if(args.isEmpty()) {
            Log.i("NameCommand", "args empty")
            return "Name Command: Empty args. Command syntax \"Command Name <name>\""
        }

        var oldName = speaker.name.value
        var name = ""
        for(str in args) {
            var word = if(nameCorrections.contains(str)) nameCorrections[str] else str
            name += "${word!!.capitalize()} "
        }
        speaker.name.value = name
        return "$oldName changed their name to ${speaker.name.value}"
    }
}