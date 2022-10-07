import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    jacoco
    `java-library`
    `maven-publish`
    id("com.diffplug.spotless")
    id("org.jetbrains.kotlin.jvm")
}

java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17

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

    testImplementation("com.github.javaparser:javaparser-core:3.24.4")
    testImplementation("com.h2database:h2:$h2DriverVersion")
    testImplementation("io.kotest:kotest-assertions-core-jvm:5.4.2")
    testImplementation("io.mockk:mockk:1.12.5")
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
        }
    }
}

spotless {
    kotlin {
        ktlint()
    }
}