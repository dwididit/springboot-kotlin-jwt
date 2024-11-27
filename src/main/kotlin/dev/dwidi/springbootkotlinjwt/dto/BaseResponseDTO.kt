package dev.dwidi.springbootkotlinjwt.dto

import org.springframework.http.HttpStatus

class BaseResponseDTO<T> (
    val statusCode: Int,
    val message: String,
    val data: T? = null
) {
    companion object {
        fun<T> success(data: T, message: String = "Success"): BaseResponseDTO<T> {
            return BaseResponseDTO(
                statusCode = HttpStatus.OK.value(),
                message = message,
                data = data
            )
        }

        fun<T> error(statusCode: Int, message: String): BaseResponseDTO<T> {
            return BaseResponseDTO(
                statusCode = statusCode,
                message = message,
                data = null
            )
        }
    }
}