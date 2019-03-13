package com.github.technoir42.plugin.aarpublish

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

@JacksonXmlRootElement(localName = "project")
data class PomProject(
    var groupId: String? = null,
    var artifactId: String? = null,
    var version: String? = null,
    var packaging: String = "jar",
    var dependencies: List<PomDependency> = emptyList()
)

data class PomDependency(
    var groupId: String? = null,
    var artifactId: String? = null,
    var version: String? = null,
    var scope: String = "compile"
)
