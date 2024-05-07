import androidx.compose.runtime.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.dom.addClass
import kotlinx.dom.clear
import kotlinx.dom.removeClass
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.renderComposable
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLLabelElement
import kotlin.Double.Companion.NaN
import kotlin.math.floor
import kotlin.random.Random

/**
 * @author PG.Xie
 * created on 2022/6/17
 */

const val SUFFIX_DOT = "点阵"
const val SUFFIX_LINE = "线组"

const val TITLE_NEW_DOT = "新建$SUFFIX_DOT"
const val TITLE_NEW_LINE = "新建$SUFFIX_LINE"

const val PLACE_HOLDER_DOT = "123.456 34.567 名字 解说字数(数字类型，可不写) 角度(数字类型，可不写)"

const val PLACE_HOLDER_LINE = "116.397461 39.909186\n116.389741 39.904873\n116.397676,39.904797\n116.403927 39.909886"

const val ID_ROOT = "rootui"
const val ID_RESULT = "route-result"
const val ID_CONTENT = "marker-content"
const val ID_DOT_CONTENT = "dot-content"
const val ID_CONTENT_TEXT_AREA = "input_content"
const val ID_COLOR_PICKER_INPUT = "input-color"
const val ID_CONTEXT_MENU = "context-menu"

const val ID_CONTENT_LIST = "marker-content-list"
const val ID_CONTENT_LIST_ITEM = "marker-content-list-"
const val ID_CONTENT_LIST_ITEM_TITLE = "marker-content-list-title-"

const val MENU_WIDTH = 160
const val MENU_HEIGHT = 410

const val WORD_PER_SECOND = 4.785


fun initUI() {
    renderComposable(rootElementId = ID_ROOT) {
        Style(StyleContentItem)
        renderNavBar()
        renderContextMenu()
        renderLoading()
        renderCoreFunction()
        renderContent()
        renderKeywordSearch()
    }
}


fun renderContentList(items: Array<CacheData>) {
    renderComposable(ID_CONTENT_LIST) {
        items.sortByDescending { it.index }
        items.forEach { item ->
            renderContentItem(item.index, listItemIcon(item.type), item.obj.title, item.obj.color, false)
        }
    }
}

fun renderNewContentItem(item: CacheData) {
    renderComposable(ID_CONTENT_LIST) {
        renderContentItem(item.index, listItemIcon(item.type), item.obj.title, item.obj.color, true)
    }
}


object StyleContentItem : StyleSheet() {

    val icon by style {
        width(24.px)
        height(24.px)
        marginRight(10.px)
        display(DisplayStyle.InlineBlock)
        property("vertical-align", "middle")
    }

    val iconCheck by style {
        width(28.px)
        height(28.px)
        marginRight(4.px)
        property("vertical-align", "middle")
        backgroundColor(Color("#666"))
        display(DisplayStyle.InlineBlock)
    }

    val iconButton by style {
        padding(0.px, 8.px)
        display(DisplayStyle.LegacyInlineFlex)
        alignItems("center")
        marginLeft(CONTENT_LIST_RIGHT_MARGIN)
    }
}

data class ContentItem(var color: String)

private val ITEM_MAP = mutableMapOf<Int, MutableState<ContentItem>>()

@Composable
fun renderContentItem(index: Int, icon: String, title: String, color: String, checked: Boolean) {

    log("render item $index $title $checked")

    val itemState by remember { ITEM_MAP.getOrPut(index) { mutableStateOf(ContentItem(color)) } }

    Li(attrs = {
        id("$ID_CONTENT_LIST_ITEM$index")
    }) {
        Div(attrs = {
            classes("uk-card", "uk-card-default", "uk-card-body", "uk-card-small", "uk-card-hover")
            style {
                padding(6.px)
            }
        }) {

            Span(attrs = {
                classes(StyleContentItem.icon)
                style {
                    background(itemState.color)
                    property("mask", "url($icon)")
                    property("mask-size", "contain")
                }
            }) {

            }

            Label(attrs = {id("$ID_CONTENT_LIST_ITEM_TITLE$index")}) { Text(title) }

            var checkState by remember { mutableStateOf(checked) }

            CheckboxInput {
                checked(checkState)
                style {
                    marginLeft(160.px)
                }
                onChange { element ->
                    checkState = element.value
                    CacheContent[index]?.toggleMapElement(checkState)
                }
            }

            Span(attrs = {
                classes(StyleContentItem.iconCheck)
                style {
                    property(
                        "mask", "url(${
                            if (checkState) {
                                "./res/icon-visible.svg"
                            } else {
                                "./res/icon-invisible.svg"
                            }
                        })"
                    )
                    property("mask-size", "contain")
                }
            }) {

            }

            Button(attrs = {
                classes("uk-button", "uk-button-small", "uk-button-default", StyleContentItem.iconButton)
                onClick {
                    CacheContent[index]?.loadMarkerContent(checkState)
                }
            }) {
                Span(attrs = {
                    attr("uk-icon", "settings")
                }) {

                }
                Text("配置")
            }

            var deleting by remember { mutableStateOf(false) }

            Button(attrs = {
                classes(
                    StyleContentItem.iconButton,
                    "uk-button", "uk-button-small", if (deleting) {
                        "uk-button-secondary"
                    } else {
                        "uk-button-default"
                    }
                )
                onClick {
                    if (deleting) {
                        deleteCacheData(index)
                    } else {
                        deleting = true
                        GlobalScope.launch {
                            delay(2000L)
                            deleting = false
                        }
                    }
                }
            }) {
                Span(attrs = {
                    attr(
                        "uk-icon", if (deleting) {
                            "check"
                        } else {
                            "trash"
                        }
                    )
                }) {

                }
                Text(
                    if (deleting) {
                        "确定"
                    } else {
                        "删除"
                    }
                )
            }

        }
    }
}


@NoLiveLiterals
@Composable
fun renderCoreFunction() {
    window.asDynamic()["loadRouteResult"] = ::loadRouteResult
    window.asDynamic()["removeRouteResult"] = ::removeRouteResult
}


@Composable
fun renderKeywordSearch() {
    Div(attrs = {
        id(ID_SEARCH_WRAPPER)
        classes("left_wrapper")
    }) {

    }
}


private val CONTENT_LIST_RIGHT_MARGIN = 16.px


@Composable
fun renderContent() {
    Div(attrs = {
        classes("left_wrapper")
    }) {
        Ul(attrs = {
            id(ID_CONTENT_LIST)
            classes("uk-list")
        }) {

        }
    }

    Div(attrs = {
        id(ID_CONTENT)
        classes("left_wrapper")
    }) {

    }
}


@Composable
fun renderLoading() {

    val loadingBar = remember { mutableStateOf(DisplayStyle.None) }

    window.asDynamic()["loading"] = { loading: Boolean ->
        loadingBar.value = if (loading) {
            removeRouteResult()
            DisplayStyle.InlineBlock
        } else {
            DisplayStyle.None
        }
        0
    }

    Div(attrs = {
        classes("lds-roller")
        style {
            display(loadingBar.value)
        }
    }) {
        Div { }
        Div { }
        Div { }
        Div { }
        Div { }
        Div { }
        Div { }
        Div { }
    }
}

private const val MAX_RADIUS = 50.0

@Composable
fun renderNavBar() {

    var keyword by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf(0.0) }

    Div(attrs = {
        classes("left_botton_wrapper")
        style {
            background("white")
        }
    }) {
        Div(attrs = {
            classes("uk-button-group", "button-radio-group")
        }) {
            makeNavActionButton("plus", "点") {
                loadDotMarkerContent(
                    TITLE_NEW_DOT,
                    "",
                    PLACE_HOLDER_DOT,
                    DEF_PROBE_RANGE,
                    DEF_SPEAK_RANGE,
                    generateRandomColor(),
                )
            }
            makeNavActionButton("plus", "线") {
                loadLineMarkerContent(TITLE_NEW_LINE, "", PLACE_HOLDER_LINE, generateRandomColor())
            }
        }
        makeNavCheckedButton("鼠标测距") {
            turnRuler(it)
        }
        Form(attrs = {
            classes("uk-search", "uk-search-default")
            style {
                backgroundColor(Color.white)
                width(260.px)
            }
            onSubmit {
                it.preventDefault()

                if (radius > MAX_RADIUS) {
                    window.alert("半径超过50千米，最大只能支持50千米")
                    return@onSubmit
                }

                if (keyword.isEmpty()) {
                    return@onSubmit
                }

                if (dispatchFunciont(keyword)) {
                    return@onSubmit
                }

                searchNearby(keyword, radius)

            }
        }) {
            Span(attrs = {
                attr("uk-search-icon", "")
            }) {

            }
            Input(InputType.Text, attrs = {
                classes("uk-search-input")
                placeholder("输入关键字，敲下回车")
                onChange {
                    keyword = it.value
                    if (it.value.isEmpty()) {
                        clearSearch()
                    }
                }
            })
        }
        Input(InputType.Number, attrs = {
            classes("uk-input", "uk-form-width-small")
            placeholder("输入半径")
            onChange {
                radius = if (it.value == NaN) {
                    0.0
                } else {
                    it.value?.toDouble() ?: 0.0
                }
            }
        })
    }
}

private fun dispatchFunciont(keyword: String): Boolean {

    return when (keyword) {
        "//help" -> {
            window.location.href = "./md/README.html"
            true
        }

        else -> false
    }
}

const val CHECK_DATA = "data-checked"
const val TRUE = "true"
const val FALSE = "false"


@Composable
fun makeNavCheckedButton(text: String, onCheck: ((open: Boolean) -> Unit)? = null) {

    Button(attrs = {
        classes("uk-button-default", "uk-button", "button-select")
        type(ButtonType.Button)
        onClick { event ->
            (event.target as? HTMLElement)?.let {
                val checked = it.getAttribute(CHECK_DATA)
                if (checked == FALSE) {
                    it.setAttribute(CHECK_DATA, TRUE)
                    it.removeClass("uk-button-default")
                    it.addClass("uk-button-primary")
                    onCheck?.invoke(true)
                } else {
                    it.setAttribute(CHECK_DATA, FALSE)
                    it.removeClass("uk-button-primary")
                    it.addClass("uk-button-default")
                    onCheck?.invoke(false)
                }
            }
        }
        attr(CHECK_DATA, FALSE)
    }) {
        Text(text)
    }
}

@Composable
fun makeNavActionButton(icon: String?, text: String, click: (() -> Unit)? = null) {

    Button(attrs = {
        classes("uk-button-default", "uk-button")
        type(ButtonType.Button)
        click?.let {
            onClick { click() }
        }
    }) {
        icon?.let {
            Span(attrs = {
                attr("uk-icon", "icon:  $icon")
                classes("uk-margin-small-right")
            })
        }
        Text(text)
    }
}

private var markPosition = Pair(0.0, 0.0)

@Composable
@NoLiveLiterals
fun renderContextMenu() {

    val contextMenuDisplay = remember { mutableStateOf(DisplayStyle.None) }
    val contextMenuPosition = remember { mutableStateOf(Pair(0.px, 0.px)) }

    val showContextMenu = { x: Int, y: Int, lng: Double, lat: Double ->
        if (!rulerTurn) {
            contextMenuDisplay.value = DisplayStyle.Block

            val pos = Pair(
                if ((x + MENU_WIDTH) > window.innerWidth) {
                    x - MENU_WIDTH
                } else {
                    x
                }.px, if (y + MENU_HEIGHT > window.innerHeight) {
                    if (y - MENU_HEIGHT < 0) {
                        20
                    } else {
                        y - MENU_HEIGHT
                    }
                } else {
                    y
                }.px
            )

            log("xy $x x $y")
            log("body ${document.body!!.clientWidth} x ${document.body!!.clientHeight} ")

            contextMenuPosition.value = pos
            markPosition = Pair(lng, lat)
        }
        0
    }

    val hideContextMenu = {
        contextMenuDisplay.value = DisplayStyle.None
        0
    }

    window.asDynamic()["showContextMenu"] = showContextMenu

    window.asDynamic()["hideContextMenu"] = hideContextMenu

    Div(attrs = {
        classes("uk-inline")
        style {
            position(Position.Fixed)
        }
    }) {
        Div(attrs = {

            id(ID_CONTEXT_MENU)

            attr("uk-dropdown", "")

            style {
                display(contextMenuDisplay.value)
                left(contextMenuPosition.value.first)
                top(contextMenuPosition.value.second)
                position(Position.Absolute)
                minWidth(160.px)
                padding(12.px)
            }

        }) {

            Div(attrs = {
                classes("uk-card")
            }) {

                Ul(attrs = {
                    classes("uk-nav", "uk-nav-default")
                }) {

                    Li(attrs = {
                        classes("uk-nav-header")
                    }) {
                        Text("算路")
                    }

                    arrayOf("设置为起点" to {
                        addStartMarker(markPosition.first, markPosition.second)
                    }, "设置为终点" to {
                        addEndMarker(markPosition.first, markPosition.second)
                    }, "设置为途经点" to {
                        addViaMarker(markPosition.first, markPosition.second)
                    }, "导出路线" to {
                        val routePoints = exportRoute()
                        if (routePoints.isEmpty()) {
                            showTip("计算路线后再导出")
                        } else {
                            copyToClipboard(routePoints.joinToString("\n") { "${it[0]},${it[1]}" })
                            showTip("路线路径点已经拷贝到剪贴板")
                        }
                        hideContextMenu()
                    }, "清除路线" to {
                        cleanMapRoute()
                    }).forEach { item ->
                        loadContextMenu(item)
                    }

                    Li(attrs = {
                        classes("uk-nav-divider")
                    })

                    Li(attrs = {
                        classes("uk-nav-header")
                    }) {
                        Text("标记")
                    }

                    arrayOf("获取经纬度" to {
                        addPositionMarker(markPosition.first, markPosition.second)
                    }, "清除标记" to {
                        cleanAllLngLatMarker()
                    }).forEach { item ->
                        loadContextMenu(item)
                    }

                    Li(attrs = {
                        classes("uk-nav-divider")
                    })

                    Li(attrs = {
                        classes("uk-nav-header")
                    }) {
                        Text("测试朗读范围")
                    }

                    arrayOf("选择位置" to {
                        hideContextMenu()
                        tryAddSpeakTestScope(markPosition.first, markPosition.second)
                    }, "清除所有位置" to {
                        removeAllSpeakTestScope()
                    }).forEach { item ->
                        loadContextMenu(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun loadContextMenu(item: Pair<String, () -> Any>) = Li(attrs = {
    onClick {
        item.second.invoke()
    }
    style {
        lineHeight(1.2.em)
    }
}) {
    A {
        Text(item.first)
    }
}

const val SPEAK_SCOPE_SEP = ";"
const val SCOPE_PARAM = "scope.param"

private fun tryAddSpeakTestScope(lng: Double, lat: Double): Boolean {
    val text = window.prompt("设置朗读字数与速度[字数;速度]", getItem(SCOPE_PARAM, "120${SPEAK_SCOPE_SEP}30"))
    if (text.isNullOrEmpty()) {
        return false
    }
    val scopePramArray = text.split(SPEAK_SCOPE_SEP)
    if (scopePramArray.size < 2) {
        window.alert("请输入正确的参数，例如120;30")
        return false
    }
    val count = scopePramArray[0].toInt()
    val speed = scopePramArray[1].toInt()

    saveItem(SCOPE_PARAM, text)

    if (count > 0 && speed > 0) {
        addSpeakTestScope(lng, lat, count, speed)
        return true
    }

    return false
}

private fun removeAllSpeakTestScope() {
    clearTestScope()
}

private fun removeRouteResult() {
    window.document.getElementById(ID_RESULT).remove()
}


const val DEF_PROBE_RANGE = 50
const val DEF_SPEAK_RANGE = 60

private fun nextCacheDataIndex(): Int {
    return (CacheContent.keys.maxOrNull() ?: -1) + 1
}

fun loadDotMarkerContent(
    title: String, content: String, placeHolder: String, probe: Int, speak: Int, color: String
) {
    loadMarkerContent(nextCacheDataIndex(), title, content, placeHolder, probe, speak, color, true)
}

fun loadLineMarkerContent(
    title: String, content: String, placeHolder: String, color: String
) {
    loadMarkerContent(nextCacheDataIndex(), title, content, placeHolder, -1, -1, color, false)
}

fun CacheData.loadMarkerContent(refresh: Boolean) {

    loadMarkerContent(
        this.index,
        this.obj.title,
        this.obj.content,
        "",
        this.obj.probeRange,
        this.obj.speakRange,
        this.obj.color,
        this.type == TYPE_DOT,
        refresh
    )

}

fun loadMarkerContent(
    index: Int,
    title: String,
    contentInput: String,
    placeHolder: String,
    probe: Int,
    speak: Int,
    color: String,
    isDot: Boolean,
    refreshElement: Boolean = true
) {
    var content: String = contentInput
    var probeRange: Int = if (probe > 0) probe else DEF_PROBE_RANGE
    var speakRange: Int = if (speak > 0) speak else DEF_SPEAK_RANGE
    var colorInput = color
    var titleState :String = title

    closeMarkerContent()
    renderComposable(ID_CONTENT) {
        Div(attrs = {
            id(ID_DOT_CONTENT)
            classes("left_wrapper", "dot_input_wrapper", "uk-card", "uk-card-default")
        }) {

            Div(attrs = {
                classes("uk-card-header")
            }) {
                Div(attrs = {
                    classes("uk-grid-small", "uk-flex-middle")
                    attr("uk-grid", "")
                }) {
                    Div(attrs = {
                        classes("uk-width-expand")
                    }) {
                        Input(InputType.Text, attrs = {
                            classes("uk-search-input")
                            defaultValue(titleState)
                            onChange {
                                titleState = it.value
                            }
                        })
                    }
                    Div(attrs = {
                        classes("uk-width-auto")
                    }) {
                        Button(attrs = {
                            classes("uk-close-large")
                            attr("uk-close", "")
                            style {
                                padding(10.px)
                            }
                            onClick {
                                closeMarkerContent()
                            }
                        }) {

                        }
                    }
                }

            }

            Div(attrs = {
                classes("uk-card-body")
            }) {

                TextArea(attrs = {
                    classes("dot_input_area")
                    id(ID_CONTENT_TEXT_AREA)
                    placeholder(placeHolder)
                    onChange {
                        content = it.value
                    }
                })

                if (isDot) {
                    Div(attrs = {
                        classes("setting-box")
                    }) {
                        Span(attrs = {
                            classes("uk-text-lead", "uk-text-default")
                        }) {
                            Text("探测范围")
                        }
                        Span(attrs = {
                            classes("uk-text-meta")
                        }) { Text("（产品确认）") }
                        Input(InputType.Number, attrs = {
                            classes("uk-input", "uk-form-width-medium", "uk-form-small")
                            placeholder("米")
                            defaultValue(probeRange)
                            onChange {
                                probeRange = (it.value ?: 0).toInt()
                            }
                        })
                    }

                    Div(attrs = {
                        classes("setting-box")
                    }) {
                        Span(attrs = {
                            classes("uk-text-lead", "uk-text-default")
                        }) {
                            Text("朗读范围")
                        }
                        Span(attrs = {
                            classes("uk-text-meta")
                        }) { Text("（带标识圈）") }
                        Input(InputType.Number, attrs = {
                            classes("uk-input", "uk-form-width-medium", "uk-form-small")
                            defaultValue(speakRange)
                            placeholder("千米/小时")
                            onChange {
                                speakRange = (it.value ?: 0).toInt()
                            }
                        })
                    }
                }

                Div(attrs = {
                    classes("setting-box")
                }) {
                    Input(InputType.Text) {
                        id(ID_COLOR_PICKER_INPUT)
                        onInput {
                            colorInput = it.value
                        }
                    }
                }

                Div(attrs = {
                    classes("uk-button-group", "control_group")
                }) {
                    Button(attrs = {
                        classes("uk-button", "uk-button-primary")
                        onClick {

                            if (content.isEmpty()) {
                                window.alert("未输入正确的内容")
                                return@onClick
                            }

                            if (isDot) {
                                submitDotContent(
                                    index,
                                    titleState,
                                    content,
                                    probeRange,
                                    speakRange,
                                    colorInput,
                                    refreshElement
                                )
                            } else {
                                submitLineContent(index, titleState, content, colorInput, refreshElement)
                            }

                            closeMarkerContent()
                        }
                    }) {
                        Text("确定")
                    }
                }

            }
        }
    }

    initJsColor(document.getElementById(ID_COLOR_PICKER_INPUT)!!, colorInput)
    fillElement(document.getElementById(ID_CONTENT_TEXT_AREA)!!, content)
}


private val splitRegex = Regex("[,\\s]+")

val CacheContent = mutableMapOf<Int, CacheData>()

fun submitDotContent(
    index: Int,
    title: String,
    content: String,
    probeRange: Int,
    speakRange: Int,
    color: String,
    refreshElement: Boolean
) {

    CacheContent[index]?.let { item ->
        item.obj.title = title
        item.obj.content = content
        item.obj.probeRange = probeRange
        item.obj.speakRange = speakRange
        item.obj.color = color

        ITEM_MAP[index]?.let {
            it.value = ContentItem(color)
        }

        if (refreshElement) {
            item.refreshMapElement()
        }

        refreshContentItem(index)

        return
    }

    val markerResult = makeDotMapElement(index, content, probeRange, speakRange, color)

    CacheData(
        index,
        TYPE_DOT,
        CacheShape(makeNewTitle(index,title, TYPE_DOT), content, probeRange, speakRange, color, markerResult)
    ).also {
        CacheContent[index] = it
        renderNewContentItem(it)
    }
}

private fun deleteCacheData(index: Int) {
    log("delete data $index")
    CacheContent[index]?.let {
        CacheContent.remove(index)
        it.removeMapElement()
        window.document.getElementById("$ID_CONTENT_LIST_ITEM$index").remove()
    }
}

fun CacheData.refreshMapElement() {
    this.removeMapElement()
    this.makeMapElement()
}

fun CacheData.toggleMapElement(open: Boolean) {
    if (open) {
        this.makeMapElement()
    } else {
        this.removeMapElement()
    }
}

fun CacheData.removeMapElement() {
    removeMapElementArray(this.obj.markerResult)
}

fun CacheData.makeMapElement() {
    if (this.type == TYPE_DOT) {
        this.obj.markerResult =
            makeDotMapElement(
                this.index,
                this.obj.content,
                this.obj.probeRange,
                this.obj.speakRange,
                this.obj.color,
                false
            )
    } else {
        this.obj.markerResult = makeLineMapElement(this.obj.content, this.obj.color, false)
    }
}

private fun makeDotMapElement(
    index: Int, content: String, probeRange: Int, speakRange: Int, color: String, moveMap: Boolean = true
): Array<Any> {

    val dotsArray = mutableListOf<Dot>()

    val lines = content.split("\n")

    val icon = centerMarkerImage(index, 20, 20, color.removePrefix("#"))

    lines.forEach {
        if (it.isNotBlank()) {
            val textArray = it.split(splitRegex)
            try {

                val dot = Dot(
                    textArray[0].toDouble(),
                    textArray[1].toDouble(),
                    textArray.getOrNull(2).orEmpty(),
                    textArray.getOrNull(3)?.toIntOrNull() ?: -1,
                    textArray.getOrNull(4)?.toDoubleOrNull() ?: -1.0
                )

                dotsArray.add(dot)

            } catch (ex: Exception) {
                log(ex.toString())
            }
        }
    }

    if (dotsArray.isEmpty()) {
        return emptyArray()
    }

    return drawMapDotElements(
        Json.encodeToString(dotsArray.toTypedArray()),
        probeRange,
        speakRange,
        color,
        icon,
        moveMap
    )
}

fun submitLineContent(index: Int, title:String, content: String, color: String, refreshElement: Boolean) {

    CacheContent[index]?.let {
        it.obj.title = title
        it.obj.content = content
        it.obj.color = color
        if (refreshElement) {
            it.refreshMapElement()
        }
        refreshContentItem(index)
        return
    }

    val markerResult = makeLineMapElement(content, color)

    CacheContent[index] = CacheData(
        index, TYPE_LINE, CacheShape(makeNewTitle(index, title, TYPE_LINE), content, 0, 0, color, arrayOf(markerResult))
    ).also {
        renderNewContentItem(it)
    }
}

private fun makeLineMapElement(content: String, color: String, moveMap: Boolean = true): Array<Any> {

    val dotsArray = mutableListOf<Line>()

    val lines = content.split("\n")

    lines.forEach {
        if (it.isNotBlank()) {
            val textArray = it.split(splitRegex)
            try {

                val dot = Line(
                    textArray[0].toDouble(),
                    textArray[1].toDouble(),
                )

                dotsArray.add(dot)

            } catch (ex: Exception) {
                log(ex.toString())
            }
        }
    }

    if (dotsArray.isEmpty()) {
        return emptyArray()
    }

    return arrayOf(drawMapLineElements(Json.encodeToString(dotsArray.toTypedArray()), color, moveMap))
}

fun makeNewTitle(index: Int, title: String, type: Int): String {
    return title.ifEmpty {
        "@$index. " + when (type) {
            TYPE_DOT -> SUFFIX_DOT
            else -> SUFFIX_LINE
        }
    }
}


fun closeMarkerContent() {
    window.document.getElementById(ID_CONTENT)?.clear()
}

/**
 * 刷新当前某个列表项的
 * 注意，刷新会导致地图元素的显示
 */
private fun refreshContentItem(index:Int) {
    (window.document.getElementById("$ID_CONTENT_LIST_ITEM_TITLE$index") as? HTMLLabelElement)?.let { label ->
        CacheContent[index]?.let {
            label.textContent = it.obj.title
        }
    }
}


fun loadRouteResult(success: Boolean, distance: Any, straight: Double, time: Int, tolls: Int) {

    removeRouteResult()

    renderComposable(ID_ROOT) {
        Div(attrs = {
            id(ID_RESULT)
            classes("uk-inline", "result-wrapper")
        }) {
            Div(attrs = {
                classes("uk-card", "uk-card-body", "uk-card-default")
            }) {
                Article(attrs = {
                    classes("uk-article")
                }) {
                    H1(attrs = {
                        classes("uk-text-lead")
                    }) {

                        Text(
                            if (success) {
                                "路线结果"
                            } else {
                                "路线规划失败，请重试"
                            }
                        )
                    }
                    P {
                        Text(
                            if (success) {
                                "路线长度为${distance}米，约${(((distance as? Int) ?: 0) / 1000)}公里"
                            } else {
                                "错误：$distance"
                            }
                        )

                    }
                    if (success) {
                        P(attrs = {
                            classes("uk-article-meta")
                        }) {
                            Text("直线距离为${straight.toInt()}米，约${(straight / 1000).toInt()}公里")
                        }
                        P {
                            Text(
                                "驾驶时长${(time / 3600).makeTime("小时")}${(time / 60 % 60).makeTime("分钟")}${
                                    (time % 60).makeTime(
                                        "秒"
                                    )
                                }"
                            )
                        }
                        P {
                            Text("大约可以朗读${(time * WORD_PER_SECOND).toInt()}个字")
                        }
                        if (tolls > 0) {
                            P {
                                Text("预计开销${tolls}元")
                            }
                        }
                    }
                }
            }
        }

    }
}

private fun generateRandomColor(): String {
    return "#${(floor(Random.nextDouble() * 16777215).toString()).toInt().toString(16)}"
}