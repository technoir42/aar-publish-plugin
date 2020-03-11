package com.github.technoir42.plugin.aarpublish

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.internal.PluginUnderTestMetadataReading
import org.gradle.util.GradleVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import java.io.File
import java.util.zip.ZipFile

@RunWith(Parameterized::class)
class AarPublishPluginFunctionalTest(private val androidPluginVersion: String, private val gradleVersion: String) {
    private lateinit var mavenRepo: MavenRepo
    private lateinit var projectGenerator: TestProjectGenerator

    @Rule
    @JvmField
    val tempFolder = TemporaryFolder()
    private lateinit var projectDir: File

    @Before
    fun setUp() {
        projectDir = tempFolder.newFolder("project")
        mavenRepo = MavenRepo(tempFolder.newFolder("maven"))

        val pluginClasspath = PluginUnderTestMetadataReading.readImplementationClasspath()
            .asSequence()
            .map { it.escapedPath }
            .joinToString(", ") { "'$it'" }

        projectGenerator = TestProjectGenerator(
            pluginClasspath = pluginClasspath,
            androidPluginVersion = androidPluginVersion,
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
        val result = buildAndPublish()

        assertEquals(TaskOutcome.SUCCESS, result.task(":packageReleaseJavadoc")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":packageReleaseSources")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":publish")?.outcome)
        verifyArtifacts(javadoc = true, sources = true)
    }

    @Test
    fun publishWithJavadoc() {
        projectGenerator.configureAarPublishing(publishJavadoc = true, publishSources = false)

        val result = buildAndPublish()

        assertNull(result.task(":packageReleaseSources"))
        assertEquals(TaskOutcome.SUCCESS, result.task(":packageReleaseJavadoc")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":publish")?.outcome)
        verifyArtifacts(javadoc = true, sources = false)
    }

    @Test
    fun publishWithSources() {
        projectGenerator.configureAarPublishing(publishJavadoc = false, publishSources = true)

        val result = buildAndPublish()

        assertNull(result.task(":packageReleaseJavadoc"))
        assertEquals(TaskOutcome.SUCCESS, result.task(":packageReleaseSources")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":publish")?.outcome)
        verifyArtifacts(javadoc = false, sources = true)
    }

    @Test
    fun publishWithoutJavadocAndSources() {
        projectGenerator.configureAarPublishing(publishJavadoc = false, publishSources = false)

        val result = buildAndPublish()

        assertNull(result.task(":packageReleaseJavadoc"))
        assertNull(result.task(":packageReleaseSources"))
        assertEquals(TaskOutcome.SUCCESS, result.task(":publish")?.outcome)
        verifyArtifacts(javadoc = false, sources = false)
    }

    private fun buildAndPublish(): BuildResult {
        return GradleRunner.create()
            .withGradleVersion(gradleVersion)
            .withProjectDir(projectDir)
            .withArguments("publish", "--stacktrace")
            .forwardOutput()
            .build()
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
        assertThat(project.dependencies).containsExactlyInAnyOrder(
            PomDependency(groupId = "org.apache.commons", artifactId = "commons-collections4", version = "4.3", scope = "compile"),
            PomDependency(groupId = "commons-io", artifactId = "commons-io", version = "2.6", scope = "runtime"),
            PomDependency(groupId = "org.apache.commons", artifactId = "commons-lang3", version = "3.8", scope = "runtime")
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

    companion object {
        private const val groupId = "com.test"
        private const val artifactId = "mylib"
        private const val version = "1.0.0"
        private const val packageName = "com.test.mylib"

        private val AGP_VERSIONS = arrayOf(
            AndroidPluginVersion("3.3.2", minGradleVersion = "4.10.1"),
            AndroidPluginVersion("3.4.2", minGradleVersion = "5.1.1"),
            AndroidPluginVersion("3.5.3", minGradleVersion = "5.4.1"),
            AndroidPluginVersion("3.6.1", minGradleVersion = "5.6.4")
        )
        private val GRADLE_VERSIONS = arrayOf(
            GradleVersion.version("5.3.1"),
            GradleVersion.version("5.6.4")
        )

        @JvmStatic
        @Parameters(name = "AGP: {0}, Gradle: {1}")
        fun params(): List<Array<Any>> {
            return AGP_VERSIONS.asSequence()
                .flatMap { androidPluginVersion ->
                    GRADLE_VERSIONS.asSequence()
                        .filter { gradleVersion -> gradleVersion >= androidPluginVersion.minGradleVersion }
                        .map { gradleVersion -> arrayOf<Any>(androidPluginVersion.version, gradleVersion.version) }
                }
                .toList()
        }
    }
}
