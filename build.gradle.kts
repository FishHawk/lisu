plugins {
    application
    kotlin("jvm") version "1.5.30"
    kotlin("plugin.serialization") version "1.5.30"
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

group = "me.fishhawk"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.3")

    implementation("io.ktor:ktor-locations:1.6.4")
    implementation("io.ktor:ktor-serialization:1.6.4")
    implementation("io.ktor:ktor-server-core:1.6.4")
    implementation("io.ktor:ktor-server-netty:1.6.4")
    implementation("io.ktor:ktor-client-core:1.6.4")
    implementation("io.ktor:ktor-client-cio:1.6.4")
    implementation("io.ktor:ktor-client-serialization:1.6.4")
    testImplementation("io.ktor:ktor-server-test-host:1.6.4")

    implementation("org.codehaus.janino:janino:3.1.6")
    implementation("ch.qos.logback:logback-classic:1.2.6")

    implementation("se.sawano.java:alphanumeric-comparator:1.4.1")

    testImplementation("io.kotest:kotest-runner-junit5:4.6.3")
    testImplementation("io.kotest:kotest-assertions-core:4.6.3")
}

application {
    mainClass.set("me.fishhawk.lisu.ApplicationKt")
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    archiveClassifier.set("")
    manifest {
        attributes(Pair("Main-Class", "me.fishhawk.ApplicationKt"))
    }
}