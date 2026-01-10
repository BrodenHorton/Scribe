package com.example.scribe.command

import com.example.scribe.MainActivity
import com.example.scribe.Speaker

abstract class SpeechCommand(var cmd: String) {
    abstract fun execute(mt: MainActivity, speaker: Speaker, args: List<String>): String
}