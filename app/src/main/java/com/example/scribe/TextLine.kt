package com.example.scribe

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

class TextLine {
    var text: MutableState<String>
    var created: Long

    constructor(text: String) {
        this.text = mutableStateOf(text)
        created = System.currentTimeMillis()
    }
}