package dev.dwidi.springbootkotlinjwt.security

import dev.dwidi.springbootkotlinjwt.controller.UserController
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(
    private val jwtService: JwtService,
    private val userDetailsService: CustomUserDetailsService
) : OncePerRequestFilter() {

    companion object {
        private val logger = LoggerFactory.getLogger(UserController::class.java)
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.servletPath
        return path.startsWith("/api/auth/") || path.startsWith("/error")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val authHeader = request.getHeader("Authorization")

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response)
                return
            }

            val jwt = authHeader.substring(7)
            val userId = jwtService.getUserIdFromToken(jwt)

            if (userId != null && SecurityContextHolder.getContext().authentication == null) {
                if (jwtService.validateAccessToken(jwt)) {
                    val roles = jwtService.getRolesFromToken(jwt)
                    val authorities = roles.map { SimpleGrantedAuthority(it) }

                    val userDetails = User.builder()
                        .username(userId)
                        .password("")
                        .authorities(authorities)
                        .build()

                    val authToken = UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        authorities
                    )
                    authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                    SecurityContextHolder.getContext().authentication = authToken

                    filterChain.doFilter(request, response)
                } else {
                    logger.error("Invalid JWT token")
                    response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid token")
                }
            } else {
                filterChain.doFilter(request, response)
            }
        } catch (e: Exception) {
            logger.error("Cannot set user authentication: ${e.message}", e)
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Authentication failed")
        }
    }
}