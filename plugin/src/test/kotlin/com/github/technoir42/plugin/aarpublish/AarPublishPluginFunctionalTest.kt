package com.github.technoir42.plugin.aarpublish

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
    }

    @Test
    fun `publish single variant`() {
        projectGenerator.generate("developmentDebug", publishJavadoc = true, publishSources = true)

        val result = buildAndPublish()

        assertEquals(TaskOutcome.SUCCESS, result.task(":javadocDevelopmentDebugJar")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":sourcesDevelopmentDebugJar")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":publish")?.outcome)
        verifyJavadoc(true, Variant.DevelopmentDebug, "javadoc")
        verifySources(true, Variant.DevelopmentDebug, "sources")
    }

    // TODO: Try to consume an actual library published with .all
    @Test
    fun `publish multiple variants`() {
        projectGenerator.generate("all", publishJavadoc = true, publishSources = true)

        val result = buildAndPublish()

        assertEquals(TaskOutcome.SUCCESS, result.task(":javadocDevelopmentDebugJar")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":sourcesDevelopmentDebugJar")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":javadocDevelopmentReleaseJar")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":sourcesDevelopmentReleaseJar")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":javadocProductionDebugJar")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":sourcesProductionDebugJar")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":javadocProductionReleaseJar")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":sourcesProductionReleaseJar")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":publish")?.outcome)
        Variant.values().forEach {
            verifyJavadoc(true, it, "${it.variantName}-javadoc")
            verifySources(true, it, "${it.variantName}-sources")
        }
    }

    private fun buildAndPublish(): BuildResult {
        return GradleRunner.create()
            .withGradleVersion(gradleVersion)
            .withProjectDir(projectDir)
            .withArguments("publish", "--stacktrace")
            .forwardOutput()
            .build()
    }

    private fun verifyJavadoc(exists: Boolean, variant: Variant, classifier: String) {
        val javadocJar = mavenRepo.getArtifactPath(groupId, artifactId, version, extension = "jar", classifier = classifier)
        if (exists) {
            assertTrue(javadocJar.exists())
            verifyJavadocJar(javadocJar, variant)
        } else {
            assertFalse(javadocJar.exists())
        }
    }

    // TODO: Merge both into one
    private fun verifySources(exists: Boolean, variant: Variant, classifier: String) {
        val sourcesJar = mavenRepo.getArtifactPath(groupId, artifactId, version, extension = "jar", classifier = classifier)
        if (exists) {
            assertTrue(sourcesJar.exists())
            verifySourcesJar(sourcesJar, variant)
        } else {
            assertFalse(sourcesJar.exists())
        }
    }

    private fun verifyJavadocJar(javadocJar: File, variant: Variant) {
        ZipFile(javadocJar).use { zip ->
            val packagePath = packageNameToPath(packageName)
            assertNotNull(zip.getEntry("$packagePath/Main.html"))
            Variant.values().forEach {
                val flavorSpecific = zip.getEntry("$packagePath/${it.flavor.capitalize()}Only.html")
                if (it.flavor == variant.flavor) {
                    assertNotNull(flavorSpecific)
                } else {
                    assertNull(flavorSpecific)
                }
                val buildTypeSpecific = zip.getEntry("$packagePath/${it.buildType.capitalize()}Only.html")
                if (it.buildType == variant.buildType) {
                    assertNotNull(buildTypeSpecific)
                } else {
                    assertNull(buildTypeSpecific)
                }
            }
        }
    }

    private fun verifySourcesJar(sourcesJar: File, variant: Variant) {
        ZipFile(sourcesJar).use { zip ->
            val packagePath = packageNameToPath(packageName)
            assertNotNull(zip.getEntry("$packagePath/Main.java"))
            Variant.values().forEach {
                val flavorSpecific = zip.getEntry("$packagePath/${it.flavor.capitalize()}Only.java")
                if (it.flavor == variant.flavor) {
                    assertNotNull(flavorSpecific)
                } else {
                    assertNull(flavorSpecific)
                }
                val buildTypeSpecific = zip.getEntry("$packagePath/${it.buildType.capitalize()}Only.java")
                if (it.buildType == variant.buildType) {
                    assertNotNull(buildTypeSpecific)
                } else {
                    assertNull(buildTypeSpecific)
                }
            }
        }
    }

    private enum class Variant(val flavor: String, val buildType: String) {
        DevelopmentDebug("development", "debug"),
        DevelopmentRelease("development", "release"),
        ProductionDebug("production", "debug"),
        ProductionRelease("production", "release");

        val variantName: String
            get() = flavor + buildType.capitalize()
    }

    companion object {
        private const val groupId = "com.test"
        private const val artifactId = "mylib"
        private const val version = "1.0.0"
        private const val packageName = "com.test.mylib"

        private val AGP_VERSIONS = arrayOf(
            AndroidPluginVersion("3.6.1", minGradleVersion = "5.6.4"),
            AndroidPluginVersion("4.0.1", minGradleVersion = "6.1.1"),
            AndroidPluginVersion("4.1.0-rc03", minGradleVersion = "6.2.1")
        )
        private val GRADLE_VERSIONS = arrayOf(
            GradleVersion.version("5.6.4"),
            GradleVersion.version("6.6.1")
        )

        @JvmStatic
        @Parameters(name = "AGP: {0}, Gradle: {1}")
        fun params(): List<Array<Any>> {
            return AGP_VERSIONS
                .map { androidPluginVersion ->
                    val gradleVersion = GRADLE_VERSIONS.first { it >= androidPluginVersion.minGradleVersion }
                    arrayOf<Any>(androidPluginVersion.version, gradleVersion.version)
                }
        }
    }
}
