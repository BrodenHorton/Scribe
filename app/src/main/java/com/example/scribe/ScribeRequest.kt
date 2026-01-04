package com.example.scribe

class ScribeRequest {
    var date: RequestDate
    var speechLines: Array<SpeechLineRequest>

    constructor(date: RequestDate, speechLines: Array<SpeechLineRequest>) {
        this.date = date
        this.speechLines = speechLines
    }
}