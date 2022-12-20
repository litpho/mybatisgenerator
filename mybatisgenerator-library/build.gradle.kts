plugins {
    `java-library`
    `maven-publish`
    signing
    id("com.diffplug.spotless")
    id("org.owasp.dependencycheck")
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
