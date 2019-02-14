package com.alorma.ktor.firebase

import java.util.concurrent.TimeUnit

internal interface TimeProvider {
    fun now(): Long
}

internal class SystemTimeProvider : TimeProvider {
    override fun now() = System.currentTimeMillis()
}

internal class ExpirationTimeProvider(
    private val current: TimeProvider,
    private val timeValue: Long,
    private val timeUnit: TimeUnit
) : TimeProvider {
    override fun now() = current.now() + timeUnit.toMillis(timeValue)
}