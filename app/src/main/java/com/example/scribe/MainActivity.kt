package com.example.scribe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
        setContent {
            BackgroundColumn {
                for(i in 1 .. 4)
                    SpeechBubble("This is my first message!")
            }
        }

    }
}

@Composable
fun BackgroundColumn(content: @Composable (ColumnScope.() -> Unit)) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(30.dp),
        content = content
    )
}

@Composable
fun SpeechBubble(text: String) {
    Box(
        contentAlignment = Alignment.TopStart,
        modifier = Modifier
            .background(Color.Green, shape = RoundedCornerShape(20.dp))
            .size(315.dp, 150.dp) // Define a specific size
            .padding(18.dp)
    ) {
        Text(
            text = text,
            fontSize = 20.sp
            //textAlign = TextAlign.Center,
        )
    }
}