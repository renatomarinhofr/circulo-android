package br.com.circulo.app.screening

import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import br.com.circulo.app.CirculoApplication
import br.com.circulo.app.domain.CallDecision
import br.com.circulo.app.domain.ScreeningPolicy
import kotlinx.coroutines.launch

class CirculoScreeningService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) return

        val app = application as CirculoApplication
        val now = System.currentTimeMillis()
        val settings = app.preferences.current()
        val blocked = ScreeningPolicy.decide(
            settings = settings,
            nowMillis = now,
            incoming = true,
            scheme = callDetails.handle?.scheme,
        ) == CallDecision.BLOCK

        // Respond before history I/O. Telecom requires a response within five seconds.
        respondToCall(callDetails, CallResponse.Builder()
            .setDisallowCall(blocked)
            .setRejectCall(blocked)
            .setSkipCallLog(false)
            .setSkipNotification(blocked)
            .build())

        if (blocked && settings.keepHistory) {
            val number = callDetails.handle?.schemeSpecificPart.orEmpty()
            app.historyScope.launch {
                try {
                    app.history.record(number, now)
                } catch (_: RuntimeException) {
                    // Never log numbers. Process death can also drop this best-effort entry.
                    Log.w("Circulo", "Não foi possível guardar um bloqueio no histórico local.")
                }
            }
        }
    }
}
