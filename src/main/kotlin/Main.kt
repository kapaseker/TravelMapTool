import kotlinx.browser.window
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventListener

/**
 * @author PG.Xie
 * created on 2022/6/17
 */

private const val KEY_MAP_CENTER_LNG = "map_center.lng";
private const val KEY_MAP_CENTER_LAT = "map_center.lat";
private const val KEY_MAP_ZOOM = "map_zoom";

private const val KEY_CONTENT = "content"

const val ID_SEARCH_WRAPPER = "search-panel"


fun initMap() {
    restoreWebState()
    window.addEventListener("beforeunload", object : EventListener {
        override fun handleEvent(event: Event) {
            saveWebState()
        }
    })
}


fun restoreWebState() {
    restoreMapState()
    restoreLoadedContent()
}

fun saveWebState() {
    saveMapState()
    saveLoadedContent()
}

fun restoreMapState() {
    val zoom = getItem(KEY_MAP_ZOOM, "11").toDouble()
    var centerLng = getItem(KEY_MAP_CENTER_LNG, "116.397428").toDouble()
    var centerLat = getItem(KEY_MAP_CENTER_LAT, "39.90923").toDouble()

    changeZoomCenter(zoom, doubleArrayOf(centerLng, centerLat))
}

fun saveMapState() {
    saveItem(KEY_MAP_ZOOM, getMapZoom().toString())
    val center = getMapCenter()
    saveItem(KEY_MAP_CENTER_LNG, center[0].toString())
    saveItem(KEY_MAP_CENTER_LAT, center[1].toString())
}

fun restoreLoadedContent() {
    val content = getItem(KEY_CONTENT, "")
    if (!content.isNullOrBlank()) {
        log("restored $content")
        try {
            val cacheValues = Json.decodeFromString<Array<CacheData>>(content)
            cacheValues.forEach {
                CacheContent[it.index] = it
            }
            renderContentList(cacheValues)
        } catch (ex: Exception) {
            log(ex.toString())
        }

    }
}

fun saveLoadedContent() {
    if (CacheContent.isNotEmpty()) {
        val cacheValues = CacheContent.values.toTypedArray()
        val saveValue = Json.encodeToString(cacheValues)
        log("saved $saveValue")
        saveItem(KEY_CONTENT, saveValue)
    } else {
        removeItem(KEY_CONTENT)
    }
}