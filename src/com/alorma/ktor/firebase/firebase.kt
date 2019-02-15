package com.alorma.ktor.firebase

import com.alorma.ktor.firebase.model.AuthResponse
import io.ktor.application.call
import io.ktor.auth.Authentication
import io.ktor.auth.AuthenticationPipeline
import io.ktor.auth.AuthenticationProvider
import io.ktor.auth.Principal
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.url
import io.ktor.util.flattenForEach
import java.net.URL

fun Authentication.Configuration.firebase(
    httpClient: HttpClient
) {
    AuthenticationProvider(null).pipeline.intercept(
        AuthenticationPipeline.RequestAuthentication
    ) { context ->
        val authUrl = EndpointDataSource(call.application).getAuthUrl()

        val response = httpClient.get<AuthResponse> {
            url(URL(authUrl))
            headers {
                call.request.headers.flattenForEach { key, value ->
                    set(key, value)
                }
            }
        }

        val authPrincipal = AuthPrincipal(response.uId, response)
        context.principal(authPrincipal)
    }
}

class AuthPrincipal(
    val uId: String,
    val authResponse: AuthResponse
) : Principal
