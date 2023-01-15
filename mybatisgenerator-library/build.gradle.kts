plugins {
    `java-library`
    `maven-publish`
    signing
    id("com.diffplug.spotless")
    id("org.owasp.dependencycheck")
    id("pl.allegro.tech.build.axion-release")
}

dependencies {
    api("org.mybatis:mybatis:3.+")
    api("org.mybatis.dynamic-sql:mybatis-dynamic-sql:1.+")

    implementation("xyz.downgoon:snowflake:1.+")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.+")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.+")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.+")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    withJavadocJar()
    withSourcesJar()
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
                name.set("MyBatis Generator Plugins Library")
                description.set("A library to support the extended MyBatis Generator plugins")
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
    java {
        googleJavaFormat()
    }
}

dependencyCheck {
    suppressionFile = "${project.rootDir}/gradle/suppressions.xml"
}
