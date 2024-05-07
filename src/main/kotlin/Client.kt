import kotlinx.browser.window
import org.w3c.dom.BeforeUnloadEvent


fun main() {
    window.onload = { initProject() }
    window.onbeforeunload = { whenWindowClose(it) }
}

private fun whenWindowClose(event: BeforeUnloadEvent): String? {
    saveWebState()
    return null
}

private fun initProject() {
    initUI()
    initMap()
}
