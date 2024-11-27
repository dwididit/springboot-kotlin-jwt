package dev.dwidi.springbootkotlinjwt.repository

import dev.dwidi.springbootkotlinjwt.entity.RefreshToken
import org.springframework.data.mongodb.repository.MongoRepository

interface RefreshTokenRepository : MongoRepository<RefreshToken, String> {
    fun findByUserId(userId: String): List<RefreshToken>
    fun findByRefreshToken(refreshToken: String): RefreshToken?
}