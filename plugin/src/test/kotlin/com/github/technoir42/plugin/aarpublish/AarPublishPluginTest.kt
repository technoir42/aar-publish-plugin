package com.github.technoir42.plugin.aarpublish

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

@Ignore
class AarPublishPluginTest {
    private lateinit var project: Project

    @Before
    fun setUp() {
        project = ProjectBuilder.builder().build()
        project.plugins.apply(LibraryPlugin::class.java)
        project.plugins.apply(AarPublishPlugin::class.java)

        project.extensions.getByType(BaseExtension::class.java).apply {
            compileSdkVersion(30)
        }
    }

    @Test
    fun `no flavors`() {
        project.evaluate()

        val components = project.components.map { it.name }.toList()
        println("")
    }

    @Test
    fun `multiple flavors`() {
        project.evaluate()
    }
}
