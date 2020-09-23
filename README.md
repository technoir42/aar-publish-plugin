AAR Publish Plugin
==================

[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/com/github/technoir42/aar-publish/com.github.technoir42.aar-publish.gradle.plugin/maven-metadata.xml.svg?colorB=007ec6&label=gradlePluginPortal)](https://plugins.gradle.org/plugin/com.github.technoir42.aar-publish)
![Build](https://github.com/technoir42/aar-publish-plugin/workflows/Build/badge.svg?branch=master)

Gradle plugin for publishing Android (AAR) libraries using [Maven Publish](https://docs.gradle.org/current/userguide/publishing_maven.html) plugin.

## Requirements

* Gradle 5.3+
* Android Gradle Plugin 3.3+

## Usage

```groovy
plugins {
    id "com.android.library"
    id "com.github.technoir42.aar-publish" version "1.0.4"
    id "maven-publish"
}

publishing {
    publications {
        maven(MavenPublication) {
            from components.android
        }
    }
}
```

To learn how to configure publications and repositories refer to the documentation for [Maven Publish](https://docs.gradle.org/current/userguide/publishing_maven.html) plugin.

## Variants

The plugin registers the following `SoftwareComponent`s:

* `components.android` - for the default variant set using `android.defaultPublishConfig` which is `release` by default.
* `components.android${capitalizedVariantName}` - for all variants, e.g. `androidMyFlavorRelease` if variant name is `myFlavorRelease`.

Changing the default variant:

```groovy
android {
    defaultPublishConfig "myFlavorRelease"

    productFlavors {
        myFlavor {
        }
    }
}
```

Publishing a specific variant:

```groovy
publishing {
    publications {
        maven(MavenPublication) {
            afterEvaluate {
                from components.androidMyFlavorRelease
            }
        }
    }
}
```

## Plugin configuration

Sources and Javadoc are published by default but you can disable publishing either of them using the following DSL:

```groovy
aarPublishing {
    publishJavadoc = false
    publishSources = false
}
```

## Limitations

- Dokka is not currently supported. It might work with some extra configuration.
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
