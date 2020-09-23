package com.github.technoir42.plugin.aarpublish

import org.gradle.api.Named
import org.gradle.api.Task
import org.gradle.api.attributes.Attribute
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider

inline fun <reified T> attributeOf(name: String): Attribute<T> =
    Attribute.of(name, T::class.java)

internal inline fun <reified T> ExtensionContainer.create(name: String): T =
    create(name, T::class.java)

internal inline fun <reified T> ExtensionContainer.getByType(): T =
    getByType(T::class.java)

internal inline fun <reified T : Named> ObjectFactory.named(name: String): T =
    named(T::class.java, name)

internal inline fun <reified T : Task> TaskContainer.register(name: String, noinline configurationAction: (T) -> Unit): TaskProvider<T> =
    register(name, T::class.java, configurationAction)
