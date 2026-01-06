package com.example.scribe

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

class SpeechLine {
    var speaker: Int
    var lineUuid: String
    var text: MutableState<String>

    constructor(speaker: Int, lineUuid: String, text: String) {
        this.speaker = speaker
        this.lineUuid = lineUuid
        this.text = mutableStateOf(text)
    }

    constructor(speechLineRequest: SpeechLineRequest) {
        this.speaker = speechLineRequest.speaker
        this.lineUuid = speechLineRequest.lineUuid
        this.text = mutableStateOf(speechLineRequest.text)
    }
}