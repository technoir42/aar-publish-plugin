package com.github.technoir42.plugin.aarpublish

import java.io.File

fun File.newFile(path: String): File {
    val file = File(this, path)
    file.parentFile.mkdirs()
    return file
}

fun packageNameToPath(packageName: String): String {
    return packageName.replace('.', '/')
}
