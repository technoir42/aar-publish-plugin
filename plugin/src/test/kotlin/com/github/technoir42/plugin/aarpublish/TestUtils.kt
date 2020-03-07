package com.github.technoir42.plugin.aarpublish

import java.io.File

fun File.newFile(path: String): File {
    val file = File(this, path)
    file.parentFile.mkdirs()
    return file
}

val File.escapedPath: String
    get() = absolutePath.replace("\\", "\\\\") // escape backslashes on Windows

fun packageNameToPath(packageName: String): String {
    return packageName.replace('.', '/')
}
