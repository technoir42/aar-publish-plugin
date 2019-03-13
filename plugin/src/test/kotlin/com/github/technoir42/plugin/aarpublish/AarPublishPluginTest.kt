package com.github.technoir42.plugin.aarpublish

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.ZipFile

class AarPublishPluginTest {
    private val groupId = "com.test"
    private val artifactId = "mylib"
    private val version = "1.0.0"
    private val packageName = "com.test.mylib"

    private lateinit var mavenRepo: MavenRepo
    private lateinit var projectGenerator: TestProjectGenerator

    @TempDir
    lateinit var projectDir: File

    @BeforeEach
    fun setUp() {
        mavenRepo = MavenRepo(projectDir.newDirectory("maven"))

        projectGenerator = TestProjectGenerator(
            projectDir = projectDir,
            mavenRepoDir = mavenRepo.path,
            groupId = groupId,
            artifactId = artifactId,
            version = version,
            packageName = packageName
        )
        projectGenerator.generate()
    }

    @Test
    fun publish() {
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("publish")
            .build()

        assertTrue(result.task(":publish")?.outcome == TaskOutcome.SUCCESS)
        verifyArtifacts(javadoc = true, sources = true)
    }

    @Test
    fun publishWithJavadoc() {
        projectGenerator.configureAarPublishing(publishJavadoc = true, publishSources = false)

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("publish")
            .build()

        assertTrue(result.task(":publish")?.outcome == TaskOutcome.SUCCESS)
        verifyArtifacts(javadoc = true, sources = false)
    }

    @Test
    fun publishWithSources() {
        projectGenerator.configureAarPublishing(publishJavadoc = false, publishSources = true)

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("publish")
            .build()

        assertTrue(result.task(":publish")?.outcome == TaskOutcome.SUCCESS)
        verifyArtifacts(javadoc = false, sources = true)
    }

    @Test
    fun publishWithoutJavadocAndSources() {
        projectGenerator.configureAarPublishing(publishJavadoc = false, publishSources = false)

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("publish")
            .build()

        assertTrue(result.task(":publish")?.outcome == TaskOutcome.SUCCESS)
        verifyArtifacts(javadoc = false, sources = false)
    }

    private fun verifyArtifacts(javadoc: Boolean, sources: Boolean) {
        val pom = mavenRepo.getArtifactPath(groupId, artifactId, version, extension = "pom")
        assertTrue(pom.exists())
        verifyPom(pom)

        val aar = mavenRepo.getArtifactPath(groupId, artifactId, version, extension = "aar")
        assertTrue(aar.exists())

        val javadocJar = mavenRepo.getArtifactPath(groupId, artifactId, version, extension = "jar", classifier = "javadoc")
        if (javadoc) {
            assertTrue(javadocJar.exists())
            verifyJavadocJar(javadocJar)
        } else {
            assertFalse(javadocJar.exists())
        }

        val sourcesJar = mavenRepo.getArtifactPath(groupId, artifactId, version, extension = "jar", classifier = "sources")
        if (sources) {
            assertTrue(sourcesJar.exists())
            verifySourcesJar(sourcesJar)
        } else {
            assertFalse(sourcesJar.exists())
        }
    }

    private fun verifyPom(pomFile: File) {
        val mapper = XmlMapper().apply {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
        val project = mapper.readValue<PomProject>(pomFile, PomProject::class.java)

        assertEquals(groupId, project.groupId)
        assertEquals(artifactId, project.artifactId)
        assertEquals(version, project.version)
        assertEquals("aar", project.packaging)
        assertEquals(
            listOf(
                PomDependency(groupId = "commons-io", artifactId = "commons-io", version = "2.6"),
                PomDependency(
                    groupId = "org.apache.commons",
                    artifactId = "commons-lang3",
                    version = "3.8",
                    scope = "runtime"
                )
            ),
            project.dependencies
        )
    }

    private fun verifyJavadocJar(jar: File) {
        ZipFile(jar).use { zip ->
            val htmlFile = zip.getEntry("${packageNameToPath(packageName)}/Foo.html")
            assertNotNull(htmlFile)
        }
    }

    private fun verifySourcesJar(jar: File) {
        ZipFile(jar).use { zip ->
            val sourceFile = zip.getEntry("${packageNameToPath(packageName)}/Foo.java")
            assertNotNull(sourceFile)
        }
    }
}
