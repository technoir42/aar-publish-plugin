package com.github.technoir42.plugin.aarpublish

import org.gradle.util.GradleVersion

data class AndroidPluginVersion(
    val version: String,
    val minGradleVersion: GradleVersion
) {
    constructor(version: String, minGradleVersion: String) : this(version, GradleVersion.version(minGradleVersion))
}
