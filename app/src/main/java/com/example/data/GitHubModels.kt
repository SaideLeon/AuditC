package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GithubRepo(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "full_name") val fullName: String,
    @Json(name = "description") val description: String?,
    @Json(name = "private") val isPrivate: Boolean,
    @Json(name = "html_url") val htmlUrl: String,
    @Json(name = "language") val language: String?,
    @Json(name = "stargazers_count") val stars: Int = 0,
    @Json(name = "owner") val owner: GithubOwner
)

@JsonClass(generateAdapter = true)
data class GithubOwner(
    @Json(name = "login") val login: String,
    @Json(name = "avatar_url") val avatarUrl: String
)

@JsonClass(generateAdapter = true)
data class GithubTreeResponse(
    @Json(name = "sha") val sha: String,
    @Json(name = "url") val url: String,
    @Json(name = "tree") val tree: List<GithubTreeEntry>,
    @Json(name = "truncated") val truncated: Boolean
)

@JsonClass(generateAdapter = true)
data class GithubTreeEntry(
    @Json(name = "path") val path: String,
    @Json(name = "mode") val mode: String?,
    @Json(name = "type") val type: String, // "blob" or "tree"
    @Json(name = "sha") val sha: String,
    @Json(name = "size") val size: Long = 0,
    @Json(name = "url") val url: String?
)

@JsonClass(generateAdapter = true)
data class GithubBlobResponse(
    @Json(name = "content") val content: String, // base64 encoded
    @Json(name = "encoding") val encoding: String
)
