import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.4.20"
    kotlin("plugin.spring") version "2.4.20"
    kotlin("plugin.jpa") version "2.4.20"
    id("org.springframework.boot") version "4.1.1"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
}

group = "ru"
version = "0.0.1-SNAPSHOT"
description = "games-telegram-bot"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

ktlint {
    version.set("1.8.0")
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.1.1"))
    implementation(enforcedPlatform("org.jetbrains.kotlin:kotlin-bom:2.4.20"))
    implementation(enforcedPlatform("org.hibernate.orm:hibernate-platform:7.4.11.Final"))
    implementation(platform("org.telegram:telegrambots-bom:10.3.0"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.liquibase:liquibase-core:5.0.4")
    implementation("org.telegram:telegrambots-longpolling")
    implementation("org.telegram:telegrambots-client")
    runtimeOnly("org.postgresql:postgresql:42.7.13")

    testImplementation(enforcedPlatform("org.junit:junit-bom:6.1.3"))
    testImplementation(enforcedPlatform("org.testcontainers:testcontainers-bom:2.0.5"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform {
        excludeTags("integration")
    }
}

val integrationTest =
    tasks.register<Test>("integrationTest") {
        description = "Checks application startup and migrations against PostgreSQL in Docker."
        group = "verification"
        testClassesDirs =
            sourceSets.test
                .get()
                .output.classesDirs
        classpath = sourceSets.test.get().runtimeClasspath
        useJUnitPlatform {
            includeTags("integration")
        }
        shouldRunAfter(tasks.test)
    }

tasks.check {
    dependsOn(integrationTest)
}
