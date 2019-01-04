package com.alorma.ktor.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import io.ktor.application.call
import io.ktor.auth.*
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.request.ApplicationRequest
import io.ktor.response.respond

fun Authentication.Configuration.firebase() {
    val provider = FirebaseAuthenticationProvider()

    provider.pipeline.intercept(AuthenticationPipeline.RequestAuthentication) { context ->
        val credentials = call.request.bearerAuthenticationCredentials()
        val principal = credentials?.let {
            val user = FirebaseAuth.getInstance().getUserAsync(it.uId).get()
            FirebasePrincipal(user.uid, user.displayName)
        }

        val cause = when {
            credentials == null -> AuthenticationFailedCause.NoCredentials
            principal == null -> AuthenticationFailedCause.InvalidCredentials
            else -> null
        }

        if (cause != null) {
            context.challenge(basicAuthenticationChallengeKey, cause) {
                call.respond(UnauthorizedResponse(HttpAuthHeader.basicAuthChallenge("Firebase", null)))
                it.complete()
            }
        }
        if (principal != null) {
            context.principal(principal)
        }
    }

    register(provider)
}

class FirebaseAuthenticationProvider : AuthenticationProvider(null)

/**
 * Retrieves Bearer authentication credentials for this [ApplicationRequest]
 */
fun ApplicationRequest.bearerAuthenticationCredentials(): FirebaseCredential? {
    val parsed = parseAuthorizationHeader()
    when (parsed) {
        is HttpAuthHeader.Single -> {
            // Verify the auth scheme is HTTP Basic. According to RFC 2617, the authorization scheme should not be case
            // sensitive; thus BASIC, or Basic, or basic are all valid.
            if (!parsed.authScheme.equals("Bearer", ignoreCase = true)) {
                return null
            }
            return try {
                val verifyIdToken = FirebaseAuth.getInstance().verifyIdToken(parsed.blob)
                val uId = verifyIdToken.uid
                FirebaseCredential(uId)
            } catch (e: FirebaseAuthException) {
                null
            }
        }
        else -> return null
    }
}

private val basicAuthenticationChallengeKey: Any = "BearerAuth"

data class FirebaseCredential(val uId: String) : Credential
data class FirebasePrincipal(val uId: String, val name: String?) : Principal