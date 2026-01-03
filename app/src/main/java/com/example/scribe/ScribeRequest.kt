package com.example.scribe

class ScribeRequest {
    var date: String
    var speechBlocks: Array<SpeechBlock>

    constructor(date: String, speechBlocks: Array<SpeechBlock>) {
        this.date = date
        this.speechBlocks = speechBlocks
    }
}