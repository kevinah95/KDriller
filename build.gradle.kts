import com.github.kevinah95.kdriller.Configuration

plugins {
    kotlin("jvm") version "1.9.0"
    id("java-library")
    id("com.vanniktech.maven.publish") version "0.25.3"
}

apply(from = "${rootDir}/scripts/publish-module.gradle.kts")

mavenPublishing {
    pom {
        version = rootProject.extra.get("libVersion").toString()
        group = Configuration.artifactGroup
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("io.mockk:mockk:1.13.7")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.0")

    // For JGit
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.6.0.202305301015-r")
    implementation("org.eclipse.jgit:org.eclipse.jgit.archive:6.6.0.202305301015-r")
    implementation("org.eclipse.jgit:org.eclipse.jgit.ssh.jsch:6.6.0.202305301015-r")
    implementation("org.eclipse.jgit:org.eclipse.jgit.ssh.apache:6.5.0.202303070854-r")
    implementation("commons-io:commons-io:2.13.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.2")

    // Logger
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("ch.qos.logback:logback-classic:1.4.8")

    // Analyzer
    implementation("io.github.kevinah95:klizard:0.1.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}