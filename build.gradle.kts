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

    val ktorVersion = "1.6.5"
    implementation("io.ktor:ktor-locations:$ktorVersion")
    implementation("io.ktor:ktor-serialization:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-serialization:$ktorVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")

    implementation("org.codehaus.janino:janino:3.1.6")
    implementation("ch.qos.logback:logback-classic:1.2.6")

    implementation("se.sawano.java:alphanumeric-comparator:1.4.1")

    val krontabVersion = "0.6.6"
    implementation("dev.inmo:krontab:$krontabVersion")

    val kotestVersion = "4.6.3"
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
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