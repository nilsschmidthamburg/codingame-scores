import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.61"
}

group = "nilsschmidthamburg.github.io"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.3.2")
    implementation("org.json:json:20190722")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "12"
}
