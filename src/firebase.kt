package com.alorma.ktor.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.UserRecord
import io.ktor.application.call
import io.ktor.auth.*
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.request.ApplicationRequest
import java.util.concurrent.TimeUnit

fun Authentication.Configuration.firebase() {
    val provider = FirebaseAuthenticationProvider()

    provider.pipeline.intercept(AuthenticationPipeline.RequestAuthentication) { context ->
        try {
            val credentials = call.request.bearerAuthenticationCredentials()
            val principal = credentials?.let {
                val user = FirebaseAuth.getInstance().getUserAsync(it.uId).get()
                FirebasePrincipal(user.uid, user.displayName, user)
            }

            val cause = when {
                credentials == null -> AuthenticationFailedCause.NoCredentials
                principal == null -> AuthenticationFailedCause.InvalidCredentials
                else -> null
            }

            if (cause != null) {
                authError()
            }
            if (principal != null) {
                context.principal(principal)
            } else {
                authError()

            }
        } catch (e: Exception) {
            authError()
        }
    }

    register(provider)
}

private fun authError() {
    throw FirebaseAuthenticationException()
}

class FirebaseAuthenticationProvider : AuthenticationProvider(null)

val authUserCredential: MutableMap<String, TimedCredential> = mutableMapOf()

/**
 * Retrieves Bearer authentication credentials for this [ApplicationRequest]
 */
fun ApplicationRequest.bearerAuthenticationCredentials(): FirebaseCredential? {
    val nowProvider = SystemTimeProvider()
    val expirationProvider = ExpirationTimeProvider(nowProvider, 5, TimeUnit.MINUTES)

    val parsed = parseAuthorizationHeader()
    when (parsed) {
        is HttpAuthHeader.Single -> {
            // Verify the auth scheme is HTTP Basic. According to RFC 2617, the authorization scheme should not be case
            // sensitive; thus BASIC, or Basic, or basic are all valid.
            if (!parsed.authScheme.equals("Bearer", ignoreCase = true)) {
                return null
            }

            val bearerToken = parsed.blob

            return authUserCredential[bearerToken]?.takeIf {
                it.expireDate < nowProvider.now()
            }?.credential ?: try {
                val verifyIdToken = FirebaseAuth.getInstance().verifyIdToken(bearerToken)
                val uId = verifyIdToken.uid
                val firebaseCredential = FirebaseCredential(uId)
                    .also { credential: FirebaseCredential ->
                        authUserCredential[bearerToken] = TimedCredential(
                            credential,
                            expirationProvider.now()
                        )
                    }
                firebaseCredential
            } catch (e: FirebaseAuthException) {
                throw FirebaseAuthenticationException()
            }
        }
        else -> return null
    }
}

class TimedCredential(val credential: FirebaseCredential, val expireDate: Long)

private val basicAuthenticationChallengeKey: Any = "BearerAuth"

data class FirebaseCredential(
    val uId: String
) : Credential

data class FirebasePrincipal(
    val uId: String,
    val name: String?,
    val firebaseUser: UserRecord? = null
) : Principal