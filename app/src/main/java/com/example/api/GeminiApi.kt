package com.example.api

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null,
    val generationConfig: GenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = 0.7f,
    val maxOutputTokens: Int? = 200
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiApiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val service: GeminiApiService by lazy {
        retrofit.create(GeminiApiService::class.java)
    }

    suspend fun generateAIResponse(contactName: String, contactAbout: String, conversationHistory: List<Pair<String, String>>): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return getLocalSimulatedResponse(contactName, conversationHistory.lastOrNull()?.second ?: "")
        }

        val historyParts = conversationHistory.map { (sender, text) ->
            "$sender: $text"
        }.joinToString("\n")

        val prompt = "Here is the conversation history:\n$historyParts\n\nPlease generate a natural, short, WhatsApp-style response as $contactName. Do not prefix with your name."

        val systemInstructionText = if (contactName == "Aero Strange AI") {
            "You are Aero Strange AI, the ultimate advanced cognitive assistant developed by Aero Strange International Platforms. " +
            "Be extremely helpful, witty, smart, and enthusiastic about security, speed, and real-time VoIP. Help users with any questions, " +
            "keep responses informative, and use formatting and emojis."
        } else {
            "You are $contactName, a user of Aero Strange Chats ($contactAbout). " +
            "Reply in a concise, human-like, realistic chat tone. Use emojis occasionally. Keep responses under 2 sentences."
        }

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            ),
            systemInstruction = Content(
                parts = listOf(
                    Part(text = systemInstructionText)
                )
            ),
            generationConfig = GenerationConfig(temperature = 0.8f)
        )

        return try {
            val response = service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                ?: getLocalSimulatedResponse(contactName, conversationHistory.lastOrNull()?.second ?: "")
        } catch (e: Exception) {
            e.printStackTrace()
            getLocalSimulatedResponse(contactName, conversationHistory.lastOrNull()?.second ?: "")
        }
    }

    private fun getLocalSimulatedResponse(name: String, lastMsg: String): String {
        val cleanMsg = lastMsg.lowercase()
        if (name == "Aero Strange AI") {
            return when {
                cleanMsg.contains("hello") || cleanMsg.contains("hi") || cleanMsg.contains("hey") -> {
                    "Greetings from Aero Strange AI! 🤖 How can I assist you on this ultra-secure chat terminal today?"
                }
                cleanMsg.contains("help") || cleanMsg.contains("feature") -> {
                    "I can guide you through our end-to-end encrypted chats, real-time audio/video VoIP calls, screen sharing, group calling, and settings configuration!"
                }
                cleanMsg.contains("call") || cleanMsg.contains("voip") || cleanMsg.contains("screen") -> {
                    "We support voice, video, group VoIP calling, and real-time screen sharing! Click the call/video buttons on top to try it out!"
                }
                cleanMsg.contains("firebase") || cleanMsg.contains("database") -> {
                    "We are integrated with Firebase Auth and Firestore for real-time messaging sync! Your connection is fully active."
                }
                else -> {
                    listOf(
                        "As Aero Strange AI, I am monitoring the security perimeter. All communications are stable and encrypted. 🔒",
                        "How can I assist you today? Ask me about encryption, VoIP calls, or groups! 🚀",
                        "Aero Strange AI is fully synchronized with Gemini cognitive modules to serve you better.",
                        "That is amazing! Aero Strange international platforms are operating at 100% capacity."
                    ).random()
                }
            }
        }

        return when {
            cleanMsg.contains("hello") || cleanMsg.contains("hi") || cleanMsg.contains("hey") -> {
                val greetings = listOf(
                    "Hey there! Aero Strange Chats is working beautifully. How are you doing today?",
                    "Hello! Glad to connect with you on Aero Strange Chats.",
                    "Hey! Hope you are having a wonderful day."
                )
                greetings.random()
            }
            cleanMsg.contains("how are you") || cleanMsg.contains("how's it going") -> {
                "I'm doing fantastic, enjoying the high speed of Aero Strange international platforms! How about you?"
            }
            cleanMsg.contains("call") || cleanMsg.contains("dial") -> {
                "Sure, we can do a call! Tap the call or video icon in the top right header to connect instantly via Wi-Fi/Data!"
            }
            cleanMsg.contains("secure") || cleanMsg.contains("security") -> {
                "Aero Strange Chats features end-to-end encryption. All our servers run on ultra-secure international platforms."
            }
            cleanMsg.contains("group") -> {
                "Yes, you can create group chats! Go back to the main list and tap the 'New Group' option."
            }
            cleanMsg.contains("wallpaper") || cleanMsg.contains("theme") -> {
                "You can change the theme and wallpaper in Settings -> Chats. It updates immediately!"
            }
            else -> {
                val genericResponses = listOf(
                    "That sounds interesting! Tell me more.",
                    "I agree with you! Aero Strange Chats is really sleek.",
                    "Got it! Let me know if you need anything else.",
                    "Aero Strange Chats makes communication so simple and fast! 🚀",
                    "Indeed! Operated by Aero Strange International Platforms, of course."
                )
                genericResponses.random()
            }
        }
    }
}
