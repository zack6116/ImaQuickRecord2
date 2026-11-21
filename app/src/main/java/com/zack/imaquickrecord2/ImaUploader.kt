package com.zack.imaquickrecord2

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ImaUploader(
    private val clientId: String,
    private val apiKey: String,
    private val knowledgeBaseId: String
) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    suspend fun upload(file: java.io.File): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val mediaId = findMediaId(file.name) ?: createMedia(file.name, file.length())
            val data = post(
                "upload_media",
                JSONObject()
                    .put("media_id", mediaId)
                    .put("knowledge_base_id", knowledgeBaseId)
                    .put("file_base64", android.util.Base64.encodeToString(file.readBytes(), android.util.Base64.NO_WRAP))
            )
            check(data.optString("media_id").isNotBlank())
            mediaId
        }
    }

    private suspend fun findMediaId(fileName: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().put("query", fileName).put("cursor", "").put("knowledge_base_id", knowledgeBaseId)
            val data = post("search_knowledge", payload)
            firstMediaId(data.arrayOrEmpty("info_list") ?: data.arrayOrEmpty("knowledge_list"), fileName)
        }.getOrNull()
    }

    private suspend fun createMedia(fileName: String, size: Long): String = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("file_name", fileName)
            .put("file_size", size)
            .put("content_type", "audio/x-m4a")
            .put("knowledge_base_id", knowledgeBaseId)
            .put("file_ext", "m4a")
        val data = post("create_media", payload)
        data.getString("media_id")
    }

    private fun post(endpoint: String, payload: JSONObject): JSONObject {
        val response = http.newCall(request(endpoint, payload.toString().toRequestBody(json))).execute()
        response.use {
            val text = it.body?.string().orEmpty()
            check(it.isSuccessful) { "$endpoint HTTP ${it.code} $text" }
            val root = JSONObject(text)
            val code = root.optInt("code", root.optInt("retcode", -1))
            check(code == 0) { "$endpoint code=$code ${root.optString("message", root.optString("errmsg"))}" }
            return root.optJSONObject("data") ?: root
        }
    }

    private fun request(endpoint: String, body: okhttp3.RequestBody): Request =
        Request.Builder()
            .url("$BASE/$endpoint")
            .post(body)
            .header("ima-openapi-clientid", clientId)
            .header("ima-openapi-apikey", apiKey)
            .header("Content-Type", "application/json")
            .build()

    private fun JSONObject.arrayOrEmpty(name: String): JSONArray? = optJSONArray(name)

    private fun firstMediaId(list: JSONArray?, fileName: String): String? {
        if (list == null) return null
        val base = fileName.removeSuffix(".m4a")
        for (index in 0 until list.length()) {
            val item = list.optJSONObject(index) ?: continue
            val title = item.optString("title")
            if (title == fileName || title.contains(base, ignoreCase = true)) {
                val mediaId = item.optString("media_id")
                if (mediaId.isNotBlank()) return mediaId
            }
        }
        return null
    }

    companion object {
        private const val BASE = "https://ima.qq.com/openapi/wiki/v1"
        private val json = "application/json; charset=utf-8".toMediaType()
        const val TAG = "ImaUploader"
    }
}
