package dev.dwidi.springbootkotlinjwt

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan("dev.dwidi.springbootkotlinjwt.jwtconfig")
class SpringbootKotlinJwtApplication

fun main(args: Array<String>) {
    runApplication<SpringbootKotlinJwtApplication>(*args)
}
