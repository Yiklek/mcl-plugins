plugins {
    val kotlinVersion = "1.6.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.10.0"
}

group = "io.github.yiklek"
version = "0.1.0"

repositories {
    maven("https://maven.aliyun.com/repository/public") // 阿里云国内代理仓库
    mavenCentral()
}
subprojects {
    apply(plugin = "net.mamoe.mirai-console")
    apply(plugin = "kotlin")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
    repositories {
        mavenCentral()
        maven("https://maven.aliyun.com/repository/public") // 阿里云国内代理仓库
    }
}