package com.alorma.ktor.firebase

import io.ktor.application.Application
import io.ktor.http.HttpMethod
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import org.junit.Test
import kotlin.test.assertEquals

class CustomFeatureTest {
    @Test
    fun testCustomHeader(): Unit = withTestApplication(Application::main) {
        handleRequest(HttpMethod.Get, "/").apply {
            assertEquals("World", response.headers["Hello"])
        }
    }
}