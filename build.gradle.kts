import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("jvm") version "1.9.0"
    id("java-library")
    id("com.vanniktech.maven.publish") version "0.25.3"
}

group = "io.github.kevinah95"
version = "0.3.1-SNAPSHOT" // x-release-please-version

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("io.mockk:mockk:1.13.7")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.0")

    // For JGit
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.7.0.202309050840-r")
    implementation("org.eclipse.jgit:org.eclipse.jgit.archive:6.7.0.202309050840-r")
    implementation("org.eclipse.jgit:org.eclipse.jgit.ssh.jsch:6.7.0.202309050840-r")
    implementation("org.eclipse.jgit:org.eclipse.jgit.ssh.apache:6.7.0.202309050840-r")
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

mavenPublishing {
    // Configuring Maven Central
    // See: https://vanniktech.github.io/gradle-maven-publish-plugin/central/#configuring-maven-central
    publishToMavenCentral(SonatypeHost.S01, true)

    signAllPublications()

    // Configuring POM
    // See: https://vanniktech.github.io/gradle-maven-publish-plugin/central/#configuring-the-pom
    val artifactId = "kdriller"

    coordinates(
        group.toString(),
        artifactId,
        version.toString()
    )

    pom {
        name.set(artifactId)
        description.set(
            "KDriller is a Kotlin framework that helps developers in analyzing Git repositories. " +
            "With KDriller you can easily extract information about commits, developers, modified files, diffs, and source code."
        )
        url.set("https://github.com/kevinah95/KDriller/")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("kevinah95")
                name.set("Kevin Hernández Rostrán")
                url.set("https://github.com/kevinah95/")
                email.set("kevinah95@gmail.com")
            }
        }
        scm {
            url.set("https://github.com/kevinah95/KDriller/")
            connection.set("scm:git:git://github.com/kevinah95/KDriller.git")
            developerConnection.set("scm:git:ssh://git@github.com/kevinah95/KDriller.git")
        }
    }
}