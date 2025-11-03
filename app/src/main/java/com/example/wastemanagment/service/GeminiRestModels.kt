package com.example.wastemanagment.service

// Request models
data class GeminiContentRequest(
    val contents: List<GeminiContent>
)

data class GeminiContent(
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String
)

// Response models (minimal fields we need)
data class GeminiGenerateContentResponse(
    val candidates: List<GeminiCandidate>?
)

data class GeminiCandidate(
    val content: GeminiContentResponse?
)

data class GeminiContentResponse(
    val parts: List<GeminiPartResponse>?
)

data class GeminiPartResponse(
    val text: String?
)


