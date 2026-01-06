package com.example.scribe

import androidx.compose.runtime.mutableStateListOf

class SpeechBlock {
    var speaker: Int
    var speechLines: MutableList<SpeechLine>

    constructor(speaker: Int) {
        this.speaker = speaker
        this.speechLines = mutableStateListOf()
    }

    constructor(speechLineRequest: SpeechLineRequest) {
        this.speaker = speechLineRequest.speaker
        this.speechLines = mutableStateListOf(SpeechLine(speechLineRequest))
    }
}