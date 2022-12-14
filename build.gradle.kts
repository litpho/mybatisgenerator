import org.owasp.dependencycheck.gradle.DependencyCheckPlugin
import org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension

plugins {
    id("com.diffplug.spotless") version ("6.11.0")
    id("org.owasp.dependencycheck") version ("7.4.1") apply(false)
}

allprojects {
    repositories {
        mavenCentral()
    }
}



