AAR Publish Plugin
==================

[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/com/github/technoir42/aar-publish/com.github.technoir42.aar-publish.gradle.plugin/maven-metadata.xml.svg?colorB=007ec6&label=gradlePluginPortal)](https://plugins.gradle.org/plugin/com.github.technoir42.aar-publish)
![Build](https://github.com/technoir42/aar-publish-plugin/workflows/Build/badge.svg?branch=master)

Gradle plugin which enables publishing of sources and Javadoc for Android libraries.
Designed to be used in conjunction with Android Gradle plugin 3.6+ and Maven Publish plugin.

Before using this plugin make sure to read the [documentation](https://developer.android.com/studio/build/maven-publish-plugin)
how to use Maven Publish plugin with Android projects.

## Usage

Apply plugin to the library you want to publish sources and/or javadoc for:

```groovy
plugins {
    id "com.android.library"
    id "com.github.technoir42.aar-publish" version "2.0.0"
    id "maven-publish"
}
```

The plugin will configure the default software components created by Android Gradle plugin to also include sources and Javadoc.

If you wish to disable publishing of Javadoc or sources, you can use the following DSL:

```groovy
aarPublishing {
    publishJavadoc = false
    publishSources = false
}
```

## Limitations

- KDoc is not currently supported. It might work with some extra configuration.
- Kotlin Multiplatform projects are not supported.

## License

```
Copyright 2019 Sergey Chelombitko

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
