package com.patikprojects.justineai.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.patikprojects.justineai.R

class AssistantOverlayActivity : AppCompatActivity() {
    private var speechRecognizer: SpeechRecognizer? = null
    private var speechIntent: Intent? = null
    private var statusText: TextView? = null
    private var resultText: TextView? = null
    private var overlayContainer: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        setContentView(R.layout.activity_assistant_overlay)

        initViews()
        setupSpeechRecognition()


        startListening()

        Log.i(TAG, "AssistantOverlayActivity created and listening started")
    }

    private fun initViews() {
        statusText = findViewById<TextView?>(R.id.status_text)
        resultText = findViewById<TextView?>(R.id.result_text)
        overlayContainer = findViewById<View?>(R.id.overlay_container)


        overlayContainer!!.setOnClickListener(View.OnClickListener { v: View? -> finish() })


        overlayContainer!!.isClickable = true
        overlayContainer!!.setFocusable(true)
    }

    private fun setupSpeechRecognition() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Audio permission not granted")
            finish()
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechIntent!!.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        speechIntent!!.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
        speechIntent!!.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        speechIntent!!.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)

        speechRecognizer!!.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                statusText!!.setText("Listening...")
                Log.i(TAG, "Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                statusText!!.setText("Speak now...")
                Log.i(TAG, "Speech started")
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Could use this for visual feedback
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                statusText!!.setText("Processing...")
                Log.i(TAG, "Speech ended")
            }

            override fun onError(error: Int) {
                val errorMsg = getErrorText(error)
                statusText!!.setText("Error: " + errorMsg)
                Log.e(TAG, "Speech recognition error: " + errorMsg)


                // Auto-close after error
                Handler(Looper.getMainLooper()).postDelayed(Runnable { finish() }, 2000)
            }

            override fun onResults(results: Bundle?) {
                if (results != null) {
                    val matches = results.getStringArray(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (matches != null && matches.size > 0) {
                        val recognizedText = matches[0]
                        resultText!!.setText(recognizedText)
                        statusText!!.setText("You said:")

                        Log.i(TAG, "Recognition result: " + recognizedText)


                        // Process the command or send to main app
                        processCommand(recognizedText)


                        // Auto-close after showing result
                        Handler(Looper.getMainLooper()).postDelayed(Runnable { finish() }, 3000)
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                if (partialResults != null) {
                    val matches =
                        partialResults.getStringArray(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (matches != null && matches.size > 0) {
                        resultText!!.setText(matches[0])
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun startListening() {
        if (speechRecognizer != null) {
            speechRecognizer!!.startListening(speechIntent)
        }
    }

    private fun processCommand(command: String?) {
        // Here you can process the voice command
        // For now, we'll just log it and potentially send it to the main app
        Log.i(TAG, "Processing command: " + command)


        // Example: Send command to main app via broadcast
        val commandIntent = Intent("VOICE_COMMAND_RECEIVED")
        commandIntent.putExtra("command", command)
        sendBroadcast(commandIntent)
    }

    private fun getErrorText(errorCode: Int): String {
        when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> return "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> return "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> return "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> return "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> return "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> return "No match found"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> return "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> return "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> return "No speech input"
            else -> return "Unknown error"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (speechRecognizer != null) {
            speechRecognizer!!.destroy()
        }
        Log.i(TAG, "AssistantOverlayActivity destroyed")
    }

    public override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    companion object {
        private const val TAG = "AssistantOverlay"
    }
}