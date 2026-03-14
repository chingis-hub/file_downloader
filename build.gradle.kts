plugins {
    kotlin("jvm") version "2.1.10"
    application
}

application {
    mainClass.set("com.chingis.MainKt")
}

group = "com.chingis"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Ktor Client
    implementation("io.ktor:ktor-client-core:3.0.3")
    implementation("io.ktor:ktor-client-cio:3.0.3")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("ch.qos.logback:logback-classic:1.5.6")

    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("io.ktor:ktor-client-mock:3.0.3")
    testImplementation("io.ktor:ktor-server-core-jvm:3.0.3")
    testImplementation("io.ktor:ktor-server-netty-jvm:3.0.3")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
