package dev.dwidi.springbootkotlinjwt.dto.user

class UserUpdateRequestDTO(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String,
    val phoneNumber: String? = null
)