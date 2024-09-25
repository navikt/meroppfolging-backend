import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
    id("io.gitlab.arturbosch.detekt") version "1.23.6"
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.spring") version "1.9.23"
}

group = "no.nav.syfo"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_21

repositories {
    mavenCentral()
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
}

val tokenSupportVersion = "4.1.8"
val logstashLogbackEncoderVersion = "7.4"
val kotestVersion = "5.9.1"
val springKotestExtensionVersion = "1.3.0"
val kotestTestContainersExtensionVersion = "2.0.2"
val mockkVersion = "1.13.12"
val wiremockVersion = "3.9.1"
val wiremockKotestExtensionVersion = "3.1.0"
val detektVersion = "1.23.7"
val testcontainersVersion = "1.20.1"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")

    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.postgresql:postgresql")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("no.nav.security:token-client-spring:$tokenSupportVersion")
    implementation("no.nav.security:token-validation-spring:$tokenSupportVersion")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashLogbackEncoderVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-property:$kotestVersion")
    testImplementation("io.kotest.extensions:kotest-extensions-testcontainers:$kotestTestContainersExtensionVersion")
    testImplementation("io.kotest.extensions:kotest-extensions-spring:$springKotestExtensionVersion")
    testImplementation("io.kotest.extensions:kotest-extensions-wiremock:$wiremockKotestExtensionVersion")
    testImplementation("org.wiremock:wiremock-standalone:$wiremockVersion")
    testImplementation("no.nav.security:token-validation-spring-test:$tokenSupportVersion")
    testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("org.testcontainers:kafka:$testcontainersVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")

    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:$detektVersion")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    this.archiveFileName.set("app.jar")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "21"
    }
}

tasks.named<Jar>("jar") {
    enabled = false
}

tasks.withType<Test> {
    useJUnitPlatform()
}

detekt {
    config.from("detekt-config.yml")
    buildUponDefaultConfig = true
}
