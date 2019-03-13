package com.github.technoir42.plugin.aarpublish

import java.io.File

class MavenRepo(val path: File) {
    fun getArtifactPath(groupId: String, artifactId: String, version: String, extension: String, classifier: String? = null): File {
        return File(getArtifactDir(groupId, artifactId, version), getArtifactName(artifactId, version, extension, classifier))
    }

    private fun getArtifactDir(groupId: String, artifactId: String, version: String): File {
        return File(path, "${groupId.replace('.', '/')}/$artifactId/$version")
    }

    private fun getArtifactName(artifactId: String, version: String, extension: String, classifier: String?): String {
        val classifierSuffix = if (classifier != null) "-$classifier" else ""
        return "$artifactId-$version$classifierSuffix.$extension"
    }
}
