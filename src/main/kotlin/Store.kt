import kotlinx.browser.window

/**
 * @author PG.Xie
 * created on 2022/6/17
 */

fun saveItem(key: String, value: String) {
    window.localStorage.setItem(key, value)
}

fun getItem(key: String, def: String): String {
    return window.localStorage.getItem(key) ?: def
}

fun removeItem(key: String) {
    window.localStorage.removeItem(key)
}