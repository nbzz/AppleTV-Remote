package dev.atvremote.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What the media notification needs in order to draw itself.
 *
 * A flattened copy rather than the live [UiState]: the service has no business
 * knowing about screens or pairing, and the playhead has to be sampled at a
 * known instant to be useful to the system's scrubber.
 */
class NowPlayingSnapshot(
    val deviceName: String,
    /** Null while connected with nothing playing. */
    val title: String?,
    val artist: String?,
    val album: String?,
    val artwork: ByteArray?,
    val playing: Boolean,
    /** Playhead in seconds when this snapshot was taken. */
    val position: Double?,
    /** Item length in seconds, null when the device reports none. */
    val duration: Double?,
    /** Level in 0..1, or null when the Apple TV cannot route volume. */
    val volume: Double?,
) {
    // Artwork is compared by size, as elsewhere: the bytes are large and a
    // fresh decode of identical artwork is wasted work.
    override fun equals(other: Any?): Boolean =
        other is NowPlayingSnapshot && deviceName == other.deviceName &&
            title == other.title && artist == other.artist && album == other.album &&
            playing == other.playing && position == other.position &&
            duration == other.duration && volume == other.volume &&
            (artwork?.size ?: 0) == (other.artwork?.size ?: 0)

    override fun hashCode(): Int =
        listOf(deviceName, title, artist, album, playing, position, duration, volume)
            .hashCode()
}

/** What a notification can ask the Apple TV to do. */
sealed interface RemoteCommand {
    /** Typed into the shade's reply field while the TV has a field focused. */
    data class SendText(val text: String) : RemoteCommand

    data object PlayPause : RemoteCommand
    data class Skip(val seconds: Double) : RemoteCommand
    data class SeekTo(val seconds: Double) : RemoteCommand
    data class SetVolume(val level: Double) : RemoteCommand
    /** Positive raises, negative lowers; the system's own volume convention. */
    data class AdjustVolume(val direction: Int) : RemoteCommand
}

/**
 * Handoff between the view model, which owns the connection, and the
 * notifications, which cannot reach it.
 *
 * Process-wide state rather than binder plumbing: both live in the same
 * process, and the foreground service is what keeps that process alive.
 */
object NotificationBridge {

    private val _snapshot = MutableStateFlow<NowPlayingSnapshot?>(null)

    /** Null means there is nothing to show, which stops the service. */
    val snapshot: StateFlow<NowPlayingSnapshot?> = _snapshot.asStateFlow()

    @Volatile
    private var handler: ((RemoteCommand) -> Unit)? = null

    fun publish(snapshot: NowPlayingSnapshot?) {
        _snapshot.value = snapshot
    }

    /** The view model claims command handling for as long as it is alive. */
    fun handleCommands(handler: ((RemoteCommand) -> Unit)?) {
        this.handler = handler
    }

    /** Dropped when nothing is connected, which is the honest outcome. */
    fun send(command: RemoteCommand) {
        handler?.invoke(command)
    }
}
