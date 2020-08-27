import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.72"
    application
    id("com.github.johnrengelman.shadow") version "6.0.0"
}

application{
    mainClassName="tv.anypoint.jonathan.persistence.v1.PServerKt"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val scalaVersion = "2.12"
val akkaVersion = "2.5.31"

tasks.withType<Test> {
    useJUnitPlatform()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.typesafe.akka:akka-remote_$scalaVersion:$akkaVersion")
    implementation("com.typesafe.akka:akka-actor_$scalaVersion:$akkaVersion")
    implementation("com.typesafe.akka:akka-cluster_$scalaVersion:$akkaVersion")
    implementation("com.typesafe.akka:akka-cluster-tools_$scalaVersion:$akkaVersion")
    implementation("com.typesafe.akka:akka-slf4j_$scalaVersion:$akkaVersion")
    implementation("com.typesafe.akka:akka-testkit_$scalaVersion:$akkaVersion")

    implementation("io.github.microutils:kotlin-logging:1.7.9")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.3.72")

    implementation("junit:junit:4.13")

    implementation("com.typesafe.akka:akka-cluster-metrics_$scalaVersion:$akkaVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.8")

    implementation("com.typesafe.akka:akka-persistence_$scalaVersion:$akkaVersion")
    implementation("com.google.protobuf:protobuf-java:3.13.0")
    implementation("org.fusesource.leveldbjni:leveldbjni-all:1.8")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.2")
}


tasks {
    named<ShadowJar>("shadowJar") {
        append("reference.conf")
    }
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

project.gradle.startParameter.excludedTaskNames.add("test")
