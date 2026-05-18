package com.example.flashcards.utils

import com.example.flashcards.model.Flashcard
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class GeminiService {
    // Lưu ý: Trong thực tế, bạn nên để API Key trong local.properties hoặc dùng giải pháp bảo mật hơn.
    private val apiKey = "AIzaSyB9WYtLhFFXng5WLvLoFoV7cGh4XfrdLI8"  // Hardcode tạm
    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
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

    suspend fun generateFlashcardsFromText(inputText: String): List<Flashcard> = withContext(Dispatchers.IO) {
        val prompt = """
            Extract key concepts from the following text and create a list of flashcards.
            Each flashcard should have a "question" (or term) and an "answer" (or definition).
            Format the output strictly as:
            Question 1 | Answer 1
            Question 2 | Answer 2
            
            Text:
            $inputText
        """.trimIndent()

        try {
            val response = generativeModel.generateContent(prompt)
            val text = response.text ?: return@withContext emptyList<Flashcard>()

            text.lines()
                .filter { it.isNotBlank() && it.contains("|") }
                .mapNotNull { line ->
                    val parts = line.split("|", limit = 2)
                    if (parts.size == 2) {
                        Flashcard(
                            id = UUID.randomUUID().toString(),
                            question = parts[0].trim(),
                            answer = parts[1].trim()
                        )
                    } else null
                }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
