package com.patikprojects.justineai.assist

import android.os.Bundle
import android.service.voice.VoiceInteractionService

class JustineVoiceService : VoiceInteractionService() {
    override fun onReady() {
        super.onReady()
        showSession(Bundle.EMPTY, 0)
    }
}

