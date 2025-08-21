package com.beratyesbek.dslteamprbuddy.service

import com.google.genai.Client
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service


@Service
class AiMessageService(
    @Value("\${gemini.api.key}")
    private val geminiApiKey: String
) {
    fun generateMessage(reviewer: String, devList: List<String>, team: String, mainLang: String): String? {
        val client = Client.builder().apiKey(geminiApiKey).build()
        val prompt = "Write a friendly and motivating message for $reviewer who will review PRs from ${devList.joinToString(", ")} this week. Team name is: ${team}, currentDateTime: ${java.time.LocalDateTime.now()}. so you can say good morning or good afternoon based on time. Main language of Reviewer: ${mainLang}. So please salute or say good morning or good afternoon in their language and add one joke in their language at the end of mail and it shouldn't be bigger than 2 sentence and rest of email must be in English" +
                "The message should be concise, encouraging, and suitable for a professional setting. Say somethings good as well for developers" +
                "Make sure to include a positive note about the team's efforts and the importance of their reviews and it should be funny. please keep it email format and Don't add subject or any other extra information, just the body of the email. and please but please add team name at the end like professinal email"

        val response =
            client.models.generateContent(
                "gemini-2.5-flash",
                prompt,
                null
            )
        return response.text()
    }
}