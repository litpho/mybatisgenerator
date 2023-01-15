import pl.allegro.tech.build.axion.release.ReleasePlugin

buildscript {
    configurations.classpath {
        resolutionStrategy.activateDependencyLocking()
    }
}

plugins {
    id("com.diffplug.spotless") version ("6.+")
    id("org.owasp.dependencycheck") version ("7.+") apply(false)
    id("pl.allegro.tech.build.axion-release") version("1.+")
}

allprojects {
    plugins.withType(ReleasePlugin::class) {
        project.version = scmVersion.version
    }

    repositories {
        mavenCentral()
    }

    dependencyLocking {
        lockAllConfigurations()
    }
}
