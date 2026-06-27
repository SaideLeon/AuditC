package com.example

import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader

class ExampleUnitTest {
    @Test
    fun probeModels() {
        val key = BuildConfig.GEMINI_API_KEY
        println("PROBE: Key = $key")
        val urls = listOf(
            "https://generativelanguage.googleapis.com/v1beta/models?key=$key",
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash?key=$key",
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash?key=$key",
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash?key=$key",
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash?key=$key"
        )

        for (urlString in urls) {
            try {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                val code = conn.responseCode
                println("PROBE: URL=${urlString.substringBefore("?key")}, CODE=$code")
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                if (stream != null) {
                    val reader = BufferedReader(InputStreamReader(stream))
                    val response = reader.readLine() ?: ""
                    println("PROBE RESPONSE (truncated): ${response.take(300)}")
                }
            } catch (e: Exception) {
                println("PROBE ERROR for ${urlString.substringBefore("?key")}: ${e.message}")
            }
        }
    }
}
