package dev.dwidi.springbootkotlinjwt.repository

import dev.dwidi.springbootkotlinjwt.entity.User
import dev.dwidi.springbootkotlinjwt.enums.Role
import org.bson.types.ObjectId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.Index
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDateTime

@Testcontainers
@DataMongoTest
class UserRepositoryTest {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    companion object {
        @Container
        private val mongoDBContainer = MongoDBContainer("mongo:6.0")
            .apply {
                withExposedPorts(27017)
                withStartupTimeout(java.time.Duration.ofSeconds(60))
                start()
            }

        @JvmStatic
        @DynamicPropertySource
        fun registerMongoProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.mongodb.uri") { mongoDBContainer.replicaSetUrl }
        }
    }

    @BeforeEach
    fun cleanup() {
        userRepository.deleteAll()
    }

    @Test
    fun `should create user successfully`() {
        val now = LocalDateTime.now()
        val user = User(
            id = "",
            firstName = "John",
            lastName = "Smith",
            email = "john.smith@example.com",
            password = "hashedPassword",
            phoneNumber = "+1234567890",
            roles = setOf(Role.USER),
            isVerified = false,
            createdAt = now,
            updatedAt = now
        )

        val savedUser = userRepository.save(user)

        assertNotNull(savedUser.id)
        assertEquals("John", savedUser.firstName)
        assertEquals("john.smith@example.com", savedUser.email)
        assertEquals(setOf(Role.USER), savedUser.roles)
    }

    @Test
    fun `should find user by email`() {
        val now = LocalDateTime.now()
        val email = "unique.email@example.com"
        val user = User(
            id = "",
            firstName = "John",
            lastName = "Smith",
            email = email,
            password = "hashedPassword",
            phoneNumber = "+1234567890",
            roles = setOf(Role.USER),
            isVerified = false,
            createdAt = now,
            updatedAt = now
        )
        val savedUser = userRepository.save(user)

        val foundUser = userRepository.findByEmail(email)

        assertNotNull(foundUser)
        assertEquals(email, foundUser?.email)
        assertEquals(savedUser.id, foundUser?.id)
    }

    @Test
    fun `should delete user`() {
        val now = LocalDateTime.now()
        val user = User(
            id = "",
            firstName = "John",
            lastName = "Smith",
            email = "john.delete@example.com",
            password = "hashedPassword",
            phoneNumber = "+1234567890",
            roles = setOf(Role.USER),
            isVerified = false,
            createdAt = now,
            updatedAt = now
        )
        val savedUser = userRepository.save(user)

        userRepository.deleteById(savedUser.id)

        val deletedUser = userRepository.findById(savedUser.id)
        assertTrue(deletedUser.isEmpty)
    }

    @Test
    fun `should return null when finding user with non-existent email`() {
        val user = userRepository.findByEmail("nonexistent@example.com")
        assertNull(user)
    }

    @Test
    fun `should return false when checking non-existent email`() {
        val exists = userRepository.existsUserByEmail("nonexistent@example.com")
        assertFalse(exists)
    }

    @Test
    fun `should return true when checking existing email`() {
        val now = LocalDateTime.now()
        val email = "test@example.com"

        val user = User(
            id = "",
            firstName = "Test",
            lastName = "User",
            email = email,
            password = "password",
            roles = setOf(Role.USER),
            createdAt = now,
            updatedAt = now
        )
        userRepository.save(user)

        val exists = userRepository.existsUserByEmail(email)
        assertTrue(exists)
    }

    @Test
    fun `should throw exception when deleting non-existent user`() {
        assertThrows<IllegalArgumentException> {
            val nonExistentId = "non-existent-id"
            val user = userRepository.findById(nonExistentId)
                .orElseThrow { IllegalArgumentException("User not found") }
            userRepository.delete(user)
        }
    }

    @Test
    fun `should throw exception when creating user with existing email`() {
        mongoTemplate.indexOps(User::class.java)
            .ensureIndex(Index().on("email", Sort.Direction.ASC).unique())

        val now = LocalDateTime.now()
        val email = "duplicate@example.com"
        val user1 = User(
            id = ObjectId().toString(),
            firstName = "John",
            lastName = "Doe",
            email = email,
            password = "pass",
            roles = setOf(Role.USER),
            isVerified = false,
            createdAt = now,
            updatedAt = now
        )
        mongoTemplate.save(user1)

        val user2 = user1.copy(id = ObjectId().toString())
        assertThrows<DuplicateKeyException> {
            mongoTemplate.save(user2)
        }
    }
}