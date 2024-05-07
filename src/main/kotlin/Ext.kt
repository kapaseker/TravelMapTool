import org.w3c.dom.Element

/**
 * @author PG.Xie
 * created on 2022/7/29
 */
/**
 * 把自己移除掉的扩展
 */
fun Element?.remove() {
    this?.parentElement?.removeChild(this)
}

fun Int.makeTime(unit: String): String {
    return if (this > 0) {
        "${this}$unit"
    } else {
        ""
    }
}

private val IMAGES_CENTER = arrayOf(
    "https://webapi.amap.com/images/mass/mass0.png",
    "http://a.amap.com/jsapi_demos/static/images/blue.png",
    "http://a.amap.com/jsapi_demos/static/images/green.png",
    "http://a.amap.com/jsapi_demos/static/images/orange.png",
    "http://a.amap.com/jsapi_demos/static/images/red.png",
    "https://webapi.amap.com/images/mass/mass1.png",
    "https://webapi.amap.com/images/mass/mass2.png",
)

fun listItemIcon(type: Int): String {
    return when (type) {
        TYPE_DOT -> "res/dot.svg"
        else -> "res/line.svg"
    }
}

fun centerMarkerImage(idx: Int, width: Int, height: Int, color: String): String {
    return IMAGES_CENTER[idx % IMAGES_CENTER.size]
}


private const val DEBUG = true
private const val DEF_TAG = "TravelTool"

/**
 * 打印日志
 */
fun log(tag: String, message: String) {
    if (DEBUG) {
        console.log("[$tag] $message")
    }
}

/**
 * 打印日志
 */
fun log(message: String) {
    log(DEF_TAG, message)
}