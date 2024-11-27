package dev.dwidi.springbootkotlinjwt.entity

import dev.dwidi.springbootkotlinjwt.enums.Role
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document(collection = "users")
data class User(
    @Id
    val id: String,

    val firstName: String,

    val lastName: String,

    @Indexed(unique = true)
    val email: String,

    val password: String,

    val phoneNumber: String? = null,

    val roles: Set<Role> = setOf(Role.USER),

    val isVerified: Boolean = true,

    val createdAt: LocalDateTime = LocalDateTime.now(),

    val updatedAt: LocalDateTime = LocalDateTime.now(),
)