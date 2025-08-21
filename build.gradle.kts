plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.5.4"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.beratyesbek"
version = "0.0.1-SNAPSHOT"
description = "dsl-team-pr-buddy"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

extra["springCloudGcpVersion"] = "7.1.0"
extra["springCloudVersion"] = "2025.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.cloud:spring-cloud-gcp-data-firestore")
    implementation("com.google.cloud:spring-cloud-gcp-starter-data-firestore")
    implementation("com.google.cloud:spring-cloud-gcp-starter")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("jakarta.mail:jakarta.mail-api")
    implementation("org.springframework:spring-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.google.genai:google-genai:1.13.0")

}

dependencyManagement {
    imports {
        mavenBom("com.google.cloud:spring-cloud-gcp-dependencies:${property("springCloudGcpVersion")}")
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
