package com.example.subscription.data.remote

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiService(
    private val apiKey: String
) {
    private val model by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = apiKey
        )
    }

    suspend fun analyzeImage(bitmap: Bitmap, prompt: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val input = content {
                image(bitmap)
                text(prompt)
            }
            val response = model.generateContent(input)
            response.text ?: "Нет ответа от модели"
        }
    }
}
