package br.com.circulo.app

import android.app.Application
import br.com.circulo.app.data.BlockedCallStore
import br.com.circulo.app.data.ProtectionPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob

class CirculoApplication : Application() {
    // One process-wide queue keeps history reads, writes and erasure ordered.
    @OptIn(ExperimentalCoroutinesApi::class)
    val historyDispatcher = Dispatchers.IO.limitedParallelism(1)
    val historyScope = CoroutineScope(SupervisorJob() + historyDispatcher)
    val preferences by lazy { ProtectionPreferences(this) }
    val history by lazy { BlockedCallStore(this) }
}
