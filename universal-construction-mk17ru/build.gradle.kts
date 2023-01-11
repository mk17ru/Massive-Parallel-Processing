buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlinx:atomicfu-gradle-plugin:0.14.4")
    }
}

plugins {
    kotlin("jvm") version "1.6.0"
    java
}

tasks {
    test {
        maxHeapSize = "4g"
    }
}

apply(plugin = "kotlinx-atomicfu")

group = "ru.ifmo.mpp"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
    maven("https://kotlin.bintray.com/kotlinx/")
    maven("https://dl.bintray.com/devexperts/Maven")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.6.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.6.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.6.0")
    testImplementation("org.jetbrains.kotlinx:lincheck:2.14.1")
}

sourceSets["main"].java.setSrcDirs(listOf("src"))
sourceSets["test"].java.setSrcDirs(listOf("test"))

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}