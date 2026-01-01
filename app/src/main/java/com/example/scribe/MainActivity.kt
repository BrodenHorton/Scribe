package com.example.scribe

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {
    private val TAG : String = "MainActivity"
    private lateinit var tvSpeakerCount : TextView
    private lateinit var tvSpeechBlock1 : TextView
    private lateinit var tvSpeechBlock2 : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvSpeakerCount = findViewById(R.id.speakerCount)
        tvSpeechBlock1 = findViewById(R.id.tvSpeechBlock1)
        tvSpeechBlock1.text = "Speech Block 1"
        tvSpeechBlock2 = findViewById(R.id.tvSpeechBlock2)
        tvSpeechBlock2.text = "Speech Block 2"

        GlobalScope.launch {
            getInitialSpeechBlocks()
        }
    }

    suspend fun getInitialSpeechBlocks() {
        var isActive = true
        while(isActive) {
            val url = URL("http://10.0.2.2:3000/all")
            val connection = url.openConnection() as HttpURLConnection

            if(connection.responseCode == 200) {
                Log.i("Coroutine", "Fetching from API")
                val inputStream = connection.getInputStream()
                val inputStreamReader = InputStreamReader(inputStream, "UTF-8")
                val scribeRequest = Gson().fromJson(inputStreamReader, ScribeRequest::class.java)
                inputStreamReader.close()
                inputStream.close()
                tvSpeakerCount.text = "Total Speech Blocks: ${scribeRequest.speechBlocks.size}"
                var count = 0
                for(speechBlock in scribeRequest.speechBlocks) {
                    if(count % 2 == 0)
                        tvSpeechBlock1.text = speechBlock.text
                    else
                        tvSpeechBlock2.text = speechBlock.text
                    count++
                }
            }
            else {
                tvSpeechBlock1.text = "Error fetching data from Scribe Server!"
                tvSpeechBlock2.text = ""
                Log.i("Coroutine", "Error in request to Scribe Server")
                isActive = false
            }

            if(isActive) {
                delay(300)
            }
        }
    }
}