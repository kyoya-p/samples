package rmmx

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.also
import kotlin.collections.associateWith
import kotlin.collections.first
import kotlin.collections.firstOrNull
import kotlin.collections.forEach
import kotlin.collections.forEachIndexed
import kotlin.collections.plus
import kotlin.collections.toMutableList
import kotlin.jvm.java
import kotlin.let
import kotlin.onFailure
import kotlin.reflect.*
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.runCatching
import kotlin.sequences.forEach
import kotlin.text.startsWith
import kotlin.text.toDoubleOrNull
import kotlin.text.toFloatOrNull
import kotlin.text.toIntOrNull
import kotlin.text.toLongOrNull


@Suppress("UNCHECKED_CAST")
@Composable
fun DynamicForm(
    v: Any?,
    kType: KType,
    label: String? = null,
    isNullable: Boolean = kType.isMarkedNullable,
    labelWidget: @Composable () -> Unit = { label?.let { Text("$it : $kType") } },
    onChange: (Any?) -> Unit = {}
): Unit = Column() {
    fun String.toTyped(kType: KType): Any? = when (kType.classifier) {
        String::class -> this
        Int::class -> toIntOrNull()
        Long::class -> toLongOrNull()
        Double::class -> toDoubleOrNull()
        Float::class -> toFloatOrNull()
        else -> null
    }

    @Composable
    fun CheckNullable1(onNull: () -> Unit) {
        if (isNullable) {
            if (v != null) TextButton(onClick = { onNull() }) { Text("Nullify") }
            else Text(" = null")
        }
    }

    @Composable
    fun CheckNullable2(onNull: () -> Unit = { onChange(null) }, onNonNull: () -> Unit) {
        if (isNullable) {
            Checkbox(
                checked = v == null,
                modifier = Modifier.graphicsLayer(0.7f, 0.7f),
                onCheckedChange = {
                    if (it) onNull() else onNonNull()
                })
            Text("isNull")
        }
    }

    when (kType.classifier) {
        Int::class, Long::class, Double::class, Float::class, String::class -> {
            var text by remember { mutableStateOf("${v ?: ""}") }
            OutlinedTextField(
                value = text,
                onValueChange = { nV ->
                    text = nV
                    nV.toTyped(kType)?.let { onChange(it) }
                },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        labelWidget()
                        CheckNullable1 { onChange(null);text = "" }
                    }
                },
//                enabled = v != null,
                isError = v != null && text.toTyped(kType) == null,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Boolean::class -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = v as Boolean? ?: false,
                    onCheckedChange = { onChange(it) },
                )
                labelWidget()
                CheckNullable1 { onChange(null) }
            }
        }

        List::class -> {
            var ref by remember { mutableStateOf(0) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                labelWidget()
                CheckNullable2 { runCatching { onChange(emptyList<Any>()) }.onFailure { it.printStackTrace() } }
            }
            if (v == null) return@Column

            val eType = kType.arguments.first().type ?: return@Column
            val list = v as List<Any?>
            Column(modifier = Modifier.padding(start = 16.dp)) {
                //AI:アイテムを再コンポーズする際、内容が変化しているため再描画したい
                key(ref) {
                    ref
                    list.forEachIndexed { i, e ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                onChange(list.toMutableList().also { it.removeAt(i) })
                                ref++
                            }) { Icon(Icons.Default.Delete, "Delete") }
                            DynamicForm(e, eType, "$i") { nV ->
                                onChange(list.toMutableList().also { it[i] = nV })
                            }
                        }
                    }
                }
            }
            IconButton(onClick = { onChange(list + makeDefaultInstance(eType)) }) {
                Icon(Icons.Default.Add, "Add")
            }
        }

        else -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                labelWidget()
                CheckNullable2 { onChange(makeDefaultInstance(kType)) }
            }
            if (v == null) return@Column
            val clazz: KClass<*> = v::class
            val properties = clazz.declaredMemberProperties

            //AI: kType が kotlin enum class
            if (kType.classifier == Enum::class) {
                return@Column Text("enum : ${kType.classifier}")
                //TODO
            }

            Column(modifier = Modifier.padding(start = 16.dp)) {
                properties.forEach { property ->
                    val child = property.getter.call(v)
                    DynamicForm(child, property.returnType, label = property.name) { nC ->
//                        val constructor = clazz.primaryConstructor ?: return@DynamicForm
                        val nextProps = clazz.memberProperties.associateWith { prop ->
                            if (prop == property) nC else prop.getter.call(v)
                        } as Map<KProperty<Any?>, Any?>
                        val updatedV = copyInstance(v, nextProps)
                        onChange(updatedV)
                    }
                }
            }
        }

    }
}

fun makeDefaultInstance(kType: KType): Any? {
    val clazz = kType.classifier as KClass<*>
    return when (clazz) {
        String::class -> ""
        Int::class -> 0
        Long::class -> 0L
        Double::class -> 0.0
        Float::class -> 0.0f
        Boolean::class -> false
        List::class -> {
            val elementType = kType.arguments.first().type ?: return emptyList<Any>()
            if (elementType.isMarkedNullable) emptyList<Any?>() else emptyList<Any>()
        }

        else -> {
            if (clazz.java.isEnum) {
                clazz.java.enumConstants.first()
            } else {
                val constructor = clazz.primaryConstructor
                    ?: throw kotlin.IllegalArgumentException("Class ${clazz.simpleName} has no primary constructor")
                val args = constructor.parameters.associateWith { param ->
                    makeDefaultInstance(param.type)
                }
                constructor.callBy(args)
            }
        }
    }
}

/*
任意のインスタンスをコピーする
その際KPropertyで指定されたメンバを別インスタンスで置き換える
*/
fun copyInstance(org: Any?, replacements: Map<KProperty<Any?>, Any?> = emptyMap<KProperty<Any?>, Any?>()): Any? {
    if (org == null) return null
    val kClass = org::class as KClass<Any>
    val constructor = kClass.primaryConstructor ?: return org
    val params = constructor.parameters
    val args = params.associateWith { param ->
        val replacement = replacements.entries.firstOrNull { it.key.name == param.name }
        if (replacement != null) replacement.value
        else kClass.memberProperties.firstOrNull { it.name == param.name }?.getter?.call(org)
    }
    return constructor.callBy(args)
}


class AppDialogScope(val closeDialog: () -> Unit)

@Composable
fun AppDialog(
    title: String? = null,
    text: String = "",
    onConfirmed: suspend () -> Unit = {},
    onDismissed: (suspend () -> Unit)? = null,
    onClosed: (suspend () -> Unit)? = null,
    titleWidget: (@Composable () -> Unit)? = title?.let { { Text(it) } },
    scope: CoroutineScope = rememberCoroutineScope(),
    content: @Composable AppDialogScope.() -> Unit = { },
): () -> Unit {
    var opened by remember { mutableStateOf(false) }
    if (opened) {
        AlertDialog(
            modifier = Modifier.fillMaxWidth(0.85f),
            onDismissRequest = { scope.launch { onClosed?.let { it() }; opened = false } },
            title = titleWidget,
            text = { Column { AppDialogScope({ opened = false }).content() } },
            confirmButton = {
                Button(onClick = {
                    scope.launch { onConfirmed();onClosed?.let { it() }; opened = false }
                }) { Text("OK") }
            },
            dismissButton = onDismissed?.let { dis ->
                { Button({ scope.launch { dis() } }) { Text("Cancel") } }
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        )
    }
    return { opened = true }
}
