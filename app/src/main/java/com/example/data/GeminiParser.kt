package com.example.data

import org.json.JSONObject

data class AuditAnalysisResult(
    val score: Int,
    val status: String,
    val criticalCount: Int,
    val highCount: Int,
    val mediumCount: Int,
    val executiveReport: String,
    val blueprint: String
)

object GeminiParser {

    fun parseResponse(rawResponse: String): AuditAnalysisResult {
        var cleaned = rawResponse.trim()
        
        // Remove markdown code blocks if the model wrapped the JSON
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.removePrefix("```json").removePrefix("```")
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.removeSuffix("```")
            }
            cleaned = cleaned.trim()
        }

        return try {
            val json = JSONObject(cleaned)
            AuditAnalysisResult(
                score = json.optInt("score", 100),
                status = json.optString("status", "APROVADO COM DISTINÇÃO"),
                criticalCount = json.optInt("criticalCount", 0),
                highCount = json.optInt("highCount", 0),
                mediumCount = json.optInt("mediumCount", 0),
                executiveReport = json.optString("executiveReport", "Nenhum relatório executivo gerado."),
                blueprint = json.optString("blueprint", "Nenhum blueprint gerado.")
            )
        } catch (e: Exception) {
            // Fallback: If parsing fails entirely, put the whole response into the blueprint field
            // so that the user still gets their security analysis.
            AuditAnalysisResult(
                score = 0,
                status = "REPROVADO",
                criticalCount = 1,
                highCount = 0,
                mediumCount = 0,
                executiveReport = "### Erro na Estrutura do JSON\nA resposta do modelo não pôde ser analisada como JSON estruturado. Abaixo está a resposta bruta gerada pelo Gemini.",
                blueprint = rawResponse
            )
        }
    }
}
