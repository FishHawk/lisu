plugins {
    application
    kotlin("jvm") version "1.7.0"
    kotlin("plugin.serialization") version "1.7.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "me.fishhawk"
version = "0.0.1"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.ajalt.clikt:clikt:3.5.0")

    val ktorVersion = "2.1.2"
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-resources:$ktorVersion")
    implementation("io.ktor:ktor-server-caching-headers:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-websockets:$ktorVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")

    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-java:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

    implementation("org.codehaus.janino:janino:3.1.8")
    implementation("ch.qos.logback:logback-classic:1.4.3")

    implementation("se.sawano.java:alphanumeric-comparator:1.4.1")

    implementation("com.tfowl.ktor:ktor-jsoup:2.0.0")

    val krontabVersion = "0.8.1"
    implementation("dev.inmo:krontab:$krontabVersion")

    val kotestVersion = "5.5.0"
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-ktor:4.4.3")
}

application {
    mainClass.set("me.fishhawk.lisu.ApplicationKt")
    applicationDefaultJvmArgs = listOf("--Djava.net.useSystemProxies=true")
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    archiveClassifier.set("")
    manifest {
        attributes("Main-Class" to "me.fishhawk.ApplicationKt")
    }
}