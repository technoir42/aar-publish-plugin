package com.github.technoir42.plugin.aarpublish

import java.io.File

class TestProjectGenerator(
    private val pluginClasspath: String,
    private val projectDir: File,
    private val mavenRepoDir: File,
    private val groupId: String,
    private val artifactId: String,
    private val version: String,
    private val packageName: String,
    private val androidPluginVersion: String,
    private val apiLevel: Int = 30
) {
    private val buildGradleFile = File(projectDir, "build.gradle")

    fun generate(publishedComponent: String, publishJavadoc: Boolean, publishSources: Boolean) {
        createBuildGradle(publishedComponent)
        createSettingsGradle()
        createLocalProperties()
        createAndroidManifest()
        createSourceFiles()
        configureAarPublishing(publishJavadoc, publishSources)
    }

    private fun configureAarPublishing(publishJavadoc: Boolean, publishSources: Boolean) {
        //language=Groovy
        buildGradleFile.appendText(
            """

            aarPublishing {
                publishJavadoc = $publishJavadoc
                publishSources = $publishSources
            }
        """.trimIndent()
        )
    }

    private fun createBuildGradle(publishedComponent: String) {
        //language=Groovy
        buildGradleFile.writeText(
            """
            buildscript {
                repositories {
                    google()
                    jcenter()
                }
                dependencies {
                    classpath "com.android.tools.build:gradle:$androidPluginVersion"
                    classpath files($pluginClasspath)
                }
            }

            apply plugin: "com.android.library"
            apply plugin: "com.github.technoir42.aar-publish"
            apply plugin: "maven-publish"

            group = "$groupId"
            version = "$version"

            repositories {
                google()
                jcenter()
            }

            android {
                compileSdkVersion $apiLevel
                defaultConfig {
                    targetSdkVersion $apiLevel
                }

                flavorDimensions "environment"
                productFlavors {
                    development {
                        dimension "environment"
                    }
                    production {
                        dimension "environment"
                    }
                }
            }

            publishing {
                publications {
                    maven(MavenPublication) {
                        afterEvaluate {
                            from components.${publishedComponent}
                        }
                    }
                }

                repositories {
                    maven {
                        url "${mavenRepoDir.escapedPath}"
                    }
                }
            }
        """.trimIndent()
        )
    }

    private fun createSettingsGradle() {
        val settingsGradleFile = projectDir.newFile("settings.gradle")
        //language=Groovy
        settingsGradleFile.writeText("rootProject.name = \"$artifactId\"")
    }

    private fun createLocalProperties() {
        val androidHome = System.getenv("ANDROID_HOME")
            ?: throw AssertionError("ANDROID_HOME is not set")

        val localPropertiesFile = projectDir.newFile("local.properties")
        localPropertiesFile.writeText("sdk.dir=${File(androidHome).escapedPath}")
    }

    private fun createAndroidManifest() {
        val manifestFile = projectDir.newFile("src/main/AndroidManifest.xml")
        //language=XML
        manifestFile.writeText(
            """<?xml version="1.0" encoding="utf-8"?>
            <manifest package="$packageName" />
        """.trimIndent()
        )
    }

    private fun createSourceFiles() {
        val packagePath = packageNameToPath(packageName)
        //language=JAVA
        projectDir.newFile("src/main/java/$packagePath/Main.java")
            .writeText(
                """
                    package $packageName;

                    /**
                     * Main class.
                     */
                    public class Main {
                    }
                """.trimIndent()
            )

        //language=JAVA
        projectDir.newFile("src/development/java/$packagePath/DevelopmentOnly.java")
            .writeText(
                """
                    package $packageName;

                    /**
                     * Development-only class.
                     */
                    public class DevelopmentOnly {
                    }
                """.trimIndent()
            )

        //language=JAVA
        projectDir.newFile("src/production/java/$packagePath/ProductionOnly.java")
            .writeText(
                """
                    package $packageName;

                    /**
                     * Production-only class.
                     */
                    public class ProductionOnly {
                    }
                """.trimIndent()
            )

        //language=JAVA
        projectDir.newFile("src/debug/java/$packagePath/DebugOnly.java")
            .writeText(
                """
                    package $packageName;

                    /**
                     * Debug-only class.
                     */
                    public class DebugOnly {
                    }
                """.trimIndent()
            )

        //language=JAVA
        projectDir.newFile("src/release/java/$packagePath/ReleaseOnly.java")
            .writeText(
                """
                    package $packageName;

                    /**
                     * Release-only class.
                     */
                    public class ReleaseOnly {
                    }
                """.trimIndent()
            )
    }
}
