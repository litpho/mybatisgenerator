plugins {
    id("com.diffplug.spotless") version ("6.11.0")
    id("org.owasp.dependencycheck") version ("7.4.1") apply(false)
    id("pl.allegro.tech.build.axion-release") version("1.14.3")
}

project.version = scmVersion.version

allprojects {
    repositories {
        mavenCentral()
    }
}
