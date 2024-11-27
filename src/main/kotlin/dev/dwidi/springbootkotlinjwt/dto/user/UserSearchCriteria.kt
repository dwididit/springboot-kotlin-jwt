package dev.dwidi.springbootkotlinjwt.dto.user

data class UserSearchCriteria(
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null
)