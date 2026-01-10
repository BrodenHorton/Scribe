package com.example.scribe

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color

class Speaker {
    var name: MutableState<String>
    var color: MutableState<Color>

    constructor(name: String, color: Color) {
        this.name = mutableStateOf(name)
        this.color = mutableStateOf(color)
    }
}