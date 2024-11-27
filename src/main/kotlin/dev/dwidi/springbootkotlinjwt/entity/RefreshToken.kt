package dev.dwidi.springbootkotlinjwt.entity

import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime
import java.util.*

@Document(collection = "refresh_tokens")
data class RefreshToken(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val refreshToken: String,
    val expiryDate: LocalDateTime,
    var isRevoked: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now()
)