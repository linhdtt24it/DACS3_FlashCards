package com.example.flashcards.utils

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiService {
    // Lưu ý: Trong thực tế, bạn nên để API Key trong local.properties hoặc dùng giải pháp bảo mật hơn.
    private val apiKey = "YOUR_GEMINI_API_KEY" 
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.0-flash",  // Nhanh, mạnh, free
        apiKey = apiKey
    )

    suspend fun generateExamples(term: String, definition: String, languageCode: String): String? = withContext(Dispatchers.IO) {
        val prompt = """
            Give me 3 short, practical example sentences for the vocabulary word: "$term" (meaning: $definition).
            The examples should be in the language corresponding to code: $languageCode.
            Format the output as a simple list of 3 sentences, separated by newlines. 
            Do not include any extra text or numbering.
        """.trimIndent()

        try {
            val response = generativeModel.generateContent(prompt)
            response.text
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
