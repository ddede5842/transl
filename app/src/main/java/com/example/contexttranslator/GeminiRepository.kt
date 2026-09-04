package com.example.contexttranslator

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiRepository(private val apiKey: String) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun translateWithContext(targetText: String, surroundingContext: String): String =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) return@withContext "API 키가 등록되지 않았습니다. 앱 설정에서 입력해주세요."

            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

            val systemPrompt = """
                너는 고급 문맥 번역기야.
                주어진 전체 문맥을 파악해서 사용자가 선택한 단어 또는 문장의 최적화된 한국어 번역과 문맥상 뉘앙스를 간결하게 알려줘.
                형식:
                [번역] 대상 문맥에 맞는 자연스러운 한국어 번역
                [해설] 왜 이 번역이 적합한지 문맥 기반 설명 (1-2줄)
            """.trimIndent()

            val userContent = "주변 문맥:\n$surroundingContext\n\n선택한 텍스트:\n$targetText"

            val bodyJson = JSONObject().apply {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
                })
                put("contents", JSONArray().put(
                    JSONObject().put("parts", JSONArray().put(
                        JSONObject().put("text", userContent)
                    ))
                ))
            }

            val request = Request.Builder()
                .url(endpoint)
                .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val bodyString = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        return@withContext "통신 오류 (${response.code})"
                    }
                    val json = JSONObject(bodyString)
                    return@withContext json.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                }
            } catch (e: Exception) {
                return@withContext "번역 호출 실패: ${e.localizedMessage}"
            }
        }
}

