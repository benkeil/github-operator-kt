package de.benkeil.support

import io.mockk.MockKMatcherScope
import io.mockk.MockKStubScope
import io.mockk.MockKVerificationScope
import io.mockk.Ordering
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.runBlocking

fun <T> everySuspending(stubBlock: suspend MockKMatcherScope.() -> T): MockKStubScope<T, T> =
    every {
      runBlocking { stubBlock() }
    }

fun verifySuspending(
    ordering: Ordering = Ordering.UNORDERED,
    inverse: Boolean = false,
    atLeast: Int = 1,
    atMost: Int = Int.MAX_VALUE,
    exactly: Int = -1,
    timeout: Long = 0,
    verifyBlock: suspend MockKVerificationScope.() -> Unit
) =
    verify(
        ordering = ordering,
        inverse = inverse,
        atLeast = atLeast,
        atMost = atMost,
        exactly = exactly,
        timeout = timeout,
    ) {
      runBlocking { verifyBlock() }
    }
