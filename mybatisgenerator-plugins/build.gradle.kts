import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    jacoco
    `java-library`
    `maven-publish`
    signing
    kotlin("jvm") version "1.8.0"
    id("com.diffplug.spotless")
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

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.mybatis:mybatis:3.+")
    implementation("org.mybatis.generator:mybatis-generator-core:1.+")
    implementation("org.yaml:snakeyaml:1.+")

    testImplementation("com.github.javaparser:javaparser-core:3.+")
    testImplementation("com.h2database:h2:2.+")
    testImplementation("io.kotest:kotest-assertions-core-jvm:5.+")
    testImplementation("io.mockk:mockk:1.+")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.+")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.+")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.+")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("main") {
            from(components["java"])
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }

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
    repositories {
        maven {
            name = "ossrh"
            credentials(PasswordCredentials::class)
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
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
