package com.example.data

import android.util.Base64
import kotlinx.coroutines.flow.Flow

class AuditRepository(private val auditDao: AuditDao) {

    val allAudits: Flow<List<AuditRecord>> = auditDao.getAllAudits()

    suspend fun getAuditById(id: Int): AuditRecord? {
        return auditDao.getAuditById(id)
    }

    suspend fun saveAudit(record: AuditRecord): Long {
        return auditDao.insertAudit(record)
    }

    suspend fun deleteAuditById(id: Int) {
        auditDao.deleteAuditById(id)
    }

    suspend fun getRepositories(token: String): List<GithubRepo> {
        val authHeader = "Bearer $token"
        return RetrofitClients.githubService.getUserRepos(authHeader)
    }

    suspend fun getRepositoryTree(token: String, owner: String, repo: String): GithubTreeResponse {
        val authHeader = "Bearer $token"
        return RetrofitClients.githubService.getRepositoryTree(authHeader, owner, repo)
    }

    suspend fun getFileContent(token: String, owner: String, repo: String, sha: String): String {
        val authHeader = "Bearer $token"
        val response = RetrofitClients.githubService.getBlobContent(authHeader, owner, repo, sha)
        val cleaned = response.content.replace("\n", "").replace("\r", "")
        val bytes = Base64.decode(cleaned, Base64.DEFAULT)
        return String(bytes, Charsets.UTF_8)
    }

    suspend fun analyzeRepository(
        apiKey: String,
        model: String,
        prompt: String,
        systemInstruction: String
    ): GeminiResponse {
        val request = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemInstruction))),
            generationConfig = GeminiGenerationConfig(temperature = 0.2f)
        )
        return RetrofitClients.geminiService.generateContent(model, apiKey, request)
    }
}
