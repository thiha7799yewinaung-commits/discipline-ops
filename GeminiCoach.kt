package com.disciplinequest.app.ai

import okhttp3.*
import org.json.JSONObject
import java.io.IOException

data class AiTaskDraft(
    val title: String,
    val description: String,
    val deadline: String,
    val difficulty: String,
    val xp: Int,
    val coins: Int,
    val hpPenalty: Int
)

data class AiPlanDraft(val missionTitle: String, val goal: String, val tasks: List<AiTaskDraft>)

class GeminiCoach(private val client: OkHttpClient = OkHttpClient()) {
    companion object {
        const val MODEL = "gemini-3.8-flash"
    }

    fun generate(goal: String, days: Int, callback: (Result<AiPlanDraft>) -> Unit) {
        val key = currentKey ?: return callback(Result.failure(IllegalStateException("NO_API_KEY")))
        val prompt = """
            You are a disciplined productivity coach.
            Create a realistic $days-day mission from this goal: $goal
            Return ONLY valid JSON:
            {
              "missionTitle":"...",
              "goal":"...",
              "tasks":[
                {
                  "title":"...",
                  "description":"...",
                  "deadline":"YYYY-MM-DD",
                  "difficulty":"EASY|MEDIUM|HARD|CRITICAL",
                  "xp":10,
                  "coins":5,
                  "hpPenalty":5
                }
              ]
            }
            Keep tasks practical. Never invent completion results or player statistics.
        """.trimIndent()

        val body = JSONObject()
            .put("contents", org.json.JSONArray()
                .put(JSONObject().put("parts", org.json.JSONArray().put(JSONObject().put("text", prompt)))))
            .put("generationConfig", JSONObject().put("responseMimeType","application/json"))
            .toString()

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=$key")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = callback(Result.failure(e))
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) return callback(Result.failure(IOException("Gemini HTTP ${it.code}")))
                    try {
                        val root = JSONObject(it.body!!.string())
                        val text = root.getJSONArray("candidates").getJSONObject(0)
                            .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
                        val clean = text.trim().removePrefix("```json").removeSuffix("```").trim()
                        val j = JSONObject(clean)
                        val arr = j.getJSONArray("tasks")
                        val list = buildList {
                            for (i in 0 until arr.length()) {
                                val t = arr.getJSONObject(i)
                                add(AiTaskDraft(
                                    t.optString("title","Task"),
                                    t.optString("description",""),
                                    t.optString("deadline",""),
                                    t.optString("difficulty","MEDIUM"),
                                    t.optInt("xp",20),
                                    t.optInt("coins",10),
                                    t.optInt("hpPenalty",10)
                                ))
                            }
                        }
                        callback(Result.success(AiPlanDraft(
                            j.optString("missionTitle","AI Mission"),
                            j.optString("goal",goal),
                            list
                        )))
                    } catch (e: Exception) { callback(Result.failure(e)) }
                }
            }
        })
    }

    private var currentKey: String? = null
    fun setKey(key: String) { currentKey = key.trim().ifBlank { null } }
}
