package com.example.scribe

class SpeechLineRequest {
    var speaker: Int
    var blockUuid: String
    var text: String

    constructor(speaker: Int, blockUuid: String, text: String) {
        this.speaker = speaker
        this.blockUuid = blockUuid
        this.text = text
    }
}