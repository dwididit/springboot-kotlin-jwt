package dev.dwidi.springbootkotlinjwt.controller

import dev.dwidi.springbootkotlinjwt.dto.BaseResponseDTO
import dev.dwidi.springbootkotlinjwt.dto.auth.AuthRequestDTO
import dev.dwidi.springbootkotlinjwt.dto.auth.AuthResponseDTO
import dev.dwidi.springbootkotlinjwt.dto.auth.LoginRequestDTO
import dev.dwidi.springbootkotlinjwt.dto.auth.LoginResponseDTO
import dev.dwidi.springbootkotlinjwt.dto.user.UserRequestDTO
import dev.dwidi.springbootkotlinjwt.dto.user.UserResponseDTO
import dev.dwidi.springbootkotlinjwt.service.UserService
import lombok.extern.slf4j.Slf4j
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userService: UserService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(UserController::class.java)
    }

    @PostMapping("/signup")
    fun createUser(@RequestBody request: UserRequestDTO): BaseResponseDTO<UserResponseDTO> {
        logger.info("Creating new user with email: ${request.email}")
        return userService.createUser(request)
    }

    @PostMapping("/login")
    fun loginUser(@RequestBody request: LoginRequestDTO): BaseResponseDTO<LoginResponseDTO> {
        logger.info("Logging in user with email: ${request.email}")
        return userService.loginUser(request)
    }

    @PostMapping("/logout")
    fun logoutUser(@RequestHeader("Authorization") authHeader: String): BaseResponseDTO<AuthResponseDTO> {
        logger.info("Processing logout request")
        return userService.logoutUser(AuthRequestDTO(authHeader))
    }

    @PostMapping("/token")
    fun generateNewAccessToken(@RequestHeader("Authorization") authHeader: String): BaseResponseDTO<LoginResponseDTO> {
        logger.info("Generating new access token")
        return userService.generateNewAccessToken(AuthRequestDTO(authHeader))
    }
}