package com.example.data

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface GitHubApiService {

    @GET("user/repos")
    suspend fun getUserRepos(
        @Header("Authorization") token: String,
        @Query("sort") sort: String = "updated",
        @Query("per_page") perPage: Int = 100
    ): List<GithubRepo>

    @GET("repos/{owner}/{repo}/git/trees/HEAD")
    suspend fun getRepositoryTree(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("recursive") recursive: Int = 1
    ): GithubTreeResponse

    @GET("repos/{owner}/{repo}/git/blobs/{sha}")
    suspend fun getBlobContent(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("sha") sha: String
    ): GithubBlobResponse
}
