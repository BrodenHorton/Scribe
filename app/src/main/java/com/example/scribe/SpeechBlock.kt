package com.example.scribe

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

class SpeechBlock {
    var speaker: Int
    var blockUuid: String
    var text: MutableState<String> = mutableStateOf("")

    constructor(speaker: Int, blockUuid: String, text: String) {
        this.speaker = speaker
        this.blockUuid = blockUuid
        this.text.value = text
    }

    constructor(speechLineRequest: SpeechLineRequest) {
        this.speaker = speechLineRequest.speaker
        this.blockUuid = speechLineRequest.blockUuid
        this.text.value = speechLineRequest.text
    }
}