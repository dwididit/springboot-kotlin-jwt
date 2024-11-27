package dev.dwidi.springbootkotlinjwt.dto.user

import dev.dwidi.springbootkotlinjwt.enums.Role

data class UserResponseDTO(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String?,
    val roles: Set<Role>,
    val isVerified : Boolean,
    val createdAt: String,
    val updatedAt: String
)