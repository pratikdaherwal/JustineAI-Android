package com.patikprojects.justineai.assist

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.patikprojects.justineai.R

class JustineAssistantBottomSheet : BottomSheetDialogFragment() {
    private var speechRecognizer: SpeechRecognizer? = null
    private var speechIntent: Intent? = null
    private lateinit var statusText: TextView
    private lateinit var resultText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_justine_assistant, container, false)
        statusText = view.findViewById(R.id.status_text)
        resultText = view.findViewById(R.id.result_text)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSpeechRecognition()
        startListening()
        view.setOnClickListener { dismiss() } // Tap anywhere to dismiss
    }

    private fun setupSpeechRecognition() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            dismiss()
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
        speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                statusText.text = "Listening..."
            }

            override fun onBeginningOfSpeech() {
                statusText.text = "Speak now..."
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                statusText.text = "Processing..."
            }

            override fun onError(error: Int) {
                statusText.text = "Error: $error"
                Handler(Looper.getMainLooper()).postDelayed({ dismiss() }, 2000)
            }

            override fun onResults(results: Bundle?) {
                val result = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                resultText.text = result ?: ""
                statusText.text = "You said:"
                Handler(Looper.getMainLooper()).postDelayed({ dismiss() }, 3000)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val result = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                resultText.text = result ?: ""
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun startListening() {
        speechRecognizer?.startListening(speechIntent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        speechRecognizer?.destroy()
    }
}