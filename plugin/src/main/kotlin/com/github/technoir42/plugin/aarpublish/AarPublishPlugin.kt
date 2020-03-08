package com.github.technoir42.plugin.aarpublish

import com.android.build.api.attributes.BuildTypeAttr
import com.android.build.api.attributes.ProductFlavorAttr
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.AndroidSourceSet
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.SourceKind
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.DocsType
import org.gradle.api.attributes.Usage
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.CoreJavadocOptions
import java.io.File

class AarPublishPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val aarPublishingExtension = project.extensions.create("aarPublishing", AarPublishingExtension::class.java)
        project.plugins.withId("com.android.library") {
            val libraryExtension = project.extensions.findByType(LibraryExtension::class.java)!!
            libraryExtension.libraryVariants.all { variant ->
                configureComponentsForVariant(project, variant, libraryExtension, aarPublishingExtension)
            }
        }
    }

    private fun configureComponentsForVariant(
        project: Project,
        variant: BaseVariant,
        baseExtension: BaseExtension,
        aarPublishingExtension: AarPublishingExtension
    ) {
        val component = project.components.getByName(variant.name) as AdhocComponentWithVariants
        val all = project.components.getByName("all") as AdhocComponentWithVariants

        if (aarPublishingExtension.publishJavadoc) {
            val javadocJar = registerJavadocTasks(project, variant, baseExtension)

            val javadocPublication = project.createDocumentationConfiguration("${variant.name}JavadocPublication", variant, DocsType.JAVADOC, false)
            javadocPublication.outgoing.artifact(javadocJar) {
                it.classifier = DocsType.JAVADOC
            }
            component.addVariantsFromConfiguration(javadocPublication) {}

            val allJavadocPublication = project.createDocumentationConfiguration("${variant.name}AllJavadocPublication", variant, DocsType.JAVADOC, true)
            allJavadocPublication.outgoing.artifact(javadocJar)
            all.addVariantsFromConfiguration(allJavadocPublication) {}
        }

        if (aarPublishingExtension.publishSources) {
            val sourcesJar = registerSourcesTask(project, variant)

            val sourcesPublication = project.createDocumentationConfiguration("${variant.name}SourcesPublication", variant, DocsType.SOURCES, false)
            sourcesPublication.outgoing.artifact(sourcesJar) {
                it.classifier = DocsType.SOURCES
            }
            component.addVariantsFromConfiguration(sourcesPublication) {}

            val allSourcesPublication = project.createDocumentationConfiguration("${variant.name}AllSourcesPublication", variant, DocsType.SOURCES, true)
            allSourcesPublication.outgoing.artifact(sourcesJar)
            all.addVariantsFromConfiguration(allSourcesPublication) {}
        }
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

        return project.tasks.register("javadoc${variant.name.capitalize()}Jar", Jar::class.java) { task ->
            task.group = BasePlugin.BUILD_GROUP
            task.description = "Assembles a jar archive containing the javadoc of ${variant.name} variant."
            task.dependsOn(javadoc)
            task.from(javadoc.get().destinationDir)
            task.archiveClassifier.set("${variant.name}-${DocsType.JAVADOC}")
        }
    }

    private fun registerSourcesTask(project: Project, variant: BaseVariant): TaskProvider<Jar> {
        return project.tasks.register("sources${variant.name.capitalize()}Jar", Jar::class.java) { task ->
            task.group = BasePlugin.BUILD_GROUP
            task.description = "Assembles a jar archive containing the sources of ${variant.name} variant."
            task.from(variant.getSourceFolders(SourceKind.JAVA))

            val kotlinSources = variant.sourceSets.asSequence()
                .filterIsInstance<AndroidSourceSet>()
                .flatMap { it.java.sourceDirectoryTrees.asSequence() }
                .map { it.setIncludes(setOf("**/*.kt")) }
                .toList()

            task.from(kotlinSources)
            task.archiveClassifier.set("${variant.name}-${DocsType.SOURCES}")
        }
    }

    private fun Project.createDocumentationConfiguration(
        configurationName: String,
        variant: BaseVariant,
        docsType: String,
        withVariantAttributes: Boolean
    ): Configuration =
        configurations.create(configurationName) {
            it.isCanBeResolved = false
            it.isCanBeConsumed = true
            it.isVisible = false
            it.description = "$docsType elements for ${variant.name}"
            it.attributes.apply {
                attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, Usage.JAVA_RUNTIME))
                attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category::class.java, Category.DOCUMENTATION))
                attribute(DocsType.DOCS_TYPE_ATTRIBUTE, project.objects.named(DocsType::class.java, docsType))
                attribute(Bundling.BUNDLING_ATTRIBUTE, project.objects.named(Bundling::class.java, Bundling.EXTERNAL))
                if (withVariantAttributes) {
                    addAttributesFromVariant(variant, project.objects)
                }
            }
        }

    private fun AttributeContainer.addAttributesFromVariant(variant: BaseVariant, objectFactory: ObjectFactory) {
        attribute(BuildTypeAttr.ATTRIBUTE, objectFactory.named(BuildTypeAttr::class.java, variant.buildType.name))
        variant.productFlavors.forEach { flavor ->
            attribute(Attribute.of(flavor.dimension, ProductFlavorAttr::class.java), objectFactory.named(ProductFlavorAttr::class.java, flavor.name))
        }
    }
}
