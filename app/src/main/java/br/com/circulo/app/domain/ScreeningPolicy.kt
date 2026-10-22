package br.com.circulo.app.domain

data class ProtectionSettings(
    val enabled: Boolean = false,
    val pauseUntilMillis: Long = 0L,
    val keepHistory: Boolean = true,
    val haptics: Boolean = true,
    val appearance: Appearance = Appearance.SYSTEM,
) {
    fun isBlockingAt(nowMillis: Long): Boolean = enabled && pauseUntilMillis <= nowMillis
    fun isPausedAt(nowMillis: Long): Boolean = enabled && pauseUntilMillis > nowMillis
}

enum class Appearance { SYSTEM, LIGHT, DARK }
enum class CallDecision { ALLOW, BLOCK }

/**
 * Input is a call delivered by Android's CallScreeningService. Without READ_CONTACTS,
 * Telecom only delivers numbers it does not recognize in the user's contacts.
 * Keep that manifest invariant: adding READ_CONTACTS would require a new policy.
 */
object ScreeningPolicy {
    fun decide(
        settings: ProtectionSettings,
        nowMillis: Long,
        incoming: Boolean,
        scheme: String?,
    ): CallDecision = if (incoming && scheme == "tel" && settings.isBlockingAt(nowMillis)) {
        CallDecision.BLOCK
    } else {
        CallDecision.ALLOW
    }
}
