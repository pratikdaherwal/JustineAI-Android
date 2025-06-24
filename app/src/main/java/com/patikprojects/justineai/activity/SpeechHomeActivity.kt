package com.patikprojects.justineai.activity

import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.Preference
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.patikprojects.justineai.R
import com.patikprojects.justineai.utils.WakeWordListener
import com.patikprojects.justineai.service.WakeWordService
import com.patikprojects.justineai.utils.AssistantUtils
import com.patikprojects.justineai.utils.MyGLSurfaceView
import com.patikprojects.justineai.utils.PermissionManager
import com.patikprojects.justineai.utils.SphereRenderer

class SpeechHomeActivity : AppCompatActivity() {
    private lateinit var glView: MyGLSurfaceView
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var speechIntent: Intent
    private lateinit var sphereRenderer: SphereRenderer

    companion object {
        private const val TAG = "SpeechHomeActivity"
    }

    private val recognitionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "START_RECOGNITION" -> {
                    Log.i(TAG, "Wake word triggered - starting speech recognition")
                    sphereRenderer.animate = true
                    WakeWordListener.stop()
                    speechRecognizer.startListening(speechIntent)
                }
                "VOICE_COMMAND_RECEIVED" -> {
                    val command = intent.getStringExtra("command")
                    if (command != null) {
                        findViewById<TextView>(R.id.command_output).text = command
                        Log.i(TAG, "Received voice command from overlay: $command")
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_speech_home)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeViews()
        checkPermissionsAndInitialize()

        findViewById<TextView>(R.id.change_assistant).setOnClickListener {
            PermissionManager.promptToSetDefaultAssistant(this)
        }

        findViewById<TextView>(R.id.change_assistant).setOnClickListener {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null && !roleManager.isRoleHeld(RoleManager.ROLE_ASSISTANT)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT)
                startActivityForResult(intent, 1234)
            } else {
                val intent = Intent(android.provider.Settings.ACTION_VOICE_INPUT_SETTINGS)
                startActivity(intent)
            }
        }

        Log.i(TAG, "SpeechHomeActivity created")
    }

    private fun checkPermissionsAndInitialize() {
        PermissionManager.checkAndRequestPermissions(this) {
            setupSpeechRecognition()
            ensureWakeWordServiceRunning()
            registerReceivers()
//            checkAndPromptDefaultAssistant()
        }
    }

    private fun ensureWakeWordServiceRunning() {
        // The service should already be running from App class, but ensure it's started
        val serviceIntent = Intent(this, WakeWordService::class.java)
        startForegroundService(serviceIntent)
        Log.i(TAG, "Wake word service ensured running")
    }

    private fun initializeViews() {
        sphereRenderer = SphereRenderer()
        glView = findViewById(R.id.gl_surface_view)
        glView.setEGLContextClientVersion(1)
        glView.setRenderer(sphereRenderer)

        val backgroundImage: ImageView = findViewById(R.id.background_image)
        backgroundImage.setRenderEffect(
            RenderEffect.createBlurEffect(100f, 100f, Shader.TileMode.MIRROR)
        )

        glView.setOnClickListener {
            toggleSpeechRecognition()
        }

        findViewById<TextView>(R.id.use_keyboard).setOnClickListener {
            startActivity(Intent(this, TextHomeActivity::class.java))
        }
    }

    private fun setupSpeechRecognition() {
        if (!PermissionManager.hasAudioPermission(this)) {
            Log.e(TAG, "Audio permission not available for speech recognition")
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognizedText = matches?.firstOrNull() ?: return
                findViewById<TextView>(R.id.command_output).text = recognizedText
                sphereRenderer.animate = false

                Log.i(TAG, "Speech recognition result: $recognizedText")

                // Restart wake word detection after speech recognition completes
                Handler(Looper.getMainLooper()).postDelayed({
                    if (!WakeWordListener.isRunning()) {
                        WakeWordListener.start(applicationContext)
                    }
                }, 500)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partialText = matches?.firstOrNull()
                if (partialText != null) {
                    findViewById<TextView>(R.id.command_output).text = partialText
                }
            }

            override fun onError(error: Int) {
                Log.e(TAG, "Speech recognition error: $error")
                sphereRenderer.animate = false

                Handler(Looper.getMainLooper()).postDelayed({
                    if (!WakeWordListener.isRunning()) {
                        WakeWordListener.start(applicationContext)
                    }
                }, 500)
            }

            override fun onReadyForSpeech(params: Bundle?) {
                Log.i(TAG, "Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                Log.i(TAG, "Speech started")
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                Log.i(TAG, "Speech ended")
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun toggleSpeechRecognition() {
        if (sphereRenderer.animate) {
            sphereRenderer.animate = false
            speechRecognizer.stopListening()

            if (!WakeWordListener.isRunning()) {
                WakeWordListener.start(applicationContext)
            }
        } else {
            sphereRenderer.animate = true
            WakeWordListener.stop()
            speechRecognizer.startListening(speechIntent)
        }
    }

    private fun startWakeWordService() {
        val serviceIntent = Intent(this, WakeWordService::class.java)
        startForegroundService(serviceIntent)
        Log.i(TAG, "Wake word service started")
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerReceivers() {
        val filter = IntentFilter().apply {
            addAction("START_RECOGNITION")
            addAction("VOICE_COMMAND_RECEIVED")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(recognitionReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(recognitionReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        glView.onPause()
        Log.i(TAG, "Activity paused")
    }

    override fun onResume() {
        super.onResume()
        glView.onResume()
        updateAssistantStatus()
        if (!sphereRenderer.animate && !WakeWordListener.isRunning()) {
            WakeWordListener.start(applicationContext)
        }
        Log.i(TAG, "Activity resumed")
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
        unregisterReceiver(recognitionReceiver)
        Log.i(TAG, "Activity destroyed")
    }

    private fun isJustineDefaultAssistant(): Boolean {
        val context = this
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager?.isRoleHeld(RoleManager.ROLE_ASSISTANT) == true &&
                    getDefaultAssistantPackage(context) == context.packageName
        } else {
            getDefaultAssistantPackage(context) == context.packageName
        }
    }

    private fun getDefaultAssistantPackage(context: Context): String? {
        val intent = Intent(Intent.ACTION_ASSIST)
        val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName
    }

    private fun updateAssistantStatus() {
        val assistantStatusView = findViewById<TextView>(R.id.assistant_status)
        val isDefault = isJustineDefaultAssistant()
        assistantStatusView.text = if (isDefault) {
            "JustineAI is your default assistant"
        } else {
            "None"
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupSpeechRecognition()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1234) {
            updateAssistantStatus()
        }
    }
}