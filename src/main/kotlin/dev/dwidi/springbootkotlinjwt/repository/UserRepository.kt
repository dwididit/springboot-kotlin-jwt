package dev.dwidi.springbootkotlinjwt.repository

import dev.dwidi.springbootkotlinjwt.entity.User
import org.springframework.data.mongodb.repository.MongoRepository

interface UserRepository : MongoRepository<User, String>{
    fun findByEmail(email: String): User?
    fun existsUserByEmail(email: String): Boolean
}