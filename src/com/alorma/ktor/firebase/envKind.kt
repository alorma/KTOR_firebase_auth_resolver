package com.alorma.ktor.firebase

import io.ktor.application.Application

private val Application.envKind get() = environment.config.property("ktor.environment").getString()

val Application.env: Environment?
    get() = when (envKind) {
        "dev" -> Environment.DEV
        "stage" -> Environment.STAGE
        "pro" -> Environment.PRO
        else -> null
    }


enum class Environment(val envName: kotlin.String) {
    DEV("dev"),
    STAGE("stage"),
    PRO("pro")
}