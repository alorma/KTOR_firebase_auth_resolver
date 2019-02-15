package com.alorma.ktor.firebase

import io.ktor.application.Application

class EndpointDataSource(private val application: Application) {

    fun getAuthUrl(): String =
        application
            .environment
            .config.property("envs.endpoints.${application.env?.envName}.auth")
            .getString()

}