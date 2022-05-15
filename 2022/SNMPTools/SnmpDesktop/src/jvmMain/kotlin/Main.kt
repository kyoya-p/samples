// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.io.File
import java.util.*
import kotlin.reflect.KProperty

@ExperimentalCoroutinesApi
fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "SNMP Agent Desktop") { WinApp(window) }
}

class FileProperty(val prop: Properties = Properties(), val propFile: File = File("build/my.properties")) {

    fun <T> T.applyIf(t: Boolean, op: T.() -> Unit) = if (t) apply { op() } else this
    val loadProp get() = prop.applyIf(propFile.exists()) { load(propFile.inputStream()) }
    fun setAndStore(k:String,v: String) = loadProp.setProperty(k, v)
    operator fun getValue(r: String?, p: KProperty<*>) = loadProp.getProperty(p.name)
    operator fun setValue(r: String?, p: KProperty<*>, v: String) = setAndStore(v)
    loadProp.setProperty(p.name, v).store(propFile.outputStream(), "Comment")
}

