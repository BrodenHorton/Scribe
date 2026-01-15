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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

class MainActivity : ComponentActivity() {
    var speechBlocks: MutableList<SpeechBlock> = mutableStateListOf()
    var lastSpeechBlockBySpeaker: MutableMap<Int, SpeechBlock> = mutableMapOf()
    var speechPrompts: MutableList<TextLine> = mutableStateListOf()
    lateinit var lastRequested: RequestDate
    var speakerByIndex: MutableMap<Int, Speaker> = mutableMapOf()
    var inProgressCommandByUuid: ConcurrentHashMap<String, SpeechLine> = ConcurrentHashMap()
    var speechCommandByName: MutableMap<String, SpeechCommand> = mutableMapOf()
    var isConnectionPanelHidden: MutableState<Boolean> = mutableStateOf(true)
    var apiAddressField: MutableState<String> = mutableStateOf("10.0.2.2:3000")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Column(modifier = Modifier.fillMaxSize()) {
                ActionBarComponent()
                SpeechBlocksLazyColumn()
            }
            if(!isConnectionPanelHidden.value) {
                ConnectionPanelComponent()
            }
        }

        registerCommands()
        speechPrompts.add(TextLine("Start of messages"))

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
        var connection: HttpURLConnection
        var hasConnected = false
        Log.i("API connection", "Connecting to API ...")
        while(!hasConnected) {
            try {
                connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 1000
                connection.readTimeout = 1000
                if(connection.responseCode == 200) {
                    Log.i("API connection", "Successfully connected to API")
                    hasConnected = true
                    val inputStream = connection.getInputStream()
                    val inputStreamReader = InputStreamReader(inputStream, "UTF-8")
                    val scribeRequest = Gson().fromJson(inputStreamReader, ScribeRequest::class.java)
                    inputStreamReader.close()
                    inputStream.close()
                    lastRequested = scribeRequest.date
                    var speechLinesCopy = scribeRequest.speechLines.map { it.copy() }.toMutableList()
                    while(!speechLinesCopy.isEmpty()) {
                        var nextSpeechLine = speechLinesCopy[0]
                        for(speechLineRequest in speechLinesCopy) {
                            if(speechLineRequest.created < nextSpeechLine.created)
                                nextSpeechLine = speechLineRequest
                        }

                        if(!speakerByIndex.contains(nextSpeechLine.speaker))
                            speakerByIndex[nextSpeechLine.speaker] = Speaker("Speaker ${nextSpeechLine.speaker}", Color.LightGray)

                        if(isSpeechCommand(nextSpeechLine.text)) {
                            inProgressCommandByUuid[nextSpeechLine.lineUuid] = SpeechLine(nextSpeechLine)
                            if(nextSpeechLine.isFinalized)
                                flushSpeechCommands()
                        }
                        else
                            addSpeechLine(nextSpeechLine)

                        speechLinesCopy.remove(nextSpeechLine)
                    }
                }
                else {
                    Log.i("API connection", "Error when sending GET request to endpoint /all from Scribe Server")
                }
            }
            catch(e: SocketTimeoutException) {
                Log.i("API connection", "Unable to connect, attempting reconnect")
            }
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

        if(!speechBlocks.isEmpty() &&
            (speechPrompts.isEmpty() || speechBlocks.last().created > speechPrompts.last().created)
            && speechBlocks.last().speaker == speechLineRequest.speaker) {
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
            var speechLine = entry.value
            if(!speechLine.isFinalized)
                continue
            Log.i("Command ForEach", "Entered Command ForEach")
            var cmdStr = speechLine.text.value
                .toLowerCase()
                .replace(".", "")
                .replace(",", "")
                .replace("?", "")
                .replace("!", "")
                .split(" ")
            if(cmdStr.size < 2) {
                inProgressCommandByUuid.remove(entry.key)
                continue
            }
            var cmd = cmdStr[1]
            if(!speechCommandByName.contains(cmd)) {
                speechPrompts.add(TextLine("Invalid command: $cmd"))
                inProgressCommandByUuid.remove(entry.key)
                continue
            }
            if(speakerByIndex[speechLine.speaker] == null) {
                speechPrompts.add(TextLine("Invalid speaker"))
                inProgressCommandByUuid.remove(entry.key)
                continue
            }

            var args: List<String> = mutableListOf()
            if(cmdStr.size > 2)
                args = cmdStr.slice(IntRange(2, cmdStr.size - 1))

            val result = speechCommandByName[cmd]?.execute(this, speakerByIndex[speechLine.speaker]!!, args)
            speechPrompts.add(TextLine(result!!))

            inProgressCommandByUuid.remove(entry.key)
        }
    }

    @Composable
    fun ColumnScope.SpeechBlocksLazyColumn() {
        val listState = rememberLazyListState()

        LaunchedEffect(speechBlocks.lastOrNull()?.speechLines?.lastOrNull()?.text?.value, speechPrompts.lastOrNull()) {
            if (speechBlocks.isNotEmpty())
                listState.animateScrollToItem(index = speechBlocks.size + speechPrompts.size)
        }

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 2.dp, bottom = 45.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .background(Color.White)
                .padding(30.dp, 0.dp, 30.dp, 0.dp)
        ) {
            var speechBlockIndex = 0
            var speechCommandTextLineIndex = 0
            while(speechBlockIndex < speechBlocks.size
                && speechCommandTextLineIndex < speechPrompts.size) {
                if(speechBlocks[speechBlockIndex].created < speechPrompts[speechCommandTextLineIndex].created) {
                    val staticIndex = speechBlockIndex
                    item {
                        SpeechBlockComponent(speechBlocks[staticIndex])
                    }
                    speechBlockIndex++
                }
                else {
                    val staticIndex = speechCommandTextLineIndex
                    item {
                        SpeechPromptComponent(speechPrompts[staticIndex])
                    }
                    speechCommandTextLineIndex++
                }
            }

            while(speechBlockIndex < speechBlocks.size) {
                val staticIndex = speechBlockIndex
                item {
                    SpeechBlockComponent(speechBlocks[staticIndex])
                }
                speechBlockIndex++
            }
            while(speechCommandTextLineIndex < speechPrompts.size) {
                val staticIndex = speechCommandTextLineIndex
                item {
                    SpeechPromptComponent(speechPrompts[staticIndex])
                }
                speechCommandTextLineIndex++
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

    @Composable
    fun SpeechPromptComponent(speechCommandTextLine: TextLine) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()) {
                Text(
                    text = speechCommandTextLine.text.value,
                    fontWeight = FontWeight.Medium,
                    color = Color.LightGray,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    @Composable
    fun ColumnScope.ActionBarComponent() {
        Box(
            contentAlignment = Alignment.BottomEnd,
            modifier = Modifier
                .weight(0.1f)
                .fillMaxSize()
                .height(IntrinsicSize.Max)
                .background(Color.White)
                .drawBehind {
                    val strokeWidth = 1f * density
                    val y = size.height - strokeWidth / 2
                    drawLine(
                        Color.LightGray,
                        Offset(0f, y),
                        Offset(size.width, y),
                        strokeWidth
                    )
                }
        ) {
            Button(
                modifier = Modifier.padding(10.dp, 0.dp, 10.dp, 10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF03a9fc)
                ),
                onClick = {
                    isConnectionPanelHidden.value = false
                }
            ) {
                Text(
                    text = "Connect",
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    @Composable
    fun ConnectionPanelComponent() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x44888888))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .padding(40.dp, 0.dp)
                    .align(Alignment.Center)
                    .background(Color.White)
                    .drawBehind {
                        val strokeWidth = 1f * density
                        val y = size.height - strokeWidth / 2
                        drawLine(
                            Color.LightGray,
                            Offset(0f, y),
                            Offset(size.width, y),
                            strokeWidth
                        )
                    }
            ) {
                Button(
                    modifier = Modifier
                        .padding(5.dp)
                        .align(Alignment.TopEnd),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White
                    ),
                    onClick = {
                        isConnectionPanelHidden.value = true
                    }
                ) {
                    Text(
                        text = "X",
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF888888),
                        fontSize = 26.sp,
                        textAlign = TextAlign.Center
                    )
                }
                OutlinedTextField(
                    value = apiAddressField.value,
                    onValueChange = { newText ->
                        // Update the state with the new value whenever the user types
                        apiAddressField.value = newText
                    },
                    label = { Text(text = "API Address", fontSize = 18.sp) },
                    placeholder = { Text(text = "Enter IP Address and port", fontSize = 18.sp) },
                    textStyle = LocalTextStyle.current.copy(fontSize = 18.sp),
                    modifier = Modifier
                        .padding(18.dp, 60.dp, 20.dp, 0.dp)
                        .height(60.dp)
                )
                Button(
                    modifier = Modifier
                        .padding(bottom = 20.dp)
                        .align(Alignment.BottomCenter),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF03a9fc)
                    ),
                    onClick = {
                        isConnectionPanelHidden.value = true
                    }
                ) {
                    Text(
                        text = "Connect",
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}