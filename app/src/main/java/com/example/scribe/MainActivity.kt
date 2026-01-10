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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFrom
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scribe.command.ColorCommand
import com.example.scribe.command.NameCommand
import com.example.scribe.command.SpeechCommand
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
    var speakerByIndex: MutableMap<Int, Speaker> = mutableMapOf()
    var inProgressCommandByUuid: MutableMap<String, SpeechLine> = mutableMapOf()
    var speechCommandByName: MutableMap<String, SpeechCommand> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            speechBlocksLazyColumn(speechBlocks)
        }

        registerCommands()

        val fetchRequestScope = CoroutineScope(Dispatchers.IO)
        fetchRequestScope.launch {
            Log.i("Coroutine", "Fetching from API /all")
            getInitialSpeechBlocks()
            Log.i("Coroutine", "Fetching from API /after")
            getSpeechBlocks()
        }
    }

    fun registerCommands() {
        val nameCommand = NameCommand()
        speechCommandByName[nameCommand.cmd] = nameCommand
        val colorCommand = ColorCommand()
        speechCommandByName[colorCommand.cmd] = colorCommand
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
            for(speechLineRequest in scribeRequest.speechLines) {
                if(!speakerByIndex.contains(speechLineRequest.speaker))
                    speakerByIndex[speechLineRequest.speaker] = Speaker("Speaker ${speechLineRequest.speaker}", Color.LightGray)

                if(isSpeechCommand(speechLineRequest.text))
                    inProgressCommandByUuid[speechLineRequest.lineUuid] = SpeechLine(speechLineRequest)
                else
                    addSpeechLine(speechLineRequest)
            }
            flushSpeechCommands()
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
                for(speechLineRequest in scribeRequest.speechLines) {
                    if(!speakerByIndex.contains(speechLineRequest.speaker))
                        speakerByIndex[speechLineRequest.speaker] = Speaker("Speaker ${speechLineRequest.speaker}", Color.LightGray)

                    if(isSpeechCommand(speechLineRequest.text))
                        inProgressCommandByUuid[speechLineRequest.lineUuid] = SpeechLine(speechLineRequest)
                    else
                        addSpeechLine(speechLineRequest)
                }
                flushSpeechCommands()
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

    fun isSpeechCommand(str: String): Boolean {
        var firstWord = str
            .toLowerCase()
            .replace(".", "")
            .replace(",", "")
            .split(" ")
            .first()

        return firstWord.equals("command", true)
    }

    fun flushSpeechCommands() {
        val entrySet = inProgressCommandByUuid.entries
        for(entry in entrySet) {
            Log.i("Command ForEach", "Entered Command ForEach")
            var speechLine = entry.value
            if(!speechLine.isFinalized)
                continue

            var cmdStr = speechLine.text.value
                .toLowerCase()
                .replace(".", "")
                .replace(",", "")
                .split(" ")
            if(cmdStr.isEmpty())
                continue

            var cmd = cmdStr[1]
            var args: List<String> = mutableListOf()
            if(cmdStr.size > 2)
                args = cmdStr.slice(IntRange(2, cmdStr.size - 1))

            Log.i("Command ForEach", "Executing Command")
            if(speakerByIndex[speechLine.speaker] != null)
                speechCommandByName[cmd]?.execute(this, speakerByIndex[speechLine.speaker]!!, args)

            inProgressCommandByUuid.remove(entry.key)
        }
    }

    @Composable
    fun speechBlocksLazyColumn(input: MutableList<SpeechBlock>) {
        val speechBlocks = remember { input }
        val listState = rememberLazyListState()

        LaunchedEffect(speechBlocks.last().speechLines.last().text.value) {
            if (speechBlocks.isNotEmpty())
                listState.animateScrollToItem(index = speechBlocks.lastIndex + 1)
        }

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
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
                    text = speakerByIndex[speechBlock.speaker]?.name?.value!!,
                    fontWeight = FontWeight.Medium,
                    color = speakerByIndex[speechBlock.speaker]?.color?.value!!,
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
                            .background(color = speakerByIndex[speechBlock.speaker]?.color?.value!!)
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
}