package dev.dwidi.springbootkotlinjwt.controller

import com.fasterxml.jackson.databind.ObjectMapper
import dev.dwidi.springbootkotlinjwt.dto.user.UserUpdateRequestDTO
import dev.dwidi.springbootkotlinjwt.entity.User
import dev.dwidi.springbootkotlinjwt.enums.Role
import dev.dwidi.springbootkotlinjwt.repository.RefreshTokenRepository
import dev.dwidi.springbootkotlinjwt.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class UserControllerTest {

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
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    private val now = LocalDateTime.now()

    @BeforeEach
    fun setup() {
        // Clean the database before each test
        mongoTemplate.dropCollection(User::class.java)
        refreshTokenRepository.deleteAll()
    }

    @Test
    @WithMockUser
    fun `should get users successfully`() {
        // Given
        val user1 = User(
            id = "test-id-1",
            firstName = "John",
            lastName = "Doe",
            email = "john@example.com",
            password = passwordEncoder.encode("password123"),
            phoneNumber = "1234567890",
            roles = setOf(Role.USER),
            isVerified = true,
            createdAt = now.minusHours(1),
            updatedAt = now
        )
        val user2 = User(
            id = "test-id-2",
            firstName = "Jane",
            lastName = "Doe",
            email = "jane@example.com",
            password = passwordEncoder.encode("password123"),
            phoneNumber = "0987654321",
            roles = setOf(Role.USER),
            isVerified = true,
            createdAt = now,
            updatedAt = now
        )
        userRepository.saveAll(listOf(user1, user2))

        // When & Then
        mockMvc.perform(
            get("/api/v1/users")
                .param("page", "0")
                .param("size", "10")
                .param("sortBy", "createdAt")
                .param("sortDirection", "DESC")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.statusCode").value(HttpStatus.OK.value()))
            .andExpect(jsonPath("$.data.content.length()").value(2))
            .andExpect(jsonPath("$.data.content[0].email").value("jane@example.com"))
            .andExpect(jsonPath("$.data.content[1].email").value("john@example.com"))
    }

    @Test
    @WithMockUser
    fun `should get users with search criteria successfully`() {
        // Given
        val user1 = User(
            id = "test-id-1",
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
        val user2 = User(
            id = "test-id-2",
            firstName = "Jane",
            lastName = "Smith",
            email = "jane@example.com",
            password = passwordEncoder.encode("password123"),
            phoneNumber = "0987654321",
            roles = setOf(Role.USER),
            isVerified = true,
            createdAt = now,
            updatedAt = now
        )
        userRepository.saveAll(listOf(user1, user2))

        // When & Then
        mockMvc.perform(get("/api/v1/users")
            .param("lastName", "Doe")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content.length()").value(1))
            .andExpect(jsonPath("$.data.content[0].email").value("john@example.com"))
    }

    @Test
    @WithMockUser
    fun `should update user successfully`() {
        // Given
        val user = User(
            id = "test-id-1",
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

        val updateRequest = UserUpdateRequestDTO(
            firstName = "John",
            lastName = "Updated",
            email = "john.updated@example.com",
            password = "newpassword",
            phoneNumber = "9876543210"
        )

        // When & Then
        mockMvc.perform(put("/api/v1/users/${savedUser.id}")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.statusCode").value(HttpStatus.OK.value()))
            .andExpect(jsonPath("$.message").value("User successfully updated"))
            .andExpect(jsonPath("$.data.lastName").value("Updated"))
            .andExpect(jsonPath("$.data.email").value("john.updated@example.com"))

        // Verify in database
        val updatedUser = userRepository.findById(savedUser.id!!).orElse(null)
        assert(updatedUser.lastName == "Updated")
        assert(updatedUser.email == "john.updated@example.com")
        assert(passwordEncoder.matches("newpassword", updatedUser.password))
    }

    @Test
    @WithMockUser
    fun `should delete user successfully`() {
        // Given
        val user = User(
            id = "test-id-1",
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

        // When & Then
        mockMvc.perform(delete("/api/v1/users/${savedUser.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.statusCode").value(HttpStatus.OK.value()))
            .andExpect(jsonPath("$.message").value("User successfully deleted"))

        // Verify in database
        assert(!userRepository.existsById(savedUser.id!!))
    }

    @Test
    @WithMockUser
    fun `should return 404 when updating non-existent user`() {
        // Given
        val updateRequest = UserUpdateRequestDTO(
            firstName = "John",
            lastName = "Updated",
            email = "john.updated@example.com",
            password = "newpassword",
            phoneNumber = "9876543210"
        )

        // When & Then
        mockMvc.perform(put("/api/v1/users/nonexistent-id")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.statusCode").value(HttpStatus.NOT_FOUND.value()))
            .andExpect(jsonPath("$.message").value("User not found"))
    }

    @Test
    @WithMockUser
    fun `should return 400 when updating user with existing email`() {
        // Given
        val existingUser = User(
            id = "test-id-1",
            firstName = "Jane",
            lastName = "Smith",
            email = "jane@example.com",
            password = passwordEncoder.encode("password123"),
            phoneNumber = "0987654321",
            roles = setOf(Role.USER),
            isVerified = true,
            createdAt = now,
            updatedAt = now
        )
        userRepository.save(existingUser)

        val userToUpdate = User(
            id = "test-id-2",
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
        val savedUser = userRepository.save(userToUpdate)

        val updateRequest = UserUpdateRequestDTO(
            firstName = "John",
            lastName = "Updated",
            email = "jane@example.com",
            password = "newpassword",
            phoneNumber = "9876543210"
        )

        // When & Then
        mockMvc.perform(put("/api/v1/users/${savedUser.id}")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.statusCode").value(HttpStatus.BAD_REQUEST.value()))
            .andExpect(jsonPath("$.message").value("Email already exists"))
    }

    @Test
    fun `should return 401 when unauthorized user tries to access endpoints`() {
        // When & Then - Get users
        mockMvc.perform(get("/api/v1/users"))
            .andExpect(status().isUnauthorized)

        // When & Then - Update user
        val updateRequest = UserUpdateRequestDTO(
            firstName = "John",
            lastName = "Updated",
            email = "john@example.com",
            password = "newpassword",
            phoneNumber = "9876543210"
        )

        mockMvc.perform(put("/api/v1/users/123")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isUnauthorized)

        // When & Then - Delete user
        mockMvc.perform(delete("/api/v1/users/123"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser
    fun `should return 404 when deleting non-existent user`() {
        // When & Then
        mockMvc.perform(delete("/api/v1/users/nonexistent-id"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.statusCode").value(HttpStatus.NOT_FOUND.value()))
            .andExpect(jsonPath("$.message").value("User not found"))
    }
}