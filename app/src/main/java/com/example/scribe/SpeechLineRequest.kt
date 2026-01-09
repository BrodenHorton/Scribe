package com.example.scribe

class SpeechLineRequest(var speaker: Int,
                        var lineUuid: String,
                        var text: String,
                        var isFinalized: Boolean)