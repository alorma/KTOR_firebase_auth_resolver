/*
package com.alorma.ktor.firebase

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.main(testing: Boolean = false) {
    install(FirebaseConfig) {
        fileName = "resources/app_service_account.json"
    }

    install(Authentication) {
        firebase()
    }
    routing {
        authenticate {
            get("/authenticated") {
                call.respond("OK")
            }
        }
        get("/noAuthenticated") {
            call.respond("OK")
        }
    }
}
*/
