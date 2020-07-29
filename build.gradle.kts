plugins {
    kotlin("jvm") version "1.3.72"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val scalaVersion = "2.12"
val akkaVersion = "2.5.31"
val kotestVersion = "4.1.2"

tasks.withType<Test> {
    useJUnitPlatform()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.typesafe.akka:akka-actor_$scalaVersion:$akkaVersion")
    implementation("com.typesafe.akka:akka-cluster_$scalaVersion:$akkaVersion")
    implementation("com.typesafe.akka:akka-slf4j_$scalaVersion:$akkaVersion")
    implementation("com.typesafe.akka:akka-testkit_$scalaVersion:$akkaVersion")
    implementation("io.github.microutils:kotlin-logging:1.7.9")
    implementation("ch.qos.logback:logback-classic:1.2.3")

    //implementation("org.junit.jupiter:junit-jupiter-api:5.6.2")
    implementation("junit:junit:4.13")

    testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion") // for kotest framework
    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion") // for kotest core jvm assertions
    testImplementation("io.kotest:kotest-property-jvm:$kotestVersion") // for kotest property test

    implementation("com.typesafe.akka:akka-cluster-metrics_$scalaVersion:$akkaVersion")
    //implementation("com.swissborg:lithium:0.11.2")
    //implementation("com.lightbend.akka:akka-split-brain-resolver_$scalaVersion:1.1.14")   //NOT working..
    //implementation("io.kamon:sigar-loader:1.6.6-rev002")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}
