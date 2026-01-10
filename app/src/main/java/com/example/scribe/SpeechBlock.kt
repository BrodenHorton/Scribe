package com.example.scribe

import androidx.compose.runtime.mutableStateListOf

class SpeechBlock {
    var speaker: Int
    var speechLines: MutableList<SpeechLine>
    var created: Long

    constructor(speaker: Int) {
        this.speaker = speaker
        this.speechLines = mutableStateListOf()
        created = System.currentTimeMillis()
    }

    constructor(speechLineRequest: SpeechLineRequest) {
        this.speaker = speechLineRequest.speaker
        this.speechLines = mutableStateListOf(SpeechLine(speechLineRequest))
        created = System.currentTimeMillis()
    }
}