package dev.dwidi.springbootkotlinjwt.dto.auth

data class LoginRequestDTO(
    val email: String,
    val password: String
)
