plugins {
    kotlin("jvm") version "2.1.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0"
}

group = "com.gmail.maxrybalkin91"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.jsqlparser:jsqlparser:5.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.0")
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.build {
    dependsOn(tasks.test)
}

tasks.assemble {
    finalizedBy(tasks.check)
}

kotlin {
    jvmToolchain(21)
}