package com.example.scribe

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

class SpeechLine {
    var speaker: Int
    var lineUuid: String
    var text: MutableState<String>
    var isFinalized: Boolean
    var created: Long

    constructor(speaker: Int, lineUuid: String, text: String, isFinalized: Boolean) {
        this.speaker = speaker
        this.lineUuid = lineUuid
        this.text = mutableStateOf(text)
        this.isFinalized = isFinalized
        created = System.currentTimeMillis()
    }

    constructor(speechLineRequest: SpeechLineRequest) {
        this.speaker = speechLineRequest.speaker
        this.lineUuid = speechLineRequest.lineUuid
        this.text = mutableStateOf(speechLineRequest.text)
        this.isFinalized = speechLineRequest.isFinalized
        created = System.currentTimeMillis()
    }
}