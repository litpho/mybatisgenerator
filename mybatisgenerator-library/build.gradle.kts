plugins {
    `java-library`
    `maven-publish`
    signing
    id("com.diffplug.spotless")
    id("org.owasp.dependencycheck")
    id("pl.allegro.tech.build.axion-release")
}

val junitVersion: String by project

dependencies {
    api("org.mybatis:mybatis:3.5.11")
    api("org.mybatis.dynamic-sql:mybatis-dynamic-sql:1.4.1")

    implementation("xyz.downgoon:snowflake:1.0.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
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
