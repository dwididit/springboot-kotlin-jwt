package dev.dwidi.springbootkotlinjwt.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.Key
import java.util.*

@Service
class JwtService {
    private val logger = LoggerFactory.getLogger(JwtService::class.java)
    private val clockSkewSeconds = 60L

    @Value("\${jwt.secret}")
    private lateinit var secret: String

    @Value("\${jwt.access-token.expiration}")
    private lateinit var accessTokenExpiration: String

    @Value("\${jwt.refresh-token.expiration}")
    private lateinit var refreshTokenExpiration: String

    private fun getSigningKey(): Key {
        val keyBytes = Base64.getEncoder().encode(secret.toByteArray())
        return Keys.hmacShaKeyFor(keyBytes)
    }

    private fun buildToken(
        userId: String,
        roles: Collection<String>,
        expiration: Long,
        claims: Map<String, Any> = emptyMap()
    ): String {
        val now = System.currentTimeMillis()

        return Jwts.builder()
            .setSubject(userId)
            .claim("roles", roles)
            .setIssuedAt(Date(now))
            .setExpiration(Date(now + expiration))
            .apply { claims.forEach { (key, value) -> claim(key, value) } }
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact()
    }

    private fun parseToken(token: String): Claims? {
        return try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .setAllowedClockSkewSeconds(clockSkewSeconds)
                .build()
                .parseClaimsJws(token)
                .body
        } catch (e: Exception) {
            logger.error("Token parsing failed: ${e.message}", e)
            null
        }
    }

    fun generateAccessToken(userId: String, roles: Collection<String>): String =
        buildToken(userId, roles, accessTokenExpiration.toLong())

    fun generateRefreshToken(userId: String, roles: Collection<String>): String =
        buildToken(
            userId,
            roles,
            refreshTokenExpiration.toLong(),
            mapOf("tokenType" to "REFRESH")
        )

    fun validateAccessToken(token: String): Boolean {
        val claims = parseToken(token) ?: return false
        return !claims.containsKey("tokenType")
    }

    fun validateRefreshToken(token: String): Boolean {
        val claims = parseToken(token) ?: return false
        return claims["tokenType"] == "REFRESH"
    }

    fun getRolesFromToken(token: String): Collection<String> {
        val claims = parseToken(token)
        @Suppress("UNCHECKED_CAST")
        return claims?.get("roles") as? Collection<String> ?: emptyList()
    }

    fun getUserIdFromToken(token: String): String? =
        parseToken(token)?.subject
}
