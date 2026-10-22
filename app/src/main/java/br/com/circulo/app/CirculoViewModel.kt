package br.com.circulo.app

import android.app.Application
import android.app.role.RoleManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.circulo.app.data.BlockedCall
import br.com.circulo.app.domain.ProtectionSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CirculoState(
    val settings: ProtectionSettings = ProtectionSettings(),
    val roleAvailable: Boolean = false,
    val roleHeld: Boolean = false,
    val calls: List<BlockedCall> = emptyList(),
    val historyLoading: Boolean = true,
    val historyError: Boolean = false,
    val busy: Boolean = false,
)

class CirculoViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as CirculoApplication
    private val mutableState = MutableStateFlow(CirculoState(settings = app.preferences.current()))
    val state = mutableState.asStateFlow()
    private val messageChannel = Channel<String>(Channel.BUFFERED)
    val messages = messageChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            app.preferences.settings.collect { settings ->
                mutableState.update { it.copy(settings = settings) }
            }
        }
        viewModelScope.launch {
            app.history.revision.drop(1).collect { refresh() }
        }
        refresh()
    }

    fun refresh() {
        val roleManager = app.getSystemService(RoleManager::class.java)
        mutableState.update { it.copy(
            roleAvailable = roleManager?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) == true,
            roleHeld = roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true,
        ) }
        viewModelScope.launch {
            try {
                val calls = withContext(app.historyDispatcher) { app.history.recent(System.currentTimeMillis()) }
                mutableState.update { it.copy(calls = calls, historyLoading = false, historyError = false) }
            } catch (_: RuntimeException) {
                mutableState.update { it.copy(historyLoading = false, historyError = true) }
            }
        }
    }

    fun onRoleResult() {
        val manager = app.getSystemService(RoleManager::class.java)
        if (manager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true) {
            changeSettings("Proteção ativada. Seu círculo está pronto.") {
                it.copy(enabled = true, pauseUntilMillis = 0)
            }
        } else {
            message("A permissão não foi concedida. Você pode tentar novamente.")
        }
        refresh()
    }

    fun enable() = changeSettings("Proteção ativada.") { it.copy(enabled = true, pauseUntilMillis = 0) }
    fun disable() = changeSettings("Proteção desativada. Todas as chamadas podem tocar.") {
        it.copy(enabled = false, pauseUntilMillis = 0)
    }
    fun pause(minutes: Int) = changeSettings("Pausa iniciada. A proteção volta automaticamente.") {
        it.copy(enabled = true, pauseUntilMillis = System.currentTimeMillis() + minutes * 60_000L)
    }

    fun changeSettings(success: String? = null, transform: (ProtectionSettings) -> ProtectionSettings) {
        if (mutableState.value.busy) return
        mutableState.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { app.preferences.update(transform) }
                success?.let { messageChannel.send(it) }
            } catch (_: RuntimeException) {
                messageChannel.send("Não foi possível salvar. Tente novamente.")
            } finally {
                mutableState.update { it.copy(busy = false) }
            }
        }
    }

    fun clearHistory() {
        if (mutableState.value.busy) return
        mutableState.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                withContext(app.historyDispatcher) { app.history.clear() }
                mutableState.update { it.copy(calls = emptyList(), historyError = false) }
                messageChannel.send("Histórico apagado deste aparelho.")
            } catch (_: RuntimeException) {
                messageChannel.send("Não foi possível apagar o histórico. Tente novamente.")
            } finally {
                mutableState.update { it.copy(busy = false) }
            }
        }
    }

    fun message(text: String) { viewModelScope.launch { messageChannel.send(text) } }
}
