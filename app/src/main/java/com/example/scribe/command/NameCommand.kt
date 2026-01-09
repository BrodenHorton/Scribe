package com.example.scribe.command

import android.util.Log
import com.example.scribe.MainActivity
import com.example.scribe.Speaker

class NameCommand : SpeechCommand("name") {

    override fun execute(mt: MainActivity, speaker: Speaker, args: List<String>) {
        if(args.isEmpty()) {
            Log.i("NameCommand", "args empty")
            return
        }

        var name = ""
        for(str in args)
            name += "$str "
        speaker.name = name
    }
}