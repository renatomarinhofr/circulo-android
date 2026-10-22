package br.com.circulo.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreeningPolicyTest {
    private val now = 1_800_000_000_000L

    @Test fun `new installation allows calls until explicitly activated`() {
        assertDecision(CallDecision.ALLOW, ProtectionSettings())
    }

    @Test fun `active protection rejects an incoming telephone call delivered by Telecom`() {
        assertDecision(CallDecision.BLOCK, ProtectionSettings(enabled = true))
    }

    @Test fun `temporary pause allows calls`() {
        assertDecision(CallDecision.ALLOW, ProtectionSettings(enabled = true, pauseUntilMillis = now + 1))
    }

    @Test fun `pause expires at the exact deadline without relying on an alarm or open app`() {
        assertDecision(CallDecision.BLOCK, ProtectionSettings(enabled = true, pauseUntilMillis = now))
    }

    @Test fun `expired pause still allows calls if user disabled protection`() {
        assertDecision(CallDecision.ALLOW, ProtectionSettings(enabled = false, pauseUntilMillis = now - 1))
    }

    @Test fun `outgoing calls are never rejected`() {
        assertEquals(CallDecision.ALLOW, ScreeningPolicy.decide(ProtectionSettings(enabled = true), now, false, "tel"))
    }

    @Test fun `missing or non telephone handles are never rejected`() {
        listOf(null, "sip", "https").forEach { scheme ->
            assertEquals(CallDecision.ALLOW, ScreeningPolicy.decide(ProtectionSettings(enabled = true), now, true, scheme))
        }
    }

    private fun assertDecision(expected: CallDecision, settings: ProtectionSettings) {
        assertEquals(expected, ScreeningPolicy.decide(settings, now, true, "tel"))
    }
}
