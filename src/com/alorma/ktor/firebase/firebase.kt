package com.alorma.ktor.firebase

import com.google.gson.Gson
import io.ktor.application.call
import io.ktor.auth.Authentication
import io.ktor.auth.AuthenticationPipeline
import io.ktor.auth.AuthenticationProvider
import io.ktor.auth.Principal
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.request.header
import java.net.URL

fun Authentication.Configuration.firebaseAuth(
    httpClient: HttpClient,
    auth: String
) {
    val provider = AuthenticationProvider(null)

    provider.pipeline.intercept(AuthenticationPipeline.RequestAuthentication) { context ->
        val authUrl = URL(auth.plus("authenticate"))
        val response = httpClient.get<String>(authUrl) {
            headers {
                val name = "Authorization"
                call.request.header(name)?.let { value ->
                    set(name, value)
                }
            }
        }

        val authResponse = Gson().fromJson(response, AuthResponse::class.java)

        val authPrincipal = AuthPrincipal(
            authResponse.uid,
            authResponse.name,
            authResponse.email,
            authResponse.phone,
            authResponse.avatar,
            authResponse.disabled
        )
        context.principal(authPrincipal)
    }

    register(provider)
}

data class AuthPrincipal(
    val uId: String,
    val name: String?,
    val email: String?,
    val phone: String?,
    val avatar: String?,
    val disabled: Boolean = false
) : Principal

data class AuthResponse(
    val uid: String,
    val name: String?,
    val email: String?,
    val phone: String?,
    val avatar: String?,
    val disabled: Boolean = false
)
