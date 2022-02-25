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
}
allprojects {
    apply(plugin = "kotlin")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
    repositories {
        mavenCentral()
        maven("https://maven.aliyun.com/repository/public") // 阿里云国内代理仓库
        maven("https://jitpack.io")
    }
    dependencies {
        api(platform("net.mamoe:mirai-bom:2.10.0"))
        implementation("net.mamoe:mirai-console-terminal")
        implementation("net.mamoe:mirai-console")
        implementation("net.mamoe:mirai-core")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    }
}
dependencies {
    implementation(project(":reply-trigger"))
    implementation(project(":cloud-recruit"))
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