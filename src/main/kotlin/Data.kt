import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * @author PG.Xie
 * created on 2022/8/2
 */

sealed interface Shape {

}

@Serializable
data class Dot(
    val lng: Double,
    val lat: Double,
    val name: String = "",
    val textCount: Int = -1,
    val angle: Double = -1.0,
) : Shape

@Serializable
data class Line(
    val lng: Double,
    val lat: Double,
) : Shape


const val TYPE_DOT = 1
const val TYPE_LINE = 2


@Serializable
data class CacheShape(
    var title: String,
    var content: String,
    var probeRange: Int,
    var speakRange: Int,
    var color: String,
    @Transient
    var markerResult: Array<Any> = arrayOf(),
)

@Serializable
data class CacheData(
    var index: Int,
    var type: Int,
    val obj: CacheShape,
)