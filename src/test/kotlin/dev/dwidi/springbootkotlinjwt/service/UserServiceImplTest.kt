package dev.dwidi.springbootkotlinjwt.service

import dev.dwidi.springbootkotlinjwt.dto.auth.LoginRequestDTO
import dev.dwidi.springbootkotlinjwt.dto.user.UserRequestDTO
import dev.dwidi.springbootkotlinjwt.dto.user.UserUpdateRequestDTO
import dev.dwidi.springbootkotlinjwt.entity.User
import dev.dwidi.springbootkotlinjwt.enums.Role
import dev.dwidi.springbootkotlinjwt.repository.RefreshTokenRepository
import dev.dwidi.springbootkotlinjwt.repository.UserRepository
import dev.dwidi.springbootkotlinjwt.security.JwtService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDateTime

@SpringBootTest
@Testcontainers
class UserServiceImplTest {

    companion object {
        @Container
        private val mongoDBContainer = MongoDBContainer("mongo:6.0")
            .apply {
                withExposedPorts(27017)
                start()
            }

        @JvmStatic
        @DynamicPropertySource
        fun registerMongoProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.mongodb.uri") { mongoDBContainer.replicaSetUrl }
        }
    }

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var jwtService: JwtService

    @Autowired
    private lateinit var authenticationManager: AuthenticationManager

    @Autowired
    private lateinit var userService: UserServiceImpl

    private val now = LocalDateTime.now()

    @BeforeEach
    fun setup() {
        // Clean the database before each test
        mongoTemplate.dropCollection(User::class.java)
        refreshTokenRepository.deleteAll()
    }

    @Test
    fun `should create user successfully`() {
        // Given
        val request = UserRequestDTO(
            firstName = "John",
            lastName = "Doe",
            email = "john@example.com",
            password = "password123",
            phoneNumber = "1234567890"
        )

        // When
        val result = userService.createUser(request)

        // Then
        assertEquals(HttpStatus.CREATED.value(), result.statusCode)
        assertEquals("User created successfully", result.message)
        assertNotNull(result.data)
        assertEquals(request.email, result.data?.email)

        // Verify user was actually saved in MongoDB
        val savedUser = userRepository.findByEmail(request.email)
        assertNotNull(savedUser)
        assertEquals(request.firstName, savedUser?.firstName)
        assertEquals(request.lastName, savedUser?.lastName)
        assertTrue(passwordEncoder.matches(request.password, savedUser?.password))
    }

    @Test
    fun `should return error when creating user with existing email`() {
        // Given
        val existingUser = User(
            id = "test-id-1",
            firstName = "John",
            lastName = "Doe",
            email = "existing@example.com",
            password = passwordEncoder.encode("password123"),
            roles = setOf(Role.USER),
            isVerified = false,
            createdAt = now,
            updatedAt = now
        )
        userRepository.save(existingUser)

        val request = UserRequestDTO(
            firstName = "Jane",
            lastName = "Doe",
            email = "existing@example.com",
            password = "password123",
            phoneNumber = "1234567890"
        )

        // When
        val result = userService.createUser(request)

        // Then
        assertEquals(HttpStatus.BAD_REQUEST.value(), result.statusCode)
        assertEquals("Email already exist", result.message)
        assertNull(result.data)
    }

    @Test
    fun `should login user successfully`() {
        // Given
        val password = "password123"
        val user = User(
            id = "test-id-1",
            firstName = "John",
            lastName = "Doe",
            email = "test@example.com",
            password = passwordEncoder.encode(password),
            roles = setOf(Role.USER),
            isVerified = true,
            createdAt = now,
            updatedAt = now
        )
        userRepository.save(user)

        val request = LoginRequestDTO(
            email = "test@example.com",
            password = password
        )

        // When
        val result = userService.loginUser(request)

        // Then
        assertEquals(HttpStatus.OK.value(), result.statusCode)
        assertEquals("Login successful", result.message)
        assertNotNull(result.data?.accessToken)
        assertNotNull(result.data?.refreshToken)

        // Verify refresh token was saved
        val savedRefreshTokens = refreshTokenRepository.findByUserId(user.id!!)
        assertTrue(savedRefreshTokens.isNotEmpty())
    }

    @Test
    fun `should update user successfully`() {
        // Given
        val user = User(
            id = "test-id-1",
            firstName = "John",
            lastName = "Doe",
            email = "john@example.com",
            password = passwordEncoder.encode("password123"),
            roles = setOf(Role.USER),
            isVerified = true,
            createdAt = now,
            updatedAt = now
        )
        val savedUser = userRepository.save(user)

        val request = UserUpdateRequestDTO(
            firstName = "John",
            lastName = "Updated",
            email = "john.updated@example.com",
            password = "newpassword123",
            phoneNumber = "9876543210"
        )

        // When
        val result = userService.updateUser(savedUser.id!!, request)

        // Then
        assertEquals(HttpStatus.OK.value(), result.statusCode)
        assertEquals("User successfully updated", result.message)
        assertNotNull(result.data)

        // Verify changes in database
        val updatedUser = userRepository.findById(savedUser.id!!).orElse(null)
        assertNotNull(updatedUser)
        assertEquals(request.lastName, updatedUser.lastName)
        assertEquals(request.email, updatedUser.email)
        assertEquals(request.phoneNumber, updatedUser.phoneNumber)
    }

    @Test
    fun `should delete user successfully`() {
        // Given
        val user = User(
            id = "test-id-1",
            firstName = "John",
            lastName = "Doe",
            email = "john@example.com",
            password = passwordEncoder.encode("password123"),
            roles = setOf(Role.USER),
            isVerified = true,
            createdAt = now,
            updatedAt = now
        )
        val savedUser = userRepository.save(user)

        // When
        val result = userService.deleteUser(savedUser.id!!)

        // Then
        assertEquals(HttpStatus.OK.value(), result.statusCode)
        assertEquals("User successfully deleted", result.message)

        // Verify user was actually deleted from database
        assertFalse(userRepository.existsById(savedUser.id!!))
    }

    @Test
    fun `should not login with incorrect password`() {
        // Given
        val correctPassword = "password123"
        val user = User(
            id = "test-id-5",
            firstName = "John",
            lastName = "Doe",
            email = "test@example.com",
            password = passwordEncoder.encode(correctPassword),
            roles = setOf(Role.USER),
            isVerified = true,
            createdAt = now,
            updatedAt = now
        )
        userRepository.save(user)

        val request = LoginRequestDTO(
            email = "test@example.com",
            password = "wrongpassword"
        )

        // When
        val result = userService.loginUser(request)

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED.value(), result.statusCode)
        assertEquals("Invalid email or password", result.message)
        assertNull(result.data)
    }

    @Test
    fun `should not update user with non-existent id`() {
        // Given
        val nonExistentId = "non-existent-id"
        val request = UserUpdateRequestDTO(
            firstName = "John",
            lastName = "Updated",
            email = "updated@example.com",
            password = "newpassword123",
            phoneNumber = "9876543210"
        )

        // When
        val result = userService.updateUser(nonExistentId, request)

        // Then
        assertEquals(HttpStatus.NOT_FOUND.value(), result.statusCode)
        assertEquals("User not found", result.message)
        assertNull(result.data)
    }

    @Test
    fun `should not update user with existing email`() {
        // Given
        // First user
        val existingUser = User(
            id = "test-id-6",
            firstName = "Jane",
            lastName = "Doe",
            email = "jane@example.com",
            password = passwordEncoder.encode("password123"),
            roles = setOf(Role.USER),
            isVerified = true,
            createdAt = now,
            updatedAt = now
        )
        userRepository.save(existingUser)

        // Second user
        val userToUpdate = User(
            id = "test-id-7",
            firstName = "John",
            lastName = "Doe",
            email = "john@example.com",
            password = passwordEncoder.encode("password123"),
            roles = setOf(Role.USER),
            isVerified = true,
            createdAt = now,
            updatedAt = now
        )
        userRepository.save(userToUpdate)

        // Try to update second user with first user's email
        val request = UserUpdateRequestDTO(
            firstName = "John",
            lastName = "Updated",
            email = "jane@example.com",
            password = "newpassword123",
            phoneNumber = "9876543210"
        )

        // When
        val result = userService.updateUser(userToUpdate.id!!, request)

        // Then
        assertEquals(HttpStatus.BAD_REQUEST.value(), result.statusCode)
        assertEquals("Email already exists", result.message)
        assertNull(result.data)
    }

    @Test
    fun `should not delete non-existent user`() {
        // Given
        val nonExistentId = "non-existent-id"

        // When
        val result = userService.deleteUser(nonExistentId)

        // Then
        assertEquals(HttpStatus.NOT_FOUND.value(), result.statusCode)
        assertEquals("User not found", result.message)
    }

    @Test
    fun `should update user with partial fields successfully`() {
        // Given
        val user = User(
            id = "test-id-8",
            firstName = "John",
            lastName = "Doe",
            email = "john@example.com",
            password = passwordEncoder.encode("password123"),
            phoneNumber = "1234567890",
            roles = setOf(Role.USER),
            isVerified = true,
            createdAt = now,
            updatedAt = now
        )
        val savedUser = userRepository.save(user)

        // Update only firstName and lastName
        val request = UserUpdateRequestDTO(
            firstName = "Johnny",
            lastName = "Smith",
            email = null,
            password = null,
            phoneNumber = null
        )

        // When
        val result = userService.updateUser(savedUser.id!!, request)

        // Then
        assertEquals(HttpStatus.OK.value(), result.statusCode)
        assertEquals("User successfully updated", result.message)
        assertNotNull(result.data)

        // Verify only requested fields were updated
        assertEquals("Johnny", result.data?.firstName)
        assertEquals("Smith", result.data?.lastName)
        assertEquals(savedUser.email, result.data?.email)
        assertEquals(savedUser.phoneNumber, result.data?.phoneNumber)

        // Verify in database
        val updatedUser = userRepository.findById(savedUser.id!!).orElse(null)
        assertNotNull(updatedUser)
        assertEquals("Johnny", updatedUser.firstName)
        assertEquals("Smith", updatedUser.lastName)
        assertEquals(savedUser.email, updatedUser.email)
        assertEquals(savedUser.phoneNumber, updatedUser.phoneNumber)
    }

    @Test
    fun `should not update user when no fields provided`() {
        // Given
        val user = User(
            id = "test-id-9",
            firstName = "John",
            lastName = "Doe",
            email = "john@example.com",
            password = passwordEncoder.encode("password123"),
            roles = setOf(Role.USER),
            isVerified = true,
            createdAt = now,
            updatedAt = now
        )
        val savedUser = userRepository.save(user)

        val request = UserUpdateRequestDTO(
            firstName = null,
            lastName = null,
            email = null,
            password = null,
            phoneNumber = null
        )

        // When
        val result = userService.updateUser(savedUser.id!!, request)

        // Then
        assertEquals(HttpStatus.BAD_REQUEST.value(), result.statusCode)
        assertEquals("At least one field is required for update", result.message)
        assertNull(result.data)

        // Verify user was not changed in database
        val unchangedUser = userRepository.findById(savedUser.id!!).orElse(null)
        assertNotNull(unchangedUser)
        assertEquals(savedUser.firstName, unchangedUser.firstName)
        assertEquals(savedUser.lastName, unchangedUser.lastName)
        assertEquals(savedUser.email, unchangedUser.email)
    }

    @Test
    fun `should not update user with empty string values`() {
        // Given
        val user = User(
            id = "test-id-8",
            firstName = "John",
            lastName = "Doe",
            email = "john@example.com",
            password = passwordEncoder.encode("password123"),
            roles = setOf(Role.USER),
            isVerified = true,
            createdAt = now,
            updatedAt = now
        )
        val savedUser = userRepository.save(user)

        // Request with empty strings
        val request = UserUpdateRequestDTO(
            firstName = "",
            lastName = "",
            email = "",
            password = "",
            phoneNumber = ""
        )

        // When
        val result = userService.updateUser(savedUser.id!!, request)

        // Then
        assertEquals(HttpStatus.BAD_REQUEST.value(), result.statusCode)
        assertEquals("Invalid update data: Fields cannot be empty", result.message)
        assertNull(result.data)

        // Verify user was not changed in database
        val unchangedUser = userRepository.findById(savedUser.id!!).orElse(null)
        assertNotNull(unchangedUser)
        assertEquals(user.firstName, unchangedUser.firstName)
        assertEquals(user.lastName, unchangedUser.lastName)
        assertEquals(user.email, unchangedUser.email)
        assertEquals(user.password, unchangedUser.password)
        assertEquals(user.phoneNumber, unchangedUser.phoneNumber)
    }
}