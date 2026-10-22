package br.com.circulo.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.app.role.RoleManager
import android.animation.ValueAnimator
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.circulo.app.data.BlockedCall
import br.com.circulo.app.ui.CirculoApp
import br.com.circulo.app.ui.CirculoTheme

class MainActivity : ComponentActivity() {
    private val model: CirculoViewModel by viewModels()
    private val roleRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        model.onRoleResult()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val opening = installSplashScreen()
        super.onCreate(savedInstanceState)
        opening.setOnExitAnimationListener { splash ->
            if (ValueAnimator.areAnimatorsEnabled()) {
                splash.iconView.animate().scaleX(.88f).scaleY(.88f).alpha(0f).setDuration(240).start()
                splash.view.animate().alpha(0f).setDuration(280)
                    .withEndAction { splash.remove() }.start()
            } else splash.remove()
        }
        enableEdgeToEdge()
        setContent {
            val state by model.state.collectAsStateWithLifecycle()
            CirculoTheme(state.settings.appearance) { dark ->
                androidx.compose.runtime.SideEffect {
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !dark
                        isAppearanceLightNavigationBars = !dark
                    }
                }
                CirculoApp(
                    state = state,
                    model = model,
                    requestRole = ::requestScreeningRole,
                    saveContact = ::saveContact,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        model.refresh()
    }

    private fun requestScreeningRole() {
        val manager = getSystemService(RoleManager::class.java)
        if (manager?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) != true) {
            model.message("Este aparelho não oferece o filtro de chamadas do Android.")
            return
        }
        try {
            roleRequest.launch(manager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
        } catch (_: ActivityNotFoundException) {
            model.message("A tela de permissão não está disponível neste aparelho.")
        }
    }

    private fun saveContact(call: BlockedCall) {
        if (call.number.isBlank()) return
        try {
            startActivity(Intent(ContactsContract.Intents.Insert.ACTION).apply {
                type = ContactsContract.RawContacts.CONTENT_TYPE
                putExtra(ContactsContract.Intents.Insert.PHONE, call.number)
            })
        } catch (_: ActivityNotFoundException) {
            model.message("Nenhum aplicativo de contatos foi encontrado.")
        }
    }
}
