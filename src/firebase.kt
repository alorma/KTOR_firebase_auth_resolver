package com.alorma.ktor.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.UserRecord
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.auth.*
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.request.ApplicationRequest
import io.ktor.util.pipeline.PipelineContext
import java.util.concurrent.TimeUnit

val authUserPrincipals: MutableMap<String, TimedCredential> = mutableMapOf()

fun Authentication.Configuration.firebase() {
    val provider = FirebaseAuthenticationProvider()

    provider.pipeline.intercept(AuthenticationPipeline.RequestAuthentication) { context ->
        val nowProvider = SystemTimeProvider()
        val expirationProvider = ExpirationTimeProvider(nowProvider, 5, TimeUnit.MINUTES)

        val parsed = call.request.parseAuthorizationHeader()

        when (parsed) {
            is HttpAuthHeader.Single -> {
                val credential = authUserPrincipals[parsed.blob]?.takeIf {
                    it.expireDate < nowProvider.now() || it.usedTimes < Const.MAX_PRINCIPAL_TIMES
                }?.also {
                    it.usedTimes++
                }

                if (credential != null) {
                    context.principal(credential.principal)
                } else {
                    authenticateUser(context, expirationProvider, parsed.blob)
                }
            }
            else -> authError()
        }
    }

    register(provider)
}

private fun PipelineContext<AuthenticationContext, ApplicationCall>.authenticateUser(
    context: AuthenticationContext,
    expirationProvider: ExpirationTimeProvider,
    token: String
) {
    try {
        val credentials = call.request.bearerAuthenticationCredentials()
        val principal = credentials?.let {
            val user = FirebaseAuth.getInstance().getUserAsync(it.uId).get()
            FirebasePrincipal(user.uid, user.displayName, user).also { principal ->
                authUserPrincipals[token] = TimedCredential(
                    principal,
                    expirationProvider.now()
                )
            }
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

private fun authError() {
    throw FirebaseAuthenticationException()
}

class FirebaseAuthenticationProvider : AuthenticationProvider(null)

/**
 * Retrieves Bearer authentication credentials for this [ApplicationRequest]
 */
fun ApplicationRequest.bearerAuthenticationCredentials(): FirebaseCredential? =
    when (val parsed = parseAuthorizationHeader()) {
        is HttpAuthHeader.Single -> {
            // Verify the auth scheme is HTTP Basic. According to RFC 2617, the authorization scheme should not be case
            // sensitive; thus BASIC, or Basic, or basic are all valid.
            if (!parsed.authScheme.equals("Bearer", ignoreCase = true)) {
                null
            } else {
                val bearerToken = parsed.blob
                try {
                    val verifyIdToken = FirebaseAuth.getInstance().verifyIdToken(bearerToken)
                    val uId = verifyIdToken.uid
                    FirebaseCredential(uId)
                } catch (e: FirebaseAuthException) {
                    throw FirebaseAuthenticationException()
                }
            }
        }
        else -> null
    }

class TimedCredential(val principal: FirebasePrincipal, val expireDate: Long, var usedTimes: Int = 0)

private val basicAuthenticationChallengeKey: Any = "BearerAuth"

data class FirebaseCredential(
    val uId: String
) : Credential

data class FirebasePrincipal(
    val uId: String,
    val name: String?,
    val firebaseUser: UserRecord? = null
) : Principal

object Const {
    internal const val MAX_PRINCIPAL_TIMES = 10
}