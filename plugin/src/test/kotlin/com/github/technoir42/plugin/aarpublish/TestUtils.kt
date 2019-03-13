package com.github.technoir42.plugin.aarpublish

import java.io.File

fun File.newDirectory(path: String): File {
    val dir = File(this, path)
    dir.mkdirs()
    return dir
}

fun File.newFile(path: String): File {
    val file = File(this, path)
    file.parentFile.mkdirs()
    return file
}

fun packageNameToPath(packageName: String): String {
    return packageName.replace('.', '/')
}
