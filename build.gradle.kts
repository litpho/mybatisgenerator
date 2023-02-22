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

plugins.withType(ReleasePlugin::class) {
    allprojects {
        project.version = rootProject.scmVersion.version
    }
}

allprojects {
    repositories {
        mavenCentral()
    }

    dependencyLocking {
        lockAllConfigurations()
    }
}

tasks.register("resolveAndLockAll") {
    group = "Build"
    description = "Write and update all dependency locks"
    doFirst {
        require(gradle.startParameter.isWriteDependencyLocks)
    }
    doLast {
        allprojects.forEach { p ->
            p.configurations.filter {
                it.isCanBeResolved
            }.forEach { it.resolve() }
        }
    }
}
