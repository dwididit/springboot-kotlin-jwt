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
import dev.dwidi.springbootkotlinjwt.entity.RefreshToken
import dev.dwidi.springbootkotlinjwt.entity.User
import dev.dwidi.springbootkotlinjwt.enums.Role
import dev.dwidi.springbootkotlinjwt.repository.RefreshTokenRepository
import dev.dwidi.springbootkotlinjwt.repository.UserRepository
import dev.dwidi.springbootkotlinjwt.security.JwtService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.support.PageableExecutionUtils
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val mongoTemplate: MongoTemplate,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val authenticationManager: AuthenticationManager
): UserService {
    override fun createUser(request: UserRequestDTO): BaseResponseDTO<UserResponseDTO> {
        if (userRepository.existsUserByEmail(request.email)) {
            return BaseResponseDTO(
                statusCode = HttpStatus.BAD_REQUEST.value(),
                message = "Email already exist",
                data = null
            )
        }

        val user = User(
            id = UUID.randomUUID().toString(),
            firstName = request.firstName,
            lastName = request.lastName,
            email = request.email,
            password = passwordEncoder.encode(request.password),
            phoneNumber = request.phoneNumber,
            roles = setOf(Role.USER),
            isVerified = false,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val savedUser = userRepository.save(user)

        return BaseResponseDTO(
            statusCode = HttpStatus.CREATED.value(),
            message = "User created successfully",
            data = savedUser.toResponseDTO()
        )
    }

    override fun findUsers(criteria: UserSearchCriteria, pageable: Pageable): BaseResponseDTO<Page<UserResponseDTO>> {
        val query = Query().with(pageable)
        val criteriaList = mutableListOf<Criteria>()

        criteria.email?.let {
            criteriaList.add(Criteria.where("email").regex(it, "i"))
        }

        criteria.firstName?.let {
            criteriaList.add(Criteria.where("firstName").regex(it, "i"))
        }

        criteria.lastName?.let {
            criteriaList.add(Criteria.where("lastName").regex(it, "i"))
        }

        if (criteriaList.isNotEmpty()) {
            query.addCriteria(Criteria().andOperator(*criteriaList.toTypedArray()))
        }

        val users = mongoTemplate.find(query, User::class.java)
        val page = PageableExecutionUtils.getPage(
            users,
            pageable
        ) { mongoTemplate.count(Query.of(query).limit(-1).skip(-1), User::class.java) }

        return BaseResponseDTO(
            statusCode = HttpStatus.OK.value(),
            message = "Users retrieved successfully",
            data = page.map { it.toResponseDTO() }
        )
    }

    override fun updateUser(id: String, request: UserUpdateRequestDTO): BaseResponseDTO<UserResponseDTO> {
        val existingUser = userRepository.findById(id).orElse(null)
            ?: return BaseResponseDTO(
                statusCode = HttpStatus.NOT_FOUND.value(),
                message = "User not found",
                data = null
            )

        if (request.email != existingUser.email &&
            userRepository.existsUserByEmail(request.email)) {
            return BaseResponseDTO(
                statusCode = HttpStatus.BAD_REQUEST.value(),
                message = "Email already exists",
                data = null
            )
        }

        val updatedUser = existingUser.copy(
            firstName = request.firstName,
            lastName = request.lastName,
            email = request.email,
            phoneNumber = request.phoneNumber,
            updatedAt = LocalDateTime.now()
        )

        val savedUser = userRepository.save(updatedUser)
        return BaseResponseDTO(
            statusCode = HttpStatus.OK.value(),
            message = "User successfully update",
            data = savedUser.toResponseDTO()
        )
    }

    override fun deleteUser(id: String): BaseResponseDTO<UserResponseDTO> {
        if (!userRepository.existsById(id)) {
            return BaseResponseDTO(
                statusCode = HttpStatus.NOT_FOUND.value(),
                message = "User not found",
                data = null
            )
        }

        userRepository.deleteById(id)
        return BaseResponseDTO(
            statusCode = HttpStatus.OK.value(),
            message = "User successfully deleted",
            data = null
        )
    }

    override fun loginUser(request: LoginRequestDTO): BaseResponseDTO<LoginResponseDTO> {
        try {
            val user = userRepository.findByEmail(request.email)
                ?: return BaseResponseDTO(
                    statusCode = HttpStatus.NOT_FOUND.value(),
                    message = "User not found",
                    data = null
                )

            val authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(
                    request.email,
                    request.password
                )
            )

            val userRoles = if (user.roles.isEmpty()) {
                setOf(Role.USER)
            } else {
                user.roles
            }

            val accessToken = jwtService.generateAccessToken(
                userId = user.id,
                roles = userRoles.map { "ROLE_${it.name}" }
            )
            val refreshToken = jwtService.generateRefreshToken(
                userId = user.id,
                roles = userRoles.map { "ROLE_${it.name}" }
            )

            refreshTokenRepository.findByUserId(user.id).forEach { token ->
                refreshTokenRepository.save(token.copy(isRevoked = true))
            }

            refreshTokenRepository.save(
                RefreshToken(
                    id = UUID.randomUUID().toString(),
                    userId = user.id,
                    refreshToken = refreshToken,
                    isRevoked = false,
                    expiryDate = LocalDateTime.now().plusDays(7)
                )
            )

            return BaseResponseDTO(
                statusCode = HttpStatus.OK.value(),
                message = "Login successful",
                data = LoginResponseDTO(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                )
            )
        } catch (e: BadCredentialsException) {
            return BaseResponseDTO(
                statusCode = HttpStatus.UNAUTHORIZED.value(),
                message = "Invalid email or password",
                data = null
            )
        } catch (e: Exception) {
            return BaseResponseDTO(
                statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                message = e.message.toString(),
                data = null
            )
        }
    }

    override fun logoutUser(request: AuthRequestDTO): BaseResponseDTO<AuthResponseDTO> {
        val refreshToken = request.authHeader.substring(7)

        val tokenEntity = refreshTokenRepository.findByRefreshToken(refreshToken)
            ?: return BaseResponseDTO(
                statusCode = HttpStatus.NOT_FOUND.value(),
                message = "Refresh token not found",
                data = null
            )

        val updatedTokenEntity = tokenEntity.copy(isRevoked = true)
        refreshTokenRepository.save(updatedTokenEntity)

        return BaseResponseDTO(
            statusCode = HttpStatus.OK.value(),
            message = "Logout successful"
        )
    }

    override fun generateNewAccessToken(request: AuthRequestDTO): BaseResponseDTO<LoginResponseDTO> {
        val refreshToken = request.authHeader.substring(7)

        val tokenEntity = refreshTokenRepository.findByRefreshToken(refreshToken)
            ?: return BaseResponseDTO(
                statusCode = HttpStatus.NOT_FOUND.value(),
                message = "Refresh token not found",
                data = null
            )

        if (tokenEntity.isRevoked) {
            return BaseResponseDTO(
                statusCode = HttpStatus.UNAUTHORIZED.value(),
                message = "Refresh token is revoke",
                data = null
            )
        }

        if (!jwtService.validateRefreshToken(refreshToken)) {
            return BaseResponseDTO(
                statusCode = HttpStatus.UNAUTHORIZED.value(),
                message = "Invalid or expired refresh token",
                data = null
            )
        }

        val newAccessToken = jwtService.generateAccessToken(tokenEntity.userId, jwtService.getRolesFromToken(refreshToken))
        val newRefreshToken = jwtService.generateRefreshToken(tokenEntity.userId, jwtService.getRolesFromToken(refreshToken))

        refreshTokenRepository.save(tokenEntity.copy(isRevoked = true))

        refreshTokenRepository.save(
            RefreshToken(
                id = UUID.randomUUID().toString(),
                userId = tokenEntity.userId,
                refreshToken = newRefreshToken,
                isRevoked = false,
                expiryDate = LocalDateTime.now().plusDays(7)
            )
        )

        return BaseResponseDTO(
            statusCode = HttpStatus.OK.value(),
            message = "New access token generated",
            data = LoginResponseDTO(
                accessToken = newAccessToken,
                refreshToken = newRefreshToken
            )
        )
    }


    private fun User.toResponseDTO(): UserResponseDTO {
        return UserResponseDTO(
            id = this.id,
            firstName = this.firstName,
            lastName = this.lastName,
            email = this.email,
            phoneNumber = this.phoneNumber,
            roles = this.roles,
            isVerified = this.isVerified,
            createdAt = this.createdAt.toString(),
            updatedAt = this.updatedAt.toString()
        )
    }
}