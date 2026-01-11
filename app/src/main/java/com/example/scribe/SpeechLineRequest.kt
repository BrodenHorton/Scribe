package com.example.scribe

class SpeechLineRequest(var speaker: Int,
                        var lineUuid: String,
                        var text: String,
                        var created: RequestDate,
                        var isFinalized: Boolean) {

    fun copy(): SpeechLineRequest {
        return SpeechLineRequest(speaker, lineUuid, text, created, isFinalized)
    }
}