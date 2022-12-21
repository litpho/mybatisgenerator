import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    jacoco
    `java-library`
    `maven-publish`
    signing
    id("com.diffplug.spotless")
    id("org.jetbrains.kotlin.jvm")
    id("org.owasp.dependencycheck")
    id("pl.allegro.tech.build.axion-release")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    withJavadocJar()
    withSourcesJar()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

val h2DriverVersion: String by project
val junitVersion: String by project

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("org.mybatis:mybatis:3.5.11")
    implementation("org.mybatis.generator:mybatis-generator-core:1.4.1")
    implementation("org.yaml:snakeyaml:1.33")

    testImplementation("com.github.javaparser:javaparser-core:3.24.9")
    testImplementation("com.h2database:h2:$h2DriverVersion")
    testImplementation("io.kotest:kotest-assertions-core-jvm:5.5.4")
    testImplementation("io.mockk:mockk:1.13.3")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("main") {
            from(components["java"])

            pom {
                name.set("MyBatis Generator Plugins")
                description.set("A set of MyBatis Generator plugins to extend the prepackaged capabilities")
                url.set("https://github.com/litpho/mybatisgenerator")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("litpho")
                        name.set("Jasper de Vries")
                        email.set("jasper.devries@the-future-group.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/mybatisgenerator.git")
                    developerConnection.set("scm:git:git://github.com/mybatisgenerator.git")
                    url.set("https://github.com/litpho/mybatisgenerator")
                }
            }
        }
    }
}

signing {
    sign(publishing.publications["main"])
}

spotless {
    kotlin {
        ktlint()
    }
}

dependencyCheck {
    suppressionFile = "${project.rootDir}/gradle/suppressions.xml"
}
