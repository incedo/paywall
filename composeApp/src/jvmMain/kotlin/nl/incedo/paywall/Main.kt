package nl.incedo.paywall

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Incedo — Customer portal") {
        App()
    }
}
