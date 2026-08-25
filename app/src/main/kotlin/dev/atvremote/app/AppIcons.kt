package dev.atvremote.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

/** Artwork plus the shape it came in, which decides how the tile draws it. */
class AppArtwork(val bitmap: Bitmap) {
    /** tvOS art is 5:3; an iOS fallback is square and must not be cropped. */
    val wide: Boolean = bitmap.width.toFloat() / bitmap.height.coerceAtLeast(1) > 1.4f
}

/**
 * Artwork for the app drawer.
 *
 * The Apple TV never sends icons — Companion Link reports a name and a bundle
 * id and nothing else — so real artwork comes from Apple's public iTunes
 * endpoints, the same ones App Store search uses.
 *
 * No single query finds everything, so several are tried in turn: the tvOS
 * entry in the phone's own store region, then the US store, then the app's iOS
 * entry, and finally a search by name. Each step down trades fidelity for
 * coverage, and the last one can be wrong, so it only counts when the store's
 * name and the TV's name agree.
 *
 * Everything is cached on disk, hits and misses alike, so an app is asked
 * about once rather than on every visit to the drawer.
 */
object AppIcons {

    private val memory = mutableMapOf<String, AppArtwork?>()
    private val lock = Mutex()

    /** Opening forty connections at once to draw one screen helps nobody. */
    private val network = Semaphore(4)

    /** Bumped whenever the queries or the match rules change. */
    private const val CACHE_DIR = "appicons-v4"

    suspend fun load(context: Context, name: String, bundleId: String): AppArtwork? {
        lock.withLock { if (memory.containsKey(bundleId)) return memory[bundleId] }

        val art = withContext(Dispatchers.IO) { fetch(context, name, bundleId) }
        lock.withLock { memory[bundleId] = art }
        return art
    }

    private suspend fun fetch(context: Context, name: String, bundleId: String): AppArtwork? {
        val dir = cacheDir(context)
        val safe = bundleId.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val cached = File(dir, "$safe.png")
        val missed = File(dir, "$safe.miss")

        if (cached.isFile) {
            decode(cached.readBytes())?.let { return AppArtwork(it) }
            cached.delete() // truncated by a cache eviction mid-write
        }
        if (missed.isFile) return null

        val country = storeCountry(context)
        val bytes = network.withPermit {
            try {
                var url = artworkUrl(bundleId, "tvSoftware", country)

                // The tvOS entry is region-specific; the US store is the widest
                // catalogue and worth a second ask when the local one is empty.
                if (url == null && !country.equals("US", ignoreCase = true)) {
                    url = artworkUrl(bundleId, "tvSoftware", "US")
                }
                // No tvOS listing at all: the iOS entry is square but real.
                if (url == null) url = artworkUrl(bundleId, null, country)
                // Bundle ids do diverge between platforms, so fall back to the
                // name. Only the search endpoint's iOS entity returns anything.
                if (url == null) url = searchUrl(name, country)

                if (url == null) null else download(url)
            } catch (e: Exception) {
                // Network trouble, as opposed to an empty catalogue. Leaving
                // without a verdict means the next visit tries again, rather
                // than the app wearing initials until the cache is cleared.
                return null
            }
        }

        if (bytes == null) {
            runCatching { missed.createNewFile() }
            return null
        }

        val bitmap = decode(bytes) ?: return null
        runCatching { cached.writeBytes(bytes) }
        return AppArtwork(bitmap)
    }

    /**
     * Cache location, versioned by lookup strategy.
     *
     * A miss recorded by an older, narrower set of queries says nothing about
     * what the current ones would find, so widening the search has to leave
     * those verdicts behind or the apps it was meant to fix keep their
     * initials for good.
     */
    private fun cacheDir(context: Context): File {
        val dir = File(context.cacheDir, CACHE_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
            // Clear out every earlier generation, which holds both misses the
            // current queries would find and matches they would now reject.
            runCatching {
                context.cacheDir.listFiles()
                    ?.filter { it.isDirectory && it.name.startsWith("appicons") }
                    ?.filter { it.name != CACHE_DIR }
                    ?.forEach { it.deleteRecursively() }
            }
        }
        return dir
    }

    /** The phone's region, which is the store a regional app is listed in. */
    private fun storeCountry(context: Context): String {
        val locales = context.resources.configuration.locales
        val country = if (locales.isEmpty) Locale.getDefault().country
        else locales[0].country
        return country.ifBlank { "US" }
    }

    private fun artworkUrl(bundleId: String, entity: String?, country: String): String? {
        val query = buildString {
            append("bundleId=").append(URLEncoder.encode(bundleId, "UTF-8"))
            append("&country=").append(URLEncoder.encode(country, "UTF-8"))
            append("&limit=1")
            if (entity != null) append("&entity=").append(entity)
        }
        val results = results("https://itunes.apple.com/lookup?$query") ?: return null
        if (results.length() == 0) return null
        return artworkOf(results.getJSONObject(0))
    }

    /**
     * Last resort: match on name. Only the software entity returns results
     * from search — tvSoftware yields nothing there — so this finds the iOS
     * listing, and its square icon, or nothing.
     */
    private fun searchUrl(name: String, country: String): String? {
        if (normalise(name).length < 4) return null // too short to match safely

        val query = buildString {
            append("term=").append(URLEncoder.encode(name, "UTF-8"))
            append("&country=").append(URLEncoder.encode(country, "UTF-8"))
            append("&entity=software&limit=5")
        }
        val results = results("https://itunes.apple.com/search?$query") ?: return null

        for (i in 0 until results.length()) {
            val entry = results.getJSONObject(i)
            if (!namesAgree(name, entry.optString("trackName"))) continue
            return artworkOf(entry)
        }
        return null
    }

    /**
     * Whether a search hit is the same app, judged on name alone.
     *
     * Store listings dress names up with a tagline — "JioCinema – TV & Shows",
     * "UHF - Love your IPTV" — so the part before the separator is allowed to
     * carry the match. What is not allowed is a bare prefix: "Fusion" matching
     * "Fusion Smart Education" put a stranger's logo on the tile, and a wrong
     * icon is worse than initials.
     */
    private fun namesAgree(appName: String, storeName: String): Boolean {
        val app = normalise(appName)
        if (app.length < 3) return false // too generic to place safely
        if (normalise(storeName) == app) return true

        val head = storeName.split(':', '–', '—', '-', '|', '(').first()
        return normalise(head) == app
    }

    private fun normalise(s: String) = s.lowercase().filter { it.isLetterOrDigit() }

    private fun artworkOf(entry: JSONObject): String? =
        listOf("artworkUrl512", "artworkUrl100", "artworkUrl60")
            .firstNotNullOfOrNull { entry.optString(it).takeIf { url -> url.isNotEmpty() } }

    private fun results(url: String): JSONArray? {
        val body = download(url)?.toString(Charsets.UTF_8) ?: return null
        return runCatching { JSONObject(body).optJSONArray("results") }.getOrNull()
    }

    /**
     * Returns null only for "this does not exist"; throws for everything else.
     *
     * The distinction decides whether a miss is written to disk for good, so
     * being throttled — iTunes answers a burst of lookups with 403 or 429 —
     * must not read as an empty catalogue. Treating those as absence left
     * every app in a rate-limited drawer wearing initials permanently.
     */
    private fun download(url: String): ByteArray? {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            val code = connection.responseCode
            if (code == HttpURLConnection.HTTP_NOT_FOUND) return null
            if (code != HttpURLConnection.HTTP_OK) {
                throw IOException("iTunes answered $code for $url")
            }
            return connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }

    private fun decode(bytes: ByteArray): Bitmap? =
        runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }.getOrNull()
}
