import org.w3c.dom.Element

/**
 * @author PG.Xie
 * created on 2022/8/2
 */

external var rulerTurn: Boolean

external fun changeZoomCenter(zoom: Double, center: DoubleArray)
external fun getMapZoom(): Double
external fun getMapCenter(): DoubleArray

external fun addStartMarker(lng: Double, lat: Double)
external fun addEndMarker(lng: Double, lat: Double)
external fun addViaMarker(lng: Double, lat: Double)
external fun tryCalcRoute()
external fun cleanMapRoute()
external fun addPositionMarker(lng: Double, lat: Double)
external fun cleanAllLngLatMarker()
external fun showTip(message: String)
external fun turnRuler(open: Boolean)
external fun searchNearby(keyword: String, radius: Double)
external fun clearSearch()
external fun buildMarkerInfo(icon: String, lng: Double, lat: Double, text: String, bkgColor: String): Any

external fun addSpeakTestScope(lng: Double, lat: Double, count: Int, speed: Int): Array<Any>
external fun fillElement(element: Element, message: String)

external fun clearTestScope()

external fun copyToClipboard(message: String)

external fun exportRoute(): Array<Array<Double>>

external fun drawMapDotElements(
    dots: String,
    probeRange: Int,
    speakRange: Int,
    color: String,
    icon: String,
    move:Boolean,
): Array<Any>

external fun drawMapLineElements(
    lines: String,
    color: String,
    move:Boolean,
): Any

external fun removeMapElementArray(any: Array<Any>)

external fun addColorPicker(id: String, inputId: String, color: String)

external fun initJsColor(element: Element, color: String)