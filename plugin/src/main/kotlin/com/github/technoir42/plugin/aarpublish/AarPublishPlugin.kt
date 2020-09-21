package com.github.technoir42.plugin.aarpublish

import com.android.build.api.attributes.BuildTypeAttr
import com.android.build.api.attributes.ProductFlavorAttr
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.AndroidSourceSet
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.LibraryVariant
import com.android.build.gradle.api.SourceKind
import com.android.utils.appendCapitalized
import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.Named
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationVariant
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Usage
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.component.ConfigurationVariantDetails
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.CoreJavadocOptions
import java.io.File
import javax.inject.Inject

class AarPublishPlugin @Inject constructor(
    private val softwareComponentFactory: SoftwareComponentFactory
) : Plugin<Project> {

    override fun apply(project: Project) {
        val aarPublishingExtension = project.extensions.create("aarPublishing", AarPublishingExtension::class.java)
        project.plugins.withId("com.android.library") {
            val libraryExtension = project.extensions.getByType(LibraryExtension::class.java)
            configurePlugin(project, libraryExtension, aarPublishingExtension)
        }
    }

    private fun configurePlugin(project: Project, libraryExtension: LibraryExtension, aarPublishingExtension: AarPublishingExtension) {
        val defaultComponent = softwareComponentFactory.adhoc("android")
        project.components.add(defaultComponent)

        libraryExtension.libraryVariants.all { variant ->
            val archives = createArchivesConfigurationForVariant(project, variant, libraryExtension, aarPublishingExtension)
            val component = softwareComponentFactory.adhoc("android".appendCapitalized(variant.name))
            val apiElements = project.configurations.getByName("${variant.name}ApiElements")
            val runtimeElements = project.configurations.getByName("${variant.name}RuntimeElements")
            component.setupFromConfigurations(archives, apiElements, runtimeElements)
            project.components.add(component)

            if (variant.name == libraryExtension.defaultPublishConfig) {
                defaultComponent.setupFromConfigurations(archives, apiElements, runtimeElements)
            }
        }
    }

    private fun createArchivesConfigurationForVariant(
        project: Project,
        variant: LibraryVariant,
        baseExtension: BaseExtension,
        aarPublishingExtension: AarPublishingExtension
    ): Configuration {
        val archives = project.configurations.create("${variant.name}Archives") {
            it.attributes.apply {
                attribute(Usage.USAGE_ATTRIBUTE, project.objects.named("javadocAndSources"))
                attribute(Bundling.BUNDLING_ATTRIBUTE, project.objects.named(Bundling.EXTERNAL))
                attribute(BuildTypeAttr.ATTRIBUTE, project.objects.named(variant.buildType.name))
                variant.productFlavors.forEach { flavor ->
                    attribute(Attribute.of(flavor.dimension, ProductFlavorAttr::class.java), project.objects.named(flavor.name))
                }
            }
        }
        project.artifacts.add(archives.name, variant.packageLibraryProvider)

        if (aarPublishingExtension.publishJavadoc) {
            val javadocJar = registerJavadocTasks(project, variant, baseExtension)
            project.artifacts.add(archives.name, javadocJar)
        }

        if (aarPublishingExtension.publishSources) {
            val sourcesJar = registerSourcesTask(project, variant)
            project.artifacts.add(archives.name, sourcesJar)
        }
        return archives
    }

    private fun registerJavadocTasks(project: Project, variant: BaseVariant, baseExtension: BaseExtension): TaskProvider<Jar> {
        val javadoc = project.tasks.register("javadoc${variant.name.capitalize()}", Javadoc::class.java) { task ->
            task.group = JavaBasePlugin.DOCUMENTATION_GROUP
            task.description = "Generates Javadoc API documentation for ${variant.name} variant."
            task.dependsOn(variant.javaCompileProvider)
            variant.getSourceFolders(SourceKind.JAVA).forEach { task.source += it }
            task.classpath += project.files(baseExtension.bootClasspath)
            task.classpath += variant.javaCompileProvider.get().classpath
            task.destinationDir = File(project.buildDir, "docs/javadoc/${variant.name}")
            if (JavaVersion.current().isJava9Compatible) {
                (task.options as CoreJavadocOptions).addBooleanOption("html5", true)
            }
        }

        return project.tasks.register("package${variant.name.capitalize()}Javadoc", Jar::class.java) { task ->
            task.group = BasePlugin.BUILD_GROUP
            task.description = "Assembles a jar archive containing the javadoc of ${variant.name} variant."
            task.dependsOn(javadoc)
            task.from(javadoc.get().destinationDir)
            task.archiveClassifier.set("javadoc")
        }
    }

    private fun registerSourcesTask(project: Project, variant: BaseVariant): TaskProvider<Jar> {
        return project.tasks.register("package${variant.name.capitalize()}Sources", Jar::class.java) { task ->
            task.group = BasePlugin.BUILD_GROUP
            task.description = "Assembles a jar archive containing the sources of ${variant.name} variant."
            task.from(variant.getSourceFolders(SourceKind.JAVA))

            val kotlinSources = variant.sourceSets.asSequence()
                .filterIsInstance<AndroidSourceSet>()
                .flatMap { it.java.sourceDirectoryTrees.asSequence() }
                .map { it.setIncludes(setOf("**/*.kt")) }
                .toList()

            task.from(kotlinSources)
            task.archiveClassifier.set("sources")
        }
    }

    private fun AdhocComponentWithVariants.setupFromConfigurations(archives: Configuration, apiElements: Configuration, runtimeElements: Configuration) {
        addVariantsFromConfiguration(archives) {}
        addVariantsFromConfiguration(apiElements, AndroidConfigurationVariantMapping("compile"))
        addVariantsFromConfiguration(runtimeElements, AndroidConfigurationVariantMapping("runtime"))
    }

    private inline fun <reified T : Named> ObjectFactory.named(name: String): T = named(T::class.java, name)

    private class AndroidConfigurationVariantMapping(private val scope: String) : Action<ConfigurationVariantDetails> {
        override fun execute(details: ConfigurationVariantDetails) {
            if (!details.configurationVariant.isIgnored) {
                details.mapToMavenScope(scope)
            } else {
                details.skip()
            }
        }

        private val ConfigurationVariant.isIgnored: Boolean
            get() = artifacts.any { it.type.contains("android-") || it.type == "jar" }
    }
}
