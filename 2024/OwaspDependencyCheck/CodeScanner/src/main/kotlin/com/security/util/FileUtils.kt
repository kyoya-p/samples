package com.security.util

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object FileUtils {
    fun createDirectoryIfNotExists(path: String) {
        val directory = File(path)
        if (!directory.exists()) {
            directory.mkdirs()
        }
    }
    
    fun ensureValidPath(path: String): Path {
        return Paths.get(path).normalize()
    }
    
    fun isJarFile(path: String): Boolean {
        return path.lowercase().endsWith(".jar")
    }
}