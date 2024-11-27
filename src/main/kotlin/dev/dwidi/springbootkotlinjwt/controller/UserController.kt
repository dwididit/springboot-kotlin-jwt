package dev.dwidi.springbootkotlinjwt.controller

import dev.dwidi.springbootkotlinjwt.dto.BaseResponseDTO
import dev.dwidi.springbootkotlinjwt.dto.user.UserResponseDTO
import dev.dwidi.springbootkotlinjwt.dto.user.UserSearchCriteria
import dev.dwidi.springbootkotlinjwt.dto.user.UserUpdateRequestDTO
import dev.dwidi.springbootkotlinjwt.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(UserController::class.java)
    }

    @GetMapping
    fun getUsers(
        @RequestParam(required = false) email: String?,
        @RequestParam(required = false) firstName: String?,
        @RequestParam(required = false) lastName: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "createdAt") sortBy: String,
        @RequestParam(defaultValue = "DESC") sortDirection: String
    ): BaseResponseDTO<Page<UserResponseDTO>> {
        logger.info("Fetching users with parameters: email=$email, firstName=$firstName, lastName=$lastName, page=$page, size=$size")

        val pageable = PageRequest.of(
            page,
            size,
            Sort.by(Sort.Direction.valueOf(sortDirection.uppercase()), sortBy)
        )

        val searchCriteria = UserSearchCriteria(
            email = email,
            firstName = firstName,
            lastName = lastName
        )

        return userService.findUsers(searchCriteria, pageable)
    }


    @PutMapping("/{id}")
    fun updateUser(@PathVariable id: String, @RequestBody request: UserUpdateRequestDTO): BaseResponseDTO<UserResponseDTO> {
        logger.info("Updating user with id: $id")
        return userService.updateUser(id, request)
    }

    @DeleteMapping("/{id}")
    fun deleteUser(@PathVariable id: String): BaseResponseDTO<UserResponseDTO> {
        logger.info("Deleting user with id: $id")
        return userService.deleteUser(id)
    }

}