package com.example.scribe

class SpeechLineRequest {
    var speaker: Int
    var lineUuid: String
    var text: String

    constructor(speaker: Int, lineUuid: String, text: String) {
        this.speaker = speaker
        this.lineUuid = lineUuid
        this.text = text
    }
}