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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    var speechBlocks: MutableList<SpeechBlock> = mutableStateListOf()
    var lastSpeechBlockBySpeaker: MutableMap<Int, SpeechBlock> = mutableMapOf()
    lateinit var lastRequested: RequestDate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            speechBlocksLazyColumn(speechBlocks)
        }

        val fetchRequestScope = CoroutineScope(Dispatchers.IO)
        fetchRequestScope.launch {
            Log.i("Coroutine", "Fetching from API /all")
            getInitialSpeechBlocks()
            Log.i("Coroutine", "Fetching from API /after")
            getSpeechBlocks()
        }
    }

    fun getInitialSpeechBlocks() {
        val url = URL("http://10.0.2.2:3000/all")
        val connection = url.openConnection() as HttpURLConnection
        Log.i("/all Request", "All 1")
        if(connection.responseCode == 200) {
            val inputStream = connection.getInputStream()
            val inputStreamReader = InputStreamReader(inputStream, "UTF-8")
            val scribeRequest = Gson().fromJson(inputStreamReader, ScribeRequest::class.java)
            inputStreamReader.close()
            inputStream.close()
            Log.i("/all Request", "All 2")
            lastRequested = scribeRequest.date
            Log.i("/all Request", "All 3")
            Log.i("/all Request", "Date: ${lastRequested.getDateQueryParam()}")
            for(speechLineRequest in scribeRequest.speechLines)
                addSpeechLine(speechLineRequest)
        }
        else {
            Log.i("Coroutine", "Error when sending GET request to endpoint /all from Scribe Server")
        }
    }

    suspend fun getSpeechBlocks() {
        var isActive = true
        while(isActive) {
            val url = URL("http://10.0.2.2:3000/after?lastRequested=${lastRequested.getDateQueryParam()}")
            val connection = url.openConnection() as HttpURLConnection

            if(connection.responseCode == 200) {
                val inputStream = connection.getInputStream()
                val inputStreamReader = InputStreamReader(inputStream, "UTF-8")
                val scribeRequest = Gson().fromJson(inputStreamReader, ScribeRequest::class.java)
                inputStreamReader.close()
                inputStream.close()
                lastRequested = scribeRequest.date
                for(speechLineRequest in scribeRequest.speechLines)
                    addSpeechLine(speechLineRequest)
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

    fun addSpeechLine(speechLineRequest: SpeechLineRequest) {
        if(!speechBlocks.isEmpty() && lastSpeechBlockBySpeaker.contains(speechLineRequest.speaker)) {
            var speechBlock = lastSpeechBlockBySpeaker[speechLineRequest.speaker]
            if(speechBlock?.speechLines?.last()?.lineUuid == speechLineRequest.lineUuid) {
                speechBlock.speechLines.last().text.value = speechLineRequest.text
                return
            }
        }

        if(!speechBlocks.isEmpty() && speechBlocks.last().speaker == speechLineRequest.speaker) {
            speechBlocks.last().speechLines.add(SpeechLine(speechLineRequest))
        }
        else {
            val nextSpeechBlock = SpeechBlock(speechLineRequest)
            speechBlocks.add(nextSpeechBlock)
            lastSpeechBlockBySpeaker[speechLineRequest.speaker] = nextSpeechBlock
        }
    }
}

@Composable
fun speechBlocksLazyColumn(input: MutableList<SpeechBlock>) {
    var speechBlocks = remember { input }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(30.dp)
    ) {
        var count = 0
        items(speechBlocks) { speechBlock ->
            SpeechBlockComponent(speechBlock)
            count++
        }
    }
}

@Composable
fun SpeechBlockComponent(speechBlock: SpeechBlock) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = speechBlock.speaker.toString(),
                fontWeight = FontWeight.Medium,
                color = if (speechBlock.speaker.toString() == "0") Color.Green else Color.Blue,
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
                        .background(color = if (speechBlock.speaker.toString() == "0") Color.Green else Color.Blue)
                        .width(4.dp)
                        .fillMaxHeight(1f)
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for(speechLine in speechBlock.speechLines) {
                        Text(
                            text = speechLine.text.value,
                            fontSize = 20.sp,
                        )
                    }
                }
            }
        }
    }
}