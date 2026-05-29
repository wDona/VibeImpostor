package org.example.project

class WasmPlatform: Platform {
    override val name: String = "Web with Kotlin/Wasm"
}

actual fun getPlatform(): Platform = WasmPlatform()

@JsFun("() => Date.now()")
private external fun jsDateNow(): Double

actual fun currentTimeMillis(): Long = jsDateNow().toLong()

@JsFun("""() => {
    window.__clipboard_done = false;
    window.__clipboard_result = null;
    if (navigator.clipboard && navigator.clipboard.readText) {
        navigator.clipboard.readText()
            .then(function(t) { window.__clipboard_result = t; window.__clipboard_done = true; })
            .catch(function() { window.__clipboard_done = true; });
    } else {
        window.__clipboard_done = true;
    }
}""")
private external fun jsStartClipboardRead()

@JsFun("() => window.__clipboard_done === true")
private external fun jsClipboardDone(): Boolean

@JsFun("() => window.__clipboard_result || ''")
private external fun jsGetClipboard(): String

actual suspend fun readClipboard(): String? {
    jsStartClipboardRead()
    repeat(100) {
        kotlinx.coroutines.delay(20)
        if (jsClipboardDone()) {
            return jsGetClipboard().ifEmpty { null }
        }
    }
    return null
}