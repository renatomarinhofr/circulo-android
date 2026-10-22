package br.com.circulo.app.data

import android.content.Context
import android.annotation.SuppressLint
import br.com.circulo.app.domain.Appearance
import br.com.circulo.app.domain.ProtectionSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProtectionPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("protection", Context.MODE_PRIVATE)
    private val mutableSettings = MutableStateFlow(read())
    val settings = mutableSettings.asStateFlow()

    fun current(): ProtectionSettings = mutableSettings.value

    @Synchronized
    @SuppressLint("UseKtx") // The KTX helper discards commit's boolean; we must detect a failed save.
    fun update(transform: (ProtectionSettings) -> ProtectionSettings) {
        val next = transform(current())
        // Commit before announcing success, so restarting cannot restore an old state.
        check(preferences.edit()
            .putBoolean("enabled", next.enabled)
            .putLong("pause_until", next.pauseUntilMillis)
            .putBoolean("keep_history", next.keepHistory)
            .putBoolean("haptics", next.haptics)
            .putString("appearance", next.appearance.name)
            .commit()) { "Não foi possível salvar esta alteração. Tente novamente." }
        mutableSettings.value = next
    }

    private fun read() = ProtectionSettings(
        enabled = preferences.getBoolean("enabled", false),
        pauseUntilMillis = preferences.getLong("pause_until", 0L),
        keepHistory = preferences.getBoolean("keep_history", true),
        haptics = preferences.getBoolean("haptics", true),
        appearance = Appearance.entries.firstOrNull {
            it.name == preferences.getString("appearance", Appearance.SYSTEM.name)
        } ?: Appearance.SYSTEM,
    )
}
