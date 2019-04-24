package com.github.technoir42.plugin.aarpublish

import java.io.File

class TestProjectGenerator(
    private val projectDir: File,
    private val mavenRepoDir: File,
    private val groupId: String,
    private val artifactId: String,
    private val version: String,
    private val packageName: String,
    private val androidPluginVersion: String,
    private val apiLevel: Int = 28
) {
    private val buildGradleFile = File(projectDir, "build.gradle")

    fun generate() {
        createBuildGradle()
        createSettingsGradle()
        createLocalProperties()
        createAndroidManifest()
        createJavaSourceFile()
    }

    fun configureAarPublishing(publishJavadoc: Boolean, publishSources: Boolean) {
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

    private fun createBuildGradle() {
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
                }
            }

            plugins {
                id "com.github.technoir42.aar-publish"
                id "maven-publish"
            }

            apply plugin: "com.android.library"

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
            }

            publishing {
                publications {
                    maven(MavenPublication) {
                        from components.android
                    }
                }

                repositories {
                    maven {
                        url "${mavenRepoDir.path}"
                    }
                }
            }

            dependencies {
                compileOnly "commons-codec:commons-codec:1.12"
                api "org.apache.commons:commons-collections4:4.3"
                implementation "commons-io:commons-io:2.6"
                runtimeOnly "org.apache.commons:commons-lang3:3.8"
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
        localPropertiesFile.writeText("sdk.dir=$androidHome")
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

    private fun createJavaSourceFile() {
        val javaSourceFile = projectDir.newFile("src/main/java/${packageNameToPath(packageName)}/Foo.java")
        //language=JAVA
        javaSourceFile.writeText(
            """
            package $packageName;

            /**
             * Sample class.
             */
            public class Foo {
            }
        """.trimIndent()
        )
    }
}
