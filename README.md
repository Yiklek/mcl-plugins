# mcl-plugins

[mirai-console](https://github.com/mamoe/mirai-console) 插件

## Usage

**Gradle**

```groovy
allprojects {
    repositories {
//      ...
        maven { url 'https://jitpack.io' }
    }
}
dependencies {
    implementation 'com.github.yiklek:mcl-plugins:-SNAPSHOT'
}
```

**Kotlin**

```kotlin
allprojects {
    repositories {
//      ...
        maven("https://jitpack.io")
    }
}
dependencies {
    implementation("com.github.yiklek:mcl-plugins:-SNAPSHOT")
}
```

## Plugins

### reply-trigger

```yaml
botIds: [ 123456 ]
rules:
  - groups: # 群   设置为null关闭，空列表允许所有
      - 12345
    friends: # 好友 设置为null关闭，空列表允许所有
      - 12345
    triggers: # 触发词
      - 1
    reply: 2    # 回复词
```

### cloud-recruit

```yaml
groupId: 12345  #群号
```
