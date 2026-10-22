package br.com.circulo.app.ui

import android.telephony.PhoneNumberUtils
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.circulo.app.CirculoState
import br.com.circulo.app.CirculoViewModel
import br.com.circulo.app.data.BlockedCall
import br.com.circulo.app.domain.Appearance
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class Destination(val label: String, val icon: ImageVector) {
    HOME("Proteção", CirculoIcons.Shield),
    HISTORY("Histórico", CirculoIcons.History),
    SETTINGS("Ajustes", CirculoIcons.Settings),
}
private enum class Sheet { SETUP, PAUSE, LIMITS, APPEARANCE, PRIVACY }
private enum class ProtectionStatus(val label: String) {
    ACTIVE("Ativa"), PAUSED("Pausada"), OFF("Desativada"), NEEDS_ROLE("Configurar"), UNSUPPORTED("Indisponível")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CirculoApp(state: CirculoState, model: CirculoViewModel, requestRole: () -> Unit, saveContact: (BlockedCall) -> Unit) {
    var destination by rememberSaveable { mutableStateOf(Destination.HOME) }
    var sheet by rememberSaveable { mutableStateOf<Sheet?>(null) }
    var clearDialog by rememberSaveable { mutableStateOf(false) }
    var selectedCallId by rememberSaveable { mutableStateOf<Long?>(null) }
    val selectedCall = state.calls.firstOrNull { it.id == selectedCallId }
    val savedDestinations = rememberSaveableStateHolder()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val snackbar = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    val wide = LocalConfiguration.current.screenWidthDp >= 600

    LaunchedEffect(model) { model.messages.collect { snackbar.showSnackbar(it) } }
    LaunchedEffect(state.settings.pauseUntilMillis) {
        while (true) {
            now = System.currentTimeMillis()
            delay(if (state.settings.pauseUntilMillis > now) 1_000 else 60_000)
        }
    }
    LaunchedEffect(destination) { if (destination == Destination.HISTORY) model.refresh() }
    BackHandler(enabled = destination != Destination.HOME && sheet == null && selectedCall == null && !clearDialog) {
        destination = Destination.HOME
    }
    val status = when {
        !state.roleAvailable -> ProtectionStatus.UNSUPPORTED
        !state.roleHeld -> ProtectionStatus.NEEDS_ROLE
        !state.settings.enabled -> ProtectionStatus.OFF
        state.settings.isPausedAt(now) -> ProtectionStatus.PAUSED
        else -> ProtectionStatus.ACTIVE
    }
    val feedback: (() -> Unit) -> Unit = { action ->
        if (state.settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        action()
    }
    val activate = {
        feedback {
            if (state.roleHeld) model.enable()
            else sheet = if (state.roleAvailable) Sheet.SETUP else Sheet.LIMITS
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BrandMark(Modifier.size(29.dp))
                        Text("círculo", style = MaterialTheme.typography.headlineMedium)
                    }
                },
                actions = {
                    IconButton(onClick = { sheet = Sheet.LIMITS },
                        modifier = Modifier.padding(end = 12.dp).size(48.dp)
                            .clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainer)) {
                        Icon(CirculoIcons.Info, "Como o bloqueio funciona", Modifier.size(22.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (!wide) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f))
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                        Destination.entries.forEach { item ->
                            val iconScale by animateFloatAsState(if (destination == item) 1.06f else 1f,
                                tween(220), label = "Destino selecionado")
                            NavigationBarItem(
                                selected = destination == item,
                                onClick = { destination = item },
                                icon = { Icon(item.icon, null, Modifier.size(23.dp).graphicsLayer { scaleX = iconScale; scaleY = iconScale }) },
                                label = { Text(item.label) },
                                colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface),
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        Row(Modifier.fillMaxSize().padding(padding)) {
            if (wide) {
                NavigationRail(containerColor = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxHeight()) {
                    Spacer(Modifier.height(32.dp))
                    Destination.entries.forEach { item ->
                        NavigationRailItem(
                            selected = destination == item, onClick = { destination = item },
                            icon = { Icon(item.icon, null) }, label = { Text(item.label) },
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                AnimatedContent(
                    targetState = destination,
                    modifier = Modifier.widthIn(max = 680.dp).fillMaxSize(),
                    transitionSpec = {
                        val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                        (fadeIn(tween(240, 40)) + slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { direction * it / 20 }) togetherWith
                            (fadeOut(tween(120)) + slideOutHorizontally(tween(180)) { -direction * it / 28 })
                    },
                    label = "Navegação",
                ) { current ->
                    savedDestinations.SaveableStateProvider(current.name) {
                    when (current) {
                        Destination.HOME -> HomeScreen(state, status, now, activate,
                            pause = { feedback { sheet = Sheet.PAUSE } },
                            history = { destination = Destination.HISTORY },
                            limits = { sheet = Sheet.LIMITS },
                            detail = { selectedCallId = it.id },
                            retry = model::refresh,
                        )
                        Destination.HISTORY -> HistoryScreen(state, now, { selectedCallId = it.id }, { clearDialog = true }, model::refresh)
                        Destination.SETTINGS -> SettingsScreen(state, status,
                            activate = activate,
                            pause = { sheet = Sheet.PAUSE },
                            appearance = { sheet = Sheet.APPEARANCE },
                            haptics = { value -> model.changeSettings { it.copy(haptics = value) } },
                            keepHistory = { value -> model.changeSettings { it.copy(keepHistory = value) } },
                            clear = { clearDialog = true }, privacy = { sheet = Sheet.PRIVACY }, limits = { sheet = Sheet.LIMITS },
                        )
                    }
                    }
                }
            }
        }
    }

    sheet?.let { current ->
        ModalBottomSheet(onDismissRequest = { sheet = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface, dragHandle = { BottomSheetDefaults.DragHandle() }) {
            Column(Modifier.fillMaxWidth().heightIn(max = (LocalConfiguration.current.screenHeightDp * .85f).dp)
                .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                when (current) {
                    Sheet.SETUP -> {
                        SheetTitle("Seu círculo,\nsuas regras.")
                        Text("Escolha o Círculo como app de identificação e bloqueio na próxima tela do Android.", style = MaterialTheme.typography.bodyLarge)
                        ExplanationRow(CirculoIcons.Contacts, "Na sua agenda? Pode tocar.", "O Android reconhece seus contatos automaticamente.")
                        ExplanationRow(CirculoIcons.PhoneOff, "Fora dela? Fica em silêncio.", "As chamadas identificáveis são rejeitadas antes de tocar.")
                        Notice("Números ocultos e ligações de aplicativos não são filtrados.")
                        PrimaryAction("Permitir no Android", CirculoIcons.Arrow, state.busy) { sheet = null; requestRole() }
                        Text("Sem acesso à agenda. Sem cadastro.", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                    Sheet.PAUSE -> {
                        SheetTitle("Precisa deixar\no telefone tocar?")
                        Text("Durante a pausa, números fora da agenda também podem ligar. Depois, a proteção volta sozinha.", style = MaterialTheme.typography.bodyLarge)
                        listOf(15 to "Pausar por 15 minutos", 60 to "Pausar por 1 hora", 480 to "Pausar por 8 horas").forEach { (minutes, title) ->
                            OutlinedButton(onClick = { sheet = null; feedback { model.pause(minutes) } }, enabled = !state.busy,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), shape = RoundedCornerShape(14.dp)) {
                                Text(title, modifier = Modifier.weight(1f)); Icon(CirculoIcons.Pause, null, Modifier.size(18.dp))
                            }
                        }
                        TextButton(onClick = { sheet = null; model.disable() }, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text("Desativar até eu ligar de novo", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Sheet.LIMITS -> {
                        SheetTitle("O que entra\nno seu círculo.")
                        ExplanationRow(CirculoIcons.Contacts, "Seus contatos continuam ligando", "A correspondência dos números é feita pelo Android e pela sua agenda atual.")
                        ExplanationRow(CirculoIcons.PhoneOff, "Números fora da agenda são bloqueados", "O filtro funciona nas chamadas telefônicas identificáveis que o Android entrega ao Círculo.")
                        ExplanationRow(CirculoIcons.Info, "Algumas chamadas ficam fora do filtro", "Números ocultos, restritos ou indisponíveis e chamadas de WhatsApp e outros apps não são entregues a este serviço.")
                        Text("Chamadas de saída continuam funcionando normalmente. Se estiver esperando um serviço ou entrega, use a pausa temporária.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!state.roleAvailable) Notice("O filtro de chamadas não está disponível neste aparelho. É necessário Android 10 ou superior e suporte do sistema.")
                        PrimaryAction("Entendi", CirculoIcons.Check, false) { sheet = null }
                    }
                    Sheet.APPEARANCE -> {
                        SheetTitle("A luz do seu jeito.")
                        Appearance.entries.forEach { appearance ->
                            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected = state.settings.appearance == appearance,
                                role = Role.RadioButton, enabled = !state.busy,
                                onClick = { model.changeSettings { it.copy(appearance = appearance) }; sheet = null })
                                .padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = state.settings.appearance == appearance, onClick = null)
                                Text(appearance.label(), modifier = Modifier.padding(start = 12.dp), style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                    Sheet.PRIVACY -> {
                        SheetTitle("Só no seu aparelho.")
                        Text("O Círculo funciona sem internet e sem conta. Sua agenda não é lida nem copiada pelo aplicativo.", style = MaterialTheme.typography.bodyLarge)
                        Text("Se o histórico estiver ligado, exibimos o número e o horário de até 200 bloqueios dos últimos 30 dias. Registros antigos são removidos ao abrir o app ou registrar um novo bloqueio. Você pode apagar tudo quando quiser ou deixar de guardar os próximos.", style = MaterialTheme.typography.bodyLarge)
                        Text("O histórico do próprio Android é independente e pode continuar mostrando as chamadas bloqueadas. Os dados do Círculo não entram no backup do app.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        PrimaryAction("Entendi", CirculoIcons.Check, false) { sheet = null }
                    }
                }
            }
        }
    }

    selectedCall?.let { call ->
        ModalBottomSheet(onDismissRequest = { selectedCallId = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().heightIn(max = (LocalConfiguration.current.screenHeightDp * .85f).dp)
                .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Icon(CirculoIcons.PhoneOff, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                SelectionContainer { Text(formatNumber(call.number), style = MaterialTheme.typography.headlineMedium) }
                Text(fullDate(call.timestamp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider()
                Text("Bloqueada por estar fora da agenda no momento da chamada.", style = MaterialTheme.typography.bodyLarge)
                Text("Se você conhece este número, salve-o na agenda para permitir as próximas ligações. Confirme o salvamento no app de contatos.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                PrimaryAction("Salvar na agenda", CirculoIcons.Contacts, false, enabled = call.number.isNotBlank()) {
                    selectedCallId = null; saveContact(call)
                }
            }
        }
    }

    if (clearDialog) AlertDialog(
        onDismissRequest = { clearDialog = false },
        title = { Text("Apagar o histórico?") },
        text = { Text("Os registros do Círculo serão apagados deste aparelho. O histórico do app Telefone permanece separado.") },
        confirmButton = { TextButton(onClick = { clearDialog = false; model.clearHistory() }, enabled = !state.busy) { Text("Apagar", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { clearDialog = false }) { Text("Manter") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeScreen(state: CirculoState, status: ProtectionStatus, now: Long, activate: () -> Unit, pause: () -> Unit,
    history: () -> Unit, limits: () -> Unit, detail: (BlockedCall) -> Unit, retry: () -> Unit) {
    val active = status == ProtectionStatus.ACTIVE
    val paused = status == ProtectionStatus.PAUSED
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 28.dp)) {
        item {
            Text(when (status) {
                ProtectionStatus.ACTIVE -> "Seu tempo,\nde volta."
                ProtectionStatus.PAUSED -> "A pausa\nfaz parte."
                else -> "Sua paz\nvem primeiro."
            }, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(10.dp))
            Text("Menos interrupções. Mais espaço para você.", style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
        }
        item {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.fillMaxWidth().animateContentSize()) {
                Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    StatusLabel(status, onDark = true)
                    ProtectionDial(status)
                    Text(when (status) {
                        ProtectionStatus.ACTIVE -> "Seu círculo está ativo."
                        ProtectionStatus.PAUSED -> "Uma pausa, com retorno."
                        ProtectionStatus.UNSUPPORTED -> "Filtro indisponível."
                        ProtectionStatus.OFF -> "Proteção desligada."
                        else -> "Seu círculo está pronto."
                    }, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { heading() })
                    Spacer(Modifier.height(8.dp))
                    Text(when (status) {
                        ProtectionStatus.ACTIVE -> "O filtro permite sua agenda e bloqueia os demais números identificáveis."
                        ProtectionStatus.PAUSED -> {
                            val minutes = ((state.settings.pauseUntilMillis - now).coerceAtLeast(0) + 59_999) / 60_000
                            "Todas as chamadas podem tocar. Volta em ${if (minutes >= 60) "${minutes / 60}h ${minutes % 60}min" else "${minutes}min"}."
                        }
                        ProtectionStatus.UNSUPPORTED -> "Este aparelho não oferece o filtro de chamadas necessário."
                        else -> "Ative para receber ligações de quem está na sua agenda."
                    }, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = .78f))
                    Spacer(Modifier.height(22.dp))
                    PrimaryAction(when {
                        active -> "Pausar proteção"
                        paused -> "Retomar proteção"
                        status == ProtectionStatus.UNSUPPORTED -> "Entender os limites"
                        state.settings.enabled && !state.roleHeld -> "Restaurar proteção"
                        else -> "Ativar meu círculo"
                    }, if (active) CirculoIcons.Pause else CirculoIcons.Arrow, state.busy, onDark = true,
                        onClick = if (active) pause else if (status == ProtectionStatus.UNSUPPORTED) limits else activate)
                }
            }
            Spacer(Modifier.height(22.dp))
        }
        item {
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Últimos bloqueios", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp).semantics { heading() })
                TextButton(onClick = history) { Text("Ver todos"); Spacer(Modifier.width(6.dp)); Icon(CirculoIcons.Arrow, null, Modifier.size(16.dp)) }
            }
            if (!state.settings.keepHistory) Text("Histórico pausado. Novos bloqueios não são guardados.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        when {
            state.historyLoading -> item { HistoryLoading() }
            state.historyError -> item { HistoryError(retry) }
            state.calls.isEmpty() -> item { EmptyHistory(compact = true) }
            else -> items(state.calls.take(3), key = { it.id }) { call -> CallRow(call, Modifier.animateItem()) { detail(call) } }
        }
        item {
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(CirculoIcons.Lock, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Só no seu aparelho. Sempre no seu controle.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ProtectionDial(status: ProtectionStatus) {
    val active = status == ProtectionStatus.ACTIVE
    val paused = status == ProtectionStatus.PAUSED
    val progress by animateFloatAsState(if (active) 1f else 0f, tween(650, easing = FastOutSlowInEasing), label = "Fechar círculo")
    val foreground = MaterialTheme.colorScheme.onSecondaryContainer
    val accent by animateColorAsState(if (paused) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.inversePrimary,
        tween(450), label = "Estado do círculo")
    val ring = if (paused) foreground else accent
    Box(Modifier.fillMaxWidth().height(176.dp).clearAndSetSemantics { contentDescription = "Proteção ${status.label.lowercase(Locale.getDefault())}" }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(152.dp)) {
            val radius = size.minDimension / 2
            drawCircle(foreground.copy(alpha = .10f), radius - 1.dp.toPx(), style = Stroke(1.dp.toPx()))
            val inset = 13.dp.toPx()
            drawCircle(foreground.copy(alpha = .07f), radius - 24.dp.toPx())
            drawArc(ring, -58f - progress * 32f, 282f + 78f * progress, false, Offset(inset, inset),
                Size(size.width - inset * 2, size.height - inset * 2), style = Stroke(2.4.dp.toPx(), cap = StrokeCap.Round))
            drawCircle(ring, 3.2.dp.toPx(), Offset(size.width * .78f, size.height * .20f))
            drawCircle(foreground.copy(alpha = .35f), 2.dp.toPx(), Offset(size.width * .07f, size.height * .72f))
        }
        Icon(if (active) CirculoIcons.Check else if (paused) CirculoIcons.Pause else CirculoIcons.Power, null,
            Modifier.size(38.dp).graphicsLayer { scaleX = 1f + progress * 0.12f; scaleY = scaleX }, tint = foreground)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryScreen(state: CirculoState, now: Long, detail: (BlockedCall) -> Unit, clear: () -> Unit, retry: () -> Unit) {
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val today = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()
    val visibleCalls = state.calls.filter { call ->
        val date = Instant.ofEpochMilli(call.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
        when (filter) { 1 -> date == today; 2 -> !date.isBefore(today.minusDays(6)); else -> true }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 22.dp)) {
        item {
            Text("Histórico", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(12.dp))
            Text("Um registro do que ficou em silêncio.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("Todas", "Hoje", "7 dias").forEachIndexed { index, label ->
                    FilterChip(selected = filter == index, onClick = { filter = index }, label = { Text(label) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary))
                }
            }
            Spacer(Modifier.height(12.dp))
            if (!state.settings.keepHistory) Notice("O histórico está pausado. Os bloqueios continuam, mas os próximos não serão guardados.")
        }
        when {
            state.historyLoading -> item { HistoryLoading() }
            state.historyError -> item { HistoryError(retry) }
            visibleCalls.isEmpty() -> item { EmptyHistory(compact = false, filtered = filter != 0) }
            else -> {
                val grouped = visibleCalls.groupBy { Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate() }
                grouped.forEach { (date, calls) ->
                    item(key = date.toString()) {
                        Text(dayLabel(date, today), modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
                            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    items(calls, key = { it.id }) { call -> CallRow(call, Modifier.animateItem()) { detail(call) } }
                }
            }
        }
        item {
            Spacer(Modifier.height(24.dp))
            Text("Até 200 registros dos últimos 30 dias. O histórico do Android é independente.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (state.calls.isNotEmpty()) TextButton(onClick = clear, enabled = !state.busy, modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(CirculoIcons.Trash, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Apagar histórico")
            }
        }
    }
}

@Composable
private fun SettingsScreen(state: CirculoState, status: ProtectionStatus, activate: () -> Unit, pause: () -> Unit,
    appearance: () -> Unit, haptics: (Boolean) -> Unit, keepHistory: (Boolean) -> Unit, clear: () -> Unit, privacy: () -> Unit, limits: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 22.dp)) {
        item {
            Text("Do seu jeito.", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(12.dp))
            Text("Ajuste o Círculo ao seu ritmo.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            SettingsGroup("Proteção") {
                SettingsRow(CirculoIcons.Shield, "Filtro de chamadas", status.label,
                    onClick = if (status == ProtectionStatus.ACTIVE) pause else activate)
                SettingsRow(CirculoIcons.Info, "Como funciona", "Conheça o alcance do filtro.", showDivider = false, onClick = limits)
            }
            Spacer(Modifier.height(22.dp))
            SettingsGroup("Experiência") {
                SettingsRow(CirculoIcons.Moon, "Aparência", state.settings.appearance.label(), onClick = appearance)
                SettingsToggle(CirculoIcons.Touch, "Resposta ao toque", "Confirmações suaves nas ações principais.", state.settings.haptics, !state.busy, haptics, showDivider = false)
            }
            Spacer(Modifier.height(22.dp))
            SettingsGroup("Privacidade e dados") {
                SettingsToggle(CirculoIcons.History, "Guardar histórico", "Até 200 bloqueios dos últimos 30 dias. Desligar mantém os registros existentes.", state.settings.keepHistory, !state.busy, keepHistory)
                SettingsRow(CirculoIcons.Trash, "Apagar histórico", "Remover os registros do Círculo.", enabled = state.calls.isNotEmpty() && !state.busy, onClick = clear)
                SettingsRow(CirculoIcons.Lock, "Seus dados", "Tudo fica neste aparelho.", showDivider = false, onClick = privacy)
            }
            Spacer(Modifier.height(32.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BrandMark(Modifier.size(20.dp))
                Text("Círculo 1.1.0", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(6.dp))
            Text("Menos interrupções. Mais presença.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BrandMark(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurface
    Canvas(modifier) {
        drawArc(color, -62f, 304f, false, Offset(2.dp.toPx(), 2.dp.toPx()), Size(size.width - 4.dp.toPx(), size.height - 4.dp.toPx()), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(color, 2.dp.toPx(), Offset(size.width * .75f, size.height * .21f))
    }
}

@Composable
private fun StatusLabel(status: ProtectionStatus, onDark: Boolean = false) {
    val active = status == ProtectionStatus.ACTIVE
    val color = if (onDark) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Row(Modifier.clip(RoundedCornerShape(50)).background(if (onDark) color.copy(alpha = .12f) else MaterialTheme.colorScheme.surfaceContainer)
        .padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (active) Icon(CirculoIcons.Check, null, Modifier.size(13.dp), tint = color)
        else Box(Modifier.size(5.dp).clip(CircleShape).background(color))
        Text(status.label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
private fun PrimaryAction(text: String, icon: ImageVector, busy: Boolean, enabled: Boolean = true, onDark: Boolean = false, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) .98f else 1f,
        tween(140, easing = FastOutSlowInEasing), label = "Resposta ao pressionar")
    Button(onClick = onClick, enabled = enabled && !busy, shape = RoundedCornerShape(12.dp),
        interactionSource = interaction,
        colors = if (onDark) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.inversePrimary,
            contentColor = MaterialTheme.colorScheme.secondaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.inversePrimary.copy(alpha = .35f),
            disabledContentColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = .6f)) else ButtonDefaults.buttonColors(),
        modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp).graphicsLayer { scaleX = pressScale; scaleY = pressScale },
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 15.dp)) {
        Text(if (busy) "Salvando…" else text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        if (busy) CircularProgressIndicator(Modifier.size(19.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
        else Icon(icon, null, Modifier.size(21.dp))
    }
}

@Composable
private fun SheetTitle(text: String) = Text(text, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })

@Composable
private fun ExplanationRow(icon: ImageVector, title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icon, null, Modifier.padding(top = 3.dp).size(23.dp), tint = MaterialTheme.colorScheme.primary)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Notice(text: String) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(CirculoIcons.Info, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CallRow(call: BlockedCall, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = "Ver detalhes do bloqueio", onClick = onClick)
            .padding(vertical = 17.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainer), contentAlignment = Alignment.Center) {
                Icon(CirculoIcons.PhoneOff, null, Modifier.size(19.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(formatNumber(call.number), style = MaterialTheme.typography.titleMedium)
                Text("Fora da agenda", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(shortDate(call.timestamp).replace(" · ", "\n"), style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
    }
}

@Composable
private fun EmptyHistory(compact: Boolean, filtered: Boolean = false) {
    Column(Modifier.fillMaxWidth().padding(vertical = if (compact) 20.dp else 48.dp),
        horizontalAlignment = if (compact) Alignment.Start else Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (!compact) Box(Modifier.padding(bottom = 12.dp).size(76.dp).clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer), contentAlignment = Alignment.Center) {
            Icon(CirculoIcons.History, null, Modifier.size(30.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(if (filtered) "Nada neste período." else "Nenhum bloqueio por enquanto.", style = MaterialTheme.typography.titleMedium,
            textAlign = if (compact) TextAlign.Start else TextAlign.Center)
        Text(if (filtered) "Tente outro período para ver seus registros." else "Quando uma chamada for bloqueada, ela aparece aqui.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = if (compact) TextAlign.Start else TextAlign.Center)
    }
}

@Composable
private fun HistoryLoading() {
    Row(Modifier.fillMaxWidth().padding(vertical = 24.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        Text("Lendo o histórico deste aparelho…", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun HistoryError(retry: () -> Unit) {
    Column(Modifier.padding(vertical = 16.dp)) {
        Text("Não foi possível ler o histórico. O filtro de chamadas continua independente dele.", style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = retry) { Text("Tentar novamente") }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 2.dp, bottom = 10.dp).semantics { heading() })
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(horizontal = 16.dp), content = content)
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, subtitle: String, enabled: Boolean = true, showDivider: Boolean = true, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else .5f))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else .5f))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(CirculoIcons.Chevron, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (showDivider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f))
}

@Composable
private fun SettingsToggle(icon: ImageVector, title: String, subtitle: String, checked: Boolean, enabled: Boolean, onChecked: (Boolean) -> Unit, showDivider: Boolean = true) {
    Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChecked, enabled = enabled,
            modifier = Modifier.semantics { contentDescription = title })
    }
    if (showDivider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f))
}

private fun Appearance.label() = when (this) { Appearance.SYSTEM -> "Acompanhar o sistema"; Appearance.LIGHT -> "Claro"; Appearance.DARK -> "Escuro" }
private fun formatNumber(number: String): String = if (number.isBlank()) "Número indisponível" else
    PhoneNumberUtils.formatNumber(number, Locale.getDefault().country.ifBlank { "BR" }) ?: number
private fun shortDate(timestamp: Long): String = DateTimeFormatter.ofPattern("dd/MM · HH:mm", Locale.getDefault())
    .format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()))
private fun fullDate(timestamp: Long): String = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' uuuu, HH:mm", Locale.forLanguageTag("pt-BR"))
    .format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()))
private fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "Hoje"
    today.minusDays(1) -> "Ontem"
    else -> date.format(DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.forLanguageTag("pt-BR")))
}
