package com.example.scribe

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    var speechBlocks: MutableList<SpeechBlock> = mutableListOf()
    var lastRequested: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            speechBlocksLazyColumn(speechBlocks)
        }
        /*for(i in 1 .. 20)
            speechBlocks.add(SpeechBlock(0, i.toString(), "This is speech block ${i}!"))*/

        val initialRequestScope = CoroutineScope(Dispatchers.IO)
        initialRequestScope.launch {
            getInitialSpeechBlocks()
            initialRequestScope.cancel()
        }

        val fetchRecentSpeechBlocksScope = CoroutineScope(Dispatchers.IO)
        fetchRecentSpeechBlocksScope.launch {
            getSpeechBlocks("")
        }
    }

    fun getInitialSpeechBlocks() {
        val url = URL("http://10.0.2.2:3000/all")
        val connection = url.openConnection() as HttpURLConnection

        if(connection.responseCode == 200) {
            Log.i("Coroutine", "Fetching from API /all")
            val inputStream = connection.getInputStream()
            val inputStreamReader = InputStreamReader(inputStream, "UTF-8")
            val scribeRequest = Gson().fromJson(inputStreamReader, ScribeRequest::class.java)
            inputStreamReader.close()
            inputStream.close()
            lastRequested = scribeRequest.date
            for(speechBlock in scribeRequest.speechBlocks) {
                speechBlocks.add(speechBlock)
            }
        }
        else {
            Log.i("Coroutine", "Error when sending GET request to endpoint /all from Scribe Server")
        }
    }

    suspend fun getSpeechBlocks(dateTime: String) {
        var isActive = true
        while(isActive) {
            val url = URL("http://10.0.2.2:3000/after?lastRequested=${dateTime}")
            val connection = url.openConnection() as HttpURLConnection

            if(connection.responseCode == 200) {
                Log.i("Coroutine", "Fetching from API /after")
                val inputStream = connection.getInputStream()
                val inputStreamReader = InputStreamReader(inputStream, "UTF-8")
                val scribeRequest = Gson().fromJson(inputStreamReader, ScribeRequest::class.java)
                inputStreamReader.close()
                inputStream.close()


            }
            else {
                Log.i("Coroutine", "Error in request to endpoint /after from Scribe Server")
                isActive = false
            }

            if(isActive) {
                delay(300)
            }
        }
    }
}

@Composable
fun speechBlocksLazyColumn(speechBlocks: MutableList<SpeechBlock>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(30.dp)
    ) {
        var count = 0
        items(speechBlocks) { speechBlock ->
            SpeechBubble(speechBlock)
            count++
        }
    }
}

@Composable
fun SpeechBubble(speechBlock: SpeechBlock) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = speechBlock.speaker.toString(),
                fontWeight = FontWeight.Medium,
                color = Color.Green,
                fontSize = 18.sp,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max)
            ) {
                Box(
                    modifier = Modifier
                        .background(color = Color.Green)
                        .width(4.dp)
                        .fillMaxHeight(1f)
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = speechBlock.text,
                        fontSize = 20.sp,
                    )
                    Text(
                        text = "Text item 2!",
                        fontSize = 20.sp,
                    )
                }
            }
        }
    }
}