package com.patikprojects.justineai.assist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.util.Log
import com.patikprojects.justineai.activity.SpeechHomeActivity

class JustineSession(context: Context) : VoiceInteractionSession(context) {
    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        Log.i("JustineSession", "Assistant invoked — launching bottom sheet")

        val intent = Intent(context, SpeechHomeActivity::class.java).apply { //JustineAssistantActivity
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
