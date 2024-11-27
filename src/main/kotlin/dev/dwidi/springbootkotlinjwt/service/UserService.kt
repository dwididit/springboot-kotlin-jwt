package dev.dwidi.springbootkotlinjwt.service

import dev.dwidi.springbootkotlinjwt.dto.BaseResponseDTO
import dev.dwidi.springbootkotlinjwt.dto.auth.AuthRequestDTO
import dev.dwidi.springbootkotlinjwt.dto.auth.AuthResponseDTO
import dev.dwidi.springbootkotlinjwt.dto.auth.LoginRequestDTO
import dev.dwidi.springbootkotlinjwt.dto.auth.LoginResponseDTO
import dev.dwidi.springbootkotlinjwt.dto.user.UserRequestDTO
import dev.dwidi.springbootkotlinjwt.dto.user.UserResponseDTO
import dev.dwidi.springbootkotlinjwt.dto.user.UserSearchCriteria
import dev.dwidi.springbootkotlinjwt.dto.user.UserUpdateRequestDTO
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface UserService {
    fun createUser(request: UserRequestDTO): BaseResponseDTO<UserResponseDTO>
    fun findUsers(criteria: UserSearchCriteria, pageable: Pageable): BaseResponseDTO<Page<UserResponseDTO>>
    fun updateUser(id: String, request: UserUpdateRequestDTO): BaseResponseDTO<UserResponseDTO>
    fun deleteUser(id: String): BaseResponseDTO<UserResponseDTO>
    fun loginUser(request: LoginRequestDTO): BaseResponseDTO<LoginResponseDTO>
    fun logoutUser(request: AuthRequestDTO): BaseResponseDTO<AuthResponseDTO>
    fun generateNewAccessToken(request: AuthRequestDTO): BaseResponseDTO<LoginResponseDTO>
}