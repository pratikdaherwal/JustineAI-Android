package com.patikprojects.justineai.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.patikprojects.justineai.R
import com.patikprojects.justineai.adapters.ChatAdapter
import com.patikprojects.justineai.models.ChatMessagesModel
import com.patikprojects.justineai.utils.LLMHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TextHomeActivity : AppCompatActivity() {
    companion object{
        lateinit var llmHelper: LLMHelper
    }

    private lateinit var chatAdapter: ChatAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var inputEditText: EditText
    private lateinit var sendButton: Button
    private var finalResponse: String = ""
    private var isGenerating = false

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_text_home)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        recyclerView = findViewById(R.id.recyclerViewChat)
        chatAdapter = ChatAdapter(mutableListOf())
        recyclerView.adapter = chatAdapter
        recyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }

        inputEditText = findViewById(R.id.promptInput)
        sendButton = findViewById(R.id.sendButton)

        sendButton.setOnClickListener {
            if (isGenerating) {
                llmHelper.cancelGeneration()
                sendButton.text = "Send"
                sendButton.isEnabled = true
                isGenerating = false
            } else {
                val prompt = inputEditText.text.toString().trim()
                if (prompt.isNotEmpty()) {
                    sendMessage(prompt)
                    inputEditText.text.clear()
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun sendMessage(prompt: String) {
        finalResponse = ""
        chatAdapter.addMessage(ChatMessagesModel(prompt, true))
        recyclerView.smoothScrollToPosition(chatAdapter.itemCount - 1)

        chatAdapter.addMessage(ChatMessagesModel("▋", false))
        recyclerView.smoothScrollToPosition(chatAdapter.itemCount - 1)

        sendButton.text = "Cancel"
        isGenerating = true

        llmHelper.onTokenReceived = { partialResponse, done ->
            runOnUiThread {
                if (isGenerating) {
                    finalResponse += partialResponse
                    if (done) {
                        chatAdapter.updateLastMessage(finalResponse)
                        sendButton.text = "Send"
                        sendButton.isEnabled = true
                        isGenerating = false
                    } else {
                        chatAdapter.updateLastMessage("${finalResponse}▋")
                    }
                    recyclerView.post {
                        recyclerView.scrollToPosition(chatAdapter.itemCount - 1)
                    }

                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val future = llmHelper.generateResponse(prompt)
                future.addListener({}, ContextCompat.getMainExecutor(this@TextHomeActivity))
            } catch (e: Exception) {
                runOnUiThread {
                    chatAdapter.updateLastMessage("Error: ${e.message}")
                    sendButton.text = "Send"
                    sendButton.isEnabled = true
                    isGenerating = false
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        llmHelper.close()
    }
}