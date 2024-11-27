package dev.dwidi.springbootkotlinjwt.dto.user

data class UserRequestDTO(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String,
    val phoneNumber: String? = null
)