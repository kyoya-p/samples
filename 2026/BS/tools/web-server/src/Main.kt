import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import java.io.File

fun setupWebAssets(staticDir: File) {
    if (!staticDir.exists()) {
        staticDir.mkdirs()
    }
    
    val srcDir = File("web-gameboard/src")
    if (srcDir.exists()) {
        srcDir.listFiles()?.forEach { file ->
            if (file.isFile && (file.name == "index.html" || file.name.startsWith("skiko."))) {
                val targetFile = File(staticDir, file.name)
                file.copyTo(targetFile, overwrite = true)
                println("Synchronized ${file.name} to ${targetFile.absolutePath}")
            }
        }
    }
    
    val mjsFile = File(staticDir, "web-gameboard.mjs")
    if (mjsFile.exists()) {
        val glueCode = """
import defaultInit from './skiko.mjs';
import * as _ref_QGpzLWpvZGEvY29yZQ_ from '@js-joda/core';
import { instantiate } from './web-gameboard.uninstantiated.mjs';

const skiko = await defaultInit();

const stubbedCalls = new Set();
let dummyHandleId = 1000;
let recorderMap = new Map();

function getOrCreateRecorder(nodeId) {
    if (!recorderMap.has(nodeId)) {
        const ptr = skiko.wasmExports.org_jetbrains_skia_PictureRecorder__1nMake();
        recorderMap.set(nodeId, { recorder: ptr, picture: null, canvas: null });
    }
    return recorderMap.get(nodeId);
}

const customImpls = {
    org_jetbrains_skiko_node_RenderNodeKt_RenderNode_1nBeginRecording(nodeId) {
        const item = getOrCreateRecorder(nodeId);
        const canvas = skiko.wasmExports.org_jetbrains_skia_PictureRecorder__1nBeginRecording(item.recorder, 0, 0, 4000, 4000);
        item.canvas = canvas;
        return canvas;
    },
    org_jetbrains_skiko_node_RenderNodeKt_RenderNode_1nEndRecording(nodeId) {
        const item = getOrCreateRecorder(nodeId);
        if (item && item.recorder) {
            item.picture = skiko.wasmExports.org_jetbrains_skia_PictureRecorder__1nFinishRecordingAsPicture(item.recorder);
        }
        return 0;
    },
    org_jetbrains_skiko_node_RenderNodeKt_RenderNode_1nDrawInto(nodeId, canvasPtr) {
        const item = getOrCreateRecorder(nodeId);
        if (item && item.picture) {
            skiko.wasmExports.org_jetbrains_skia_Canvas__1nDrawPicture(canvasPtr, item.picture);
        }
        return 0;
    }
};

const skikoHandler = {
    get(target, prop) {
        if (typeof prop === 'string') {
            if (prop in customImpls) {
                return customImpls[prop];
            }
            if (prop in target && target[prop] !== undefined) {
                return typeof target[prop] === 'function' ? target[prop].bind(target) : target[prop];
            }
            if (target.wasmExports && prop in target.wasmExports && target.wasmExports[prop] !== undefined) {
                return target.wasmExports[prop];
            }
            if (target['_' + prop] && typeof target['_' + prop] === 'function') {
                return target['_' + prop].bind(target);
            }
            if (target.wasmExports && target.wasmExports['_' + prop]) {
                return target.wasmExports['_' + prop];
            }
            return (...args) => {
                if (!stubbedCalls.has(prop)) {
                    stubbedCalls.add(prop);
                    console.log(`[STUBBED SKIKO FUNCTION]: ${"$"}{prop}`, args);
                }
                if (prop.includes('nMake') || prop.includes('Make') || prop.includes('Create') || prop.includes('Context')) {
                    return ++dummyHandleId;
                }
                return 0;
            };
        }
        return target[prop];
    }
};

const skikoProxy = new Proxy(skiko, skikoHandler);

const exports = (await instantiate({
    './skiko.mjs': skikoProxy,
    '@js-joda/core': _ref_QGpzLWpvZGEvY29yZQ_
})).exports;

export const {
    memory,
    _initialize
} = exports;
""".trimIndent()
        mjsFile.writeText(glueCode)
        println("Synchronized and patched web-gameboard.mjs")
    }
}

fun main() {
    println("Starting Ktor Server on port 8080...")
    
    val staticDir = File("build/tasks/_web-gameboard_linkWasmJs")
    setupWebAssets(staticDir)
    
    println("Serving static files from: ${staticDir.absolutePath}")
    
    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            json()
        }
        
        routing {
            staticFiles("/", staticDir) {
                default("index.html")
                contentType { file ->
                    when (file.extension.lowercase()) {
                        "mjs", "js" -> ContentType.Application.JavaScript
                        "wasm" -> ContentType("application", "wasm")
                        "html" -> ContentType.Text.Html
                        "css" -> ContentType.Text.CSS
                        "json" -> ContentType.Application.Json
                        else -> null
                    }
                }
            }
            
            get("/api/status") {
                call.respond(mapOf("status" to "ok"))
            }
        }
    }.start(wait = true)
}
