package com.github.technoir42.plugin.aarpublish

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.util.zip.ZipFile

@RunWith(Parameterized::class)
class AarPublishPluginTest(private val gradleVersion: String, private val androidPluginVersion: String) {
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

        projectGenerator = TestProjectGenerator(
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

        assertTrue(result.task(":publish")?.outcome == TaskOutcome.SUCCESS)
        verifyArtifacts(javadoc = true, sources = true)
    }

    @Test
    fun publishWithJavadoc() {
        projectGenerator.configureAarPublishing(publishJavadoc = true, publishSources = false)

        val result = buildAndPublish()

        assertTrue(result.task(":publish")?.outcome == TaskOutcome.SUCCESS)
        verifyArtifacts(javadoc = true, sources = false)
    }

    @Test
    fun publishWithSources() {
        projectGenerator.configureAarPublishing(publishJavadoc = false, publishSources = true)

        val result = buildAndPublish()

        assertTrue(result.task(":publish")?.outcome == TaskOutcome.SUCCESS)
        verifyArtifacts(javadoc = false, sources = true)
    }

    @Test
    fun publishWithoutJavadocAndSources() {
        projectGenerator.configureAarPublishing(publishJavadoc = false, publishSources = false)

        val result = buildAndPublish()

        assertTrue(result.task(":publish")?.outcome == TaskOutcome.SUCCESS)
        verifyArtifacts(javadoc = false, sources = false)
    }

    private fun buildAndPublish(): BuildResult {
        return GradleRunner.create()
            .withGradleVersion(gradleVersion)
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("publish")
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
        private val GRADLE_VERSIONS = arrayOf("5.3.1", "5.4")
        private val AGP_VERSIONS = arrayOf("3.3.2", "3.4.0", "3.5.0-alpha12")

        @JvmStatic
        @Parameterized.Parameters(name = "Gradle: {0}, AGP: {1}")
        fun params(): List<Array<Any>> {
            return GRADLE_VERSIONS.asSequence()
                .flatMap { gradleVersion ->
                    AGP_VERSIONS.asSequence().map { androidPluginVersion -> arrayOf<Any>(gradleVersion, androidPluginVersion) }
                }
                .toList()
        }
    }
}
