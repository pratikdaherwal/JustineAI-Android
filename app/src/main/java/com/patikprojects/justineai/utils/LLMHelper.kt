package com.patikprojects.justineai.utils

import android.content.Context
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession.LlmInferenceSessionOptions
import com.google.mediapipe.tasks.genai.llminference.ProgressListener
import java.util.concurrent.Executors
import java.util.concurrent.Future

class LLMHelper(context: Context) {
    private val llmInference: LlmInference
    private var llmInferenceSession: LlmInferenceSession
    private val executor = Executors.newSingleThreadExecutor()
    private val chatHistory = StringBuilder()
    private var currentFuture: Future<*>? = null
    var onTokenReceived: ((String, Boolean) -> Unit)? = null

    companion object {
        private const val MAX_TOKENS = 1024
        private const val MODEL_PATH = "/data/local/tmp/llm/gemma3-1b-it-int4.task"
    }

    init {
        val inferenceOptions = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(MODEL_PATH)
            .setMaxTokens(MAX_TOKENS)
            .build()
        llmInference = LlmInference.createFromOptions(context, inferenceOptions)

        val sessionOptions = LlmInferenceSessionOptions.builder()
            .setTopK(40)
            .setTopP(0.9f)
            .setTemperature(0.7f)
            .build()
        llmInferenceSession = LlmInferenceSession.createFromOptions(llmInference, sessionOptions)
    }

    fun generateResponse(userPrompt: String): ListenableFuture<String> {
        return try {
            val inputWithHistory = buildPromptWithHistory(userPrompt)
            val tokensRemaining = estimateTokensRemaining(inputWithHistory)
            println("remaining tokens: $tokensRemaining")
            if (tokensRemaining < 0) {
                return SettableFuture.create<String>().apply {
                    setException(Exception("Input exceeds context window."))
                }
            }

            llmInferenceSession.addQueryChunk(inputWithHistory)
            val future = llmInferenceSession.generateResponseAsync(
                ProgressListener { partialResponse, done ->
                    executor.execute {
                        onTokenReceived?.invoke(partialResponse, done)
                        if (done) {
                            appendToHistory(userPrompt, partialResponse)
                        }
                    }
                }
            )
            currentFuture = future
            future
        } catch (e: Exception) {
            executor.execute {
                onTokenReceived?.invoke("Error: ${e.message}", true)
            }
            return SettableFuture.create<String>().apply {
                setException(e)
            }
        }
    }

    fun cancelGeneration() {
        currentFuture?.cancel(true)
        currentFuture = null
    }

    private fun buildPromptWithHistory(userPrompt: String): String {
        return chatHistory.toString() + "\nUser: $userPrompt\nAI:"
    }

    private fun appendToHistory(userPrompt: String, aiResponse: String) {
        chatHistory.append("\nUser: $userPrompt\nAI: $aiResponse")
    }

    fun estimateTokensRemaining(context: String): Int {
        return try {
            val contextSize = llmInferenceSession.sizeInTokens(context)
            MAX_TOKENS - contextSize - 256
        } catch (e: Exception) {
            -1
        }
    }

    fun clearHistory() {
        chatHistory.clear()
    }

    fun resetSession() {
        llmInferenceSession.close()
        val sessionOptions = LlmInferenceSessionOptions.builder()
            .setTopK(40)
            .setTopP(0.9f)
            .setTemperature(0.7f)
            .build()
        llmInferenceSession = LlmInferenceSession.createFromOptions(llmInference, sessionOptions)
    }

    fun close() {
        llmInferenceSession.close()
        llmInference.close()
        executor.shutdown()
    }
}
