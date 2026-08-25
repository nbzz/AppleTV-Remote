package dev.atvremote.app

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import android.graphics.BitmapFactory
import dev.atvremote.protocol.mrp.NowPlaying
import dev.atvremote.protocol.mrp.PlaybackState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import dev.atvremote.protocol.companion.AppInfo
import dev.atvremote.protocol.companion.Button
import dev.atvremote.protocol.discovery.AppleTvDevice
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.absoluteValue

// combinedClickable, for the power button's tap-versus-hold split.
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RemoteScreen(device: AppleTvDevice, state: UiState, vm: RemoteViewModel) {
    var showApps by remember { mutableStateOf(false) }
    // A hold has no on-screen feedback of its own, so the buzz is the only
    // signal that it registered rather than a tap.
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(showApps) {
        if (showApps && state.apps.isEmpty()) vm.loadApps()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        // ---- header ----
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Disconnect",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { vm.disconnect() }
                    .padding(8.dp),
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    device.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val subtitle = when {
                    state.reconnecting -> "Reconnecting…"
                    state.capabilities?.volume == true && state.volume != null ->
                        "Volume ${(state.volume * 100).toInt()}%"
                    else -> "Connected"
                }
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Default.Keyboard,
                contentDescription = "Keyboard",
                tint = if (state.keyboardOpen) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { vm.toggleKeyboard() }
                    .padding(8.dp),
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Default.Apps,
                contentDescription = "Apps",
                tint = if (showApps) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { showApps = !showApps }
                    .padding(8.dp),
            )
            Spacer(Modifier.width(4.dp))
            // A tap opens Control Centre, which is what the remote's own power
            // button does — sleeping outright, with no confirmation and no way
            // back, is not what pressing power on a remote means. Sleep lives
            // inside that panel; waking does not, since a sleeping TV has no
            // panel to open, so it stays on the hold.
            Icon(
                Icons.Default.PowerSettingsNew,
                contentDescription = "Control Centre, or hold to wake",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .combinedClickable(
                        onClick = { vm.holdHome() },
                        onLongClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.wake()
                        },
                    )
                    .padding(8.dp),
            )
        }

        state.error?.let { message ->
            Spacer(Modifier.height(8.dp))
            ErrorBanner(message) { vm.dismissError() }
        }

        if (showApps) {
            Spacer(Modifier.height(12.dp))
            AppGrid(state, vm, Modifier.weight(1f))
        }

        if (state.keyboardOpen) {
            Spacer(Modifier.height(12.dp))
            TextEntry(state, vm)
        }

        val playing = state.nowPlaying?.takeIf { it.isActive }
        if (playing != null) {
            Spacer(Modifier.height(12.dp))
            NowPlayingCard(state, playing, vm)
        } else if (!state.airplayPaired) {
            Spacer(Modifier.height(12.dp))
            EnableNowPlaying(device, state, vm)
        }

        // The drawer stands in for the controls rather than pushing them off
        // screen: a grid of apps needs the room, and the pad is unreachable
        // underneath it anyway.
        if (!showApps) {
            Spacer(Modifier.height(14.dp))

            PadModeToggle(state.padMode) { vm.setPadMode(it) }

            Spacer(Modifier.height(10.dp))

            // ---- touch surface: D-pad taps or trackpad swiping ----
            TouchPad(
                mode = state.padMode,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                onDirection = { vm.press(it) },
                onSelect = { vm.press(Button.SELECT) },
                onSelectHold = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.hold(Button.SELECT)
                },
                onSwipe = { sx, sy, ex, ey -> vm.swipe(sx, sy, ex, ey, 220) },
            )

            Spacer(Modifier.height(20.dp))

            // ---- transport row ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                RoundButton(Icons.AutoMirrored.Filled.ArrowBack, "Menu") { vm.press(Button.MENU) }

                // One button toggles playback, so it shows the action it will
                // perform. Without now-playing there is no state to reflect, and
                // it falls back to the play glyph.
                val isPlaying = state.nowPlaying?.playbackState == PlaybackState.PLAYING
                RoundButton(
                    icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    description = if (isPlaying) "Pause" else "Play",
                ) { vm.press(Button.PLAY_PAUSE) }

                RoundButton(Icons.Default.Home, "Home") { vm.press(Button.HOME) }
            }

            // Volume only appears when the Apple TV reports it can route it. With an
            // IR setup the Siri Remote blasts infrared itself, so nothing on the
            // network can change the volume and dead buttons would be misleading.
            if (state.capabilities?.volume == true && playing == null) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    RoundButton(Icons.Default.VolumeDown, "Volume down") { vm.volumeDown() }
                    RoundButton(Icons.Default.VolumeUp, "Volume up") { vm.volumeUp() }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}

/**
 * Combined D-pad and trackpad.
 *
 * A tap near the centre selects; a tap elsewhere maps to the dominant
 * direction, which is what makes it usable one-handed. Dragging sends a real
 * swipe so momentum scrolling in tvOS lists behaves naturally.
 */
@Composable
private fun TouchPad(
    mode: PadMode,
    modifier: Modifier = Modifier,
    onDirection: (Button) -> Unit,
    onSelect: () -> Unit,
    onSelectHold: () -> Unit,
    onSwipe: (Int, Int, Int, Int) -> Unit,
) {
    // Keyed on `mode` so the gesture detectors are rebuilt when it changes.
    val gestures = when (mode) {
        PadMode.DPAD -> Modifier.pointerInput(mode) {
            fun centre(offset: Offset): Boolean {
                val deadZone = size.width * 0.22f
                return abs(offset.x - size.width / 2f) < deadZone &&
                    abs(offset.y - size.height / 2f) < deadZone
            }

            fun direction(offset: Offset) {
                val dx = offset.x - size.width / 2f
                val dy = offset.y - size.height / 2f
                if (abs(dx) > abs(dy)) {
                    onDirection(if (dx > 0) Button.RIGHT else Button.LEFT)
                } else {
                    onDirection(if (dy > 0) Button.DOWN else Button.UP)
                }
            }

            detectTapGestures(
                // Compose fires either onLongPress or onTap, never both, so a
                // held direction has to be sent from here as well — otherwise
                // resting on an arrow for half a second sends nothing at all.
                // Only the centre holds; tvOS repeats directions itself.
                onLongPress = { offset ->
                    if (centre(offset)) onSelectHold() else direction(offset)
                },
                onTap = { offset ->
                    if (centre(offset)) onSelect() else direction(offset)
                },
            )
        }

        // One detector rather than a tap one and a drag one side by side, so
        // the gesture is classified on release when the whole of it is known.
        // Two detectors cannot agree: resting a finger before dragging fires a
        // long press at the timeout, and the drag then arrives behind it, so a
        // swipe would open a context menu on its way past.
        PadMode.SWIPE -> Modifier.pointerInput(mode) {
            awaitEachGesture {
                val down = awaitFirstDown()
                val start = down.position
                var end = start
                var moved = false
                // Pointer events carry their own clock; wall time is a
                // different base and would not compare meaningfully.
                var releasedAt = down.uptimeMillis

                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    end = change.position
                    releasedAt = change.uptimeMillis
                    if (!moved && (end - start).getDistance() > viewConfiguration.touchSlop) {
                        moved = true
                    }
                    if (moved) change.consume()
                    if (!change.pressed) break
                }

                val heldMs = releasedAt - down.uptimeMillis

                // Map local pixels into the 0..1000 space the device uses.
                val sx = (start.x / size.width * 1000).toInt().coerceIn(0, 1000)
                val sy = (start.y / size.height * 1000).toInt().coerceIn(0, 1000)
                val ex = (end.x / size.width * 1000).toInt().coerceIn(0, 1000)
                val ey = (end.y / size.height * 1000).toInt().coerceIn(0, 1000)

                when {
                    moved && (abs(ex - sx) > 40 || abs(ey - sy) > 40) -> onSwipe(sx, sy, ex, ey)
                    moved -> Unit // too small to mean anything either way
                    heldMs >= viewConfiguration.longPressTimeoutMillis -> onSelectHold()
                    else -> onSelect()
                }
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surface)
            .then(gestures),
        contentAlignment = Alignment.Center,
    ) {
        // Direction affordances only apply when taps steer.
        if (mode == PadMode.DPAD) {
        Icon(
            Icons.Default.KeyboardArrowUp, null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 18.dp).size(30.dp),
        )
        Icon(
            Icons.Default.KeyboardArrowDown, null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp).size(30.dp),
        )
        Icon(
            Icons.Default.KeyboardArrowLeft, null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 18.dp).size(30.dp),
        )
        Icon(
            Icons.Default.KeyboardArrowRight, null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 18.dp).size(30.dp),
        )
        }

        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "OK",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (mode == PadMode.SWIPE) {
            Text(
                "Swipe to scroll · tap to select",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp),
            )
        }
    }
}

/** Segmented control choosing how the touch surface interprets gestures. */
@Composable
private fun PadModeToggle(current: PadMode, onSelect: (PadMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PadModeOption(
            label = "D-pad",
            icon = Icons.Default.Gamepad,
            selected = current == PadMode.DPAD,
            modifier = Modifier.weight(1f),
        ) { onSelect(PadMode.DPAD) }

        PadModeOption(
            label = "Swipe",
            icon = Icons.Default.TouchApp,
            selected = current == PadMode.SWIPE,
            modifier = Modifier.weight(1f),
        ) { onSelect(PadMode.SWIPE) }
    }
}

@Composable
private fun PadModeOption(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RoundButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(26.dp),
        )
    }
}

/**
 * Text entry for the focused field on the Apple TV.
 *
 * Text is pushed on submit rather than per keystroke: each send restarts the
 * RTI session to read authoritative state, so per-character sends would be both
 * slow and racy.
 */
@Composable
private fun TextEntry(state: UiState, vm: RemoteViewModel) {
    var draft by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                singleLine = true,
                placeholder = { Text("Type on the Apple TV") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    vm.sendText(draft)
                    draft = ""
                }),
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable {
                        vm.sendText(draft)
                        draft = ""
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Send text",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { vm.clearText() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Backspace,
                    contentDescription = "Clear field",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        val status = when {
            state.checkingField -> "Checking for a focused field…"
            state.fieldText == null -> "No text field focused on the Apple TV."
            state.fieldText.isEmpty() -> "Field is empty."
            else -> "Field: \"${state.fieldText}\""
        }
        Text(
            status,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Now-playing card, modelled on the iOS Control Centre widget: artwork and
 * track on top, a scrubber, transport controls, then volume.
 *
 * Artwork arrives as raw bytes on the MRP channel; it is decoded here rather
 * than in the protocol layer so that module stays free of Android types.
 */
@Composable
private fun NowPlayingCard(state: UiState, playing: NowPlaying, vm: RemoteViewModel) {
    val artwork = remember(playing.artwork?.size) {
        playing.artwork?.let { bytes ->
            runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }.getOrNull()
        }
    }

    // The device reports a position now and then, so the card runs the clock
    // itself in between; the anchor it extrapolates from lives in the state.
    var position by remember { mutableStateOf(state.positionNow()) }
    LaunchedEffect(state.playhead, state.playheadAt, state.playheadAdvancing) {
        position = state.positionNow()
        while (state.playheadAdvancing) {
            delay(500)
            position = state.positionNow()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (artwork != null) {
                    Image(
                        bitmap = artwork.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                // The app is the source line. Playing and paused are already
                // obvious from the transport glyph, so only the states that
                // are not are worth spelling out.
                val stateLabel = when (playing.playbackState) {
                    PlaybackState.SEEKING -> "Seeking"
                    PlaybackState.STOPPED -> "Stopped"
                    PlaybackState.INTERRUPTED -> "Interrupted"
                    else -> null
                }
                val caption = listOfNotNull(playing.appName, stateLabel).joinToString(" · ")
                if (caption.isNotBlank()) {
                    Text(
                        caption,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    playing.title ?: "Nothing playing",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                playing.artist?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        val elapsed = position
        val duration = playing.position?.second
        if (elapsed != null && duration != null) {
            Spacer(Modifier.height(12.dp))
            Scrubber(elapsed, duration) { vm.seekTo(it) }
        }

        // Transport stays available while paused: skipping about is as useful
        // then as it is during playback.
        val seekable = playing.playbackState in setOf(
            PlaybackState.PLAYING, PlaybackState.PAUSED, PlaybackState.SEEKING,
        )
        if (seekable) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TransportButton(Icons.Default.Replay10, "Back 10 seconds", 30.dp) {
                    vm.skip(-SKIP_SECONDS)
                }
                val isPlaying = playing.playbackState == PlaybackState.PLAYING
                TransportButton(
                    icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    description = if (isPlaying) "Pause" else "Play",
                    size = 40.dp,
                ) { vm.press(Button.PLAY_PAUSE) }
                TransportButton(Icons.Default.Forward10, "Forward 10 seconds", 30.dp) {
                    vm.skip(SKIP_SECONDS)
                }
            }
        }

        if (state.capabilities?.volume == true) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PillButton(
                    icon = Icons.Default.VolumeDown,
                    label = "Volume down",
                    modifier = Modifier.weight(1f),
                ) { vm.volumeDown() }

                PillButton(
                    icon = Icons.Default.VolumeUp,
                    label = "Volume up",
                    modifier = Modifier.weight(1f),
                ) { vm.volumeUp() }
            }
        }
    }
}

private const val SKIP_SECONDS = 10.0

/**
 * Playhead with elapsed and remaining times.
 *
 * Dragging only moves the local fill; the seek is sent on release, because
 * every intermediate position would otherwise become its own network command.
 */
@Composable
private fun Scrubber(position: Double, duration: Double, onSeek: (Double) -> Unit) {
    var scrubbing by remember { mutableStateOf<Float?>(null) }
    val fraction = scrubbing ?: (position / duration).toFloat().coerceIn(0f, 1f)
    val shown = fraction * duration

    val trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
    val fillColor = MaterialTheme.colorScheme.onSurface

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                formatTime(shown),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "-${formatTime(duration - shown)}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .pointerInput(duration) {
                    detectTapGestures { offset ->
                        onSeek((offset.x / size.width).coerceIn(0f, 1f) * duration)
                    }
                }
                .pointerInput(duration) {
                    detectHorizontalDragGestures(
                        onDragStart = { scrubbing = (it.x / size.width).coerceIn(0f, 1f) },
                        onDragEnd = {
                            scrubbing?.let { onSeek(it * duration) }
                            scrubbing = null
                        },
                        onDragCancel = { scrubbing = null },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            scrubbing = (change.position.x / size.width).coerceIn(0f, 1f)
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxWidth().height(4.dp)) {
                val radius = CornerRadius(size.height / 2)
                drawRoundRect(color = trackColor, cornerRadius = radius)
                if (fraction > 0f) {
                    drawRoundRect(
                        color = fillColor,
                        size = Size(size.width * fraction, size.height),
                        cornerRadius = radius,
                    )
                }
            }
        }
    }
}

private fun formatTime(seconds: Double): String {
    val total = seconds.coerceAtLeast(0.0).toInt()
    val hours = total / 3600
    return if (hours > 0) "%d:%02d:%02d".format(hours, total % 3600 / 60, total % 60)
    else "%d:%02d".format(total / 60, total % 60)
}

@Composable
private fun TransportButton(
    icon: ImageVector,
    description: String,
    size: Dp,
    onClick: () -> Unit,
) {
    Icon(
        icon,
        contentDescription = description,
        tint = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(10.dp)
            .size(size),
    )
}

@Composable
private fun PillButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun EnableNowPlaying(
    device: dev.atvremote.protocol.discovery.AppleTvDevice,
    state: UiState,
    vm: RemoteViewModel,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = !state.busy) { vm.startAirPlayPairing(device) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Show what's playing",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "Needs a second pairing code from the TV",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            if (state.busy) "…" else "Set up",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun AppGrid(state: UiState, vm: RemoteViewModel, modifier: Modifier = Modifier) {
    if (state.apps.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Text(
                "Loading apps…",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(state.apps, key = { it.bundleId }) { app ->
            AppTile(app) { vm.launchApp(app.bundleId) }
        }
    }
}

/**
 * One app, drawn as a tvOS-shaped tile.
 *
 * Real artwork is fetched in the background and takes a moment to arrive, so
 * every tile starts as initials over a colour derived from its bundle id. That
 * stand-in is also the permanent answer for anything the store does not know,
 * which covers the built-in apps.
 */
@Composable
private fun AppTile(app: AppInfo, onClick: () -> Unit) {
    // tvOS icons are landscape, 400x240, and the shelf reads wrong at 1:1.
    val hue = (app.bundleId.hashCode().absoluteValue % 360).toFloat()
    val tile = Brush.linearGradient(
        listOf(Color.hsv(hue, 0.45f, 0.62f), Color.hsv((hue + 26f) % 360f, 0.55f, 0.42f))
    )

    // Apple's own apps are drawn from bundled artwork and never fetched; they
    // are in no catalogue, and the tile is the same 5:3 either way.
    val bundled = remember(app.bundleId) { appleIcon(app.bundleId) }
    val lookupId = remember(app.bundleId) { storeBundleId(app.bundleId) }
    val context = LocalContext.current
    var artwork by remember(app.bundleId) { mutableStateOf<AppArtwork?>(null) }
    LaunchedEffect(app.bundleId) {
        if (lookupId != null) artwork = AppIcons.load(context, app.name, lookupId)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(5f / 3f)
                .clip(RoundedCornerShape(10.dp))
                .background(tile)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            val icon = artwork
            when {
                bundled != null -> Image(
                    painter = painterResource(bundled),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )

                // Fetched artwork replaces the initials once it lands.
                icon != null -> Image(
                    bitmap = icon.bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = if (icon.wide) ContentScale.Crop else ContentScale.Fit,
                    modifier = if (icon.wide) Modifier.fillMaxSize()
                    else Modifier.fillMaxHeight().padding(vertical = 7.dp),
                )

                else -> Text(
                    initials(app.name),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
        }

        Spacer(Modifier.height(5.dp))

        Text(
            app.name,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/** Up to two letters, the way a contact avatar reduces a name. */
private fun initials(name: String): String {
    val words = name.split(' ', '-', '+').filter { it.isNotBlank() }
    return when {
        words.isEmpty() -> "?"
        words.size == 1 -> words[0].take(2).uppercase()
        else -> (words[0].take(1) + words[1].take(1)).uppercase()
    }
}
