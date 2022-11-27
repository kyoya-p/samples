// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.io.File
import java.util.*
import kotlin.reflect.KProperty

val app = AppProperties()

@ExperimentalCoroutinesApi
fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "SNMP Agent Desktop") { WinApp(window) }
}

class AppProperty(
    val prop: Properties = Properties(),
    val propFile: File = File("${System.getProperty("user.home")}/.snmpagent.properties"),
) {
    fun <T> T.applyIf(t: Boolean, op: T.() -> Unit) = if (t) apply { op() } else this
    val loadProp get() = prop.applyIf(propFile.exists()) { load(propFile.inputStream()) }
    fun setAndStore(k: String, v: String) = loadProp.apply { setProperty(k, v) }.store(propFile.outputStream(), "")
    operator fun getValue(r: Any?, p: KProperty<*>) = loadProp.getProperty(p.name)
    operator fun setValue(r: Any?, p: KProperty<*>, v: String) = setAndStore(p.name, v)
}

class AppProperties {
    var ip by AppProperty()
    var port by AppProperty()
    var commStr by AppProperty()
    var mibFile by AppProperty()

    var ipRange  by AppProperty()
}
