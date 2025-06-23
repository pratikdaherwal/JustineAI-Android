package com.patikprojects.justineai.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.patikprojects.justineai.R
import com.patikprojects.justineai.utils.LLMHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("CustomSplashScreen")
class SplashScreen : AppCompatActivity() {
    private lateinit var splashText: TextView
    private val textList = listOf(
        "Justine AI",
        "Your Personal AI Assistant",
        "Thinking with You, Not for You",
        "Offline, On Your Side",
        "Smarter Conversations Start Here",
        "Always Here. Always Yours."
    )
    private var index = 0
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash_screen)

        splashText = findViewById(R.id.splashText)

        startTextAnimation()
        proceedToNextScreen()
    }

    private fun startTextAnimation() {
        val slideOut = AnimationUtils.loadAnimation(this, R.anim.slide_out_up)
        val slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_bottom)

        runnable = object : Runnable {
            override fun run() {
                slideOut.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {}
                    override fun onAnimationEnd(animation: Animation?) {
                        splashText.text = textList[index]
                        splashText.startAnimation(slideIn)
                        index = (index + 1) % textList.size
                    }

                    override fun onAnimationRepeat(animation: Animation?) {}
                })

                splashText.startAnimation(slideOut)
                handler.postDelayed(this, 2000)
            }
        }

        splashText.text = textList[index]
        index = (index + 1) % textList.size
        handler.postDelayed(runnable, 2000)
    }


    private fun proceedToNextScreen() {
        lifecycleScope.launch(Dispatchers.IO) {
            TextHomeActivity.Companion.llmHelper = LLMHelper(applicationContext)
            withContext(Dispatchers.Main) {
                handler.removeCallbacks(runnable)
                startActivity(Intent(this@SplashScreen, SpeechHomeActivity::class.java))
                finish()
            }
        }
    }
}