package com.example.scribe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        var speechBlocks: MutableList<SpeechBlock> = mutableListOf()
        setContent {
            speechBlocksLazyColumn(speechBlocks)
        }
        for(i in 1 .. 20)
            speechBlocks.add(SpeechBlock(0, i.toString(), "This is speech block ${i}!"))

    }
}

@Composable
fun speechBlocksLazyColumn(speechBlocks: MutableList<SpeechBlock>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(30.dp),
    ) {
        var count = 0
        items(speechBlocks) { speechBlock ->
            val alignment: Alignment = if(count % 2 == 0) Alignment.TopStart else Alignment.TopEnd
            SpeechBubble(speechBlock.text, alignment)
            count++
        }
    }
}

@Composable
fun SpeechBubble(text: String, boxAlignment: Alignment) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            contentAlignment = Alignment.TopStart,
            modifier = Modifier
                .background(Color.Green, shape = RoundedCornerShape(20.dp))
                .sizeIn(maxWidth = 315.dp)
                .padding(18.dp)
                .align(boxAlignment)
        ) {
            Text(
                text = text,
                fontSize = 20.sp,
            )
        }
    }
}