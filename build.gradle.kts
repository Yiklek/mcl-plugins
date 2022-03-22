plugins {
    val kotlinVersion = "1.6.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    id("com.github.johnrengelman.shadow") version "6.1.0"
    application
}

group = "io.github.yiklek"
version = "0.1.0"

repositories {
    maven("https://maven.aliyun.com/repository/public") // 阿里云国内代理仓库
    mavenCentral()
    maven("https://jitpack.io")
}

buildscript {
    repositories {
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }
    dependencies {
        classpath("net.mamoe:mirai-console-gradle:2.10.0")
    }
}

subprojects {
    apply(plugin = "net.mamoe.mirai-console")
    repositories {
        mavenCentral()
        maven("https://maven.aliyun.com/repository/public") // 阿里云国内代理仓库
        maven("https://jitpack.io")
    }
    dependencies {
        compileOnly("net.mamoe:mirai-console-terminal")
        compileOnly("net.mamoe:mirai-console")
        compileOnly("net.mamoe:mirai-core")
        implementation("io.ktor:ktor-client-core:1.6.8")
        implementation("io.ktor:ktor-client-cio:1.6.8")

    }
}

allprojects {
    apply(plugin = "kotlin")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
    dependencies {
        api(platform("net.mamoe:mirai-bom:2.10.0"))
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
        runtimeOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
    }
}

dependencies {
    implementation(project("plugins:reply-trigger"))
    implementation(project("plugins:cloud-recruit"))
    implementation(project("plugins:site-monitor"))
    implementation("net.mamoe:mirai-console-terminal")
    implementation("net.mamoe:mirai-console")
    implementation("net.mamoe:mirai-core")

    implementation("xyz.cssxsh.mirai:mirai-device-generator:1.0.2")
    implementation("com.github.project-mirai:chat-command:0.5.1")
}
application {
    mainClass.set("RunMiraiKt")
    mainClassName = mainClass.get()
}

tasks {
    shadowJar {
        mergeServiceFiles()
    }
    build {
        dependsOn(shadowJar)
    }
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}