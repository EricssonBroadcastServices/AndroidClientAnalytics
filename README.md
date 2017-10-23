# AndroidClientAnalytics
Library for android 2.0 analytics

## Requirements

* `Android` 4.4+

## Installation

### JitPack
Releases are available on [JitPack](https://jitpack.io/#EricssonBroadcastServices/AndroidClientAnalytics) and can be automatically imported to your project using Gradle dependency management.

Add the jitpack.io repository to your project **build.gradle**:
```gradle
allprojects {
 repositories {
    jcenter()
    maven { url "https://jitpack.io" }
 }
}
```

Then add the dependency to your module **build.gradle**:
```gradle
dependencies {
    compile 'com.github.EricssonBroadcastServices:analytics:{version}'
}
```

Note: do not add the jitpack.io repository under *buildscript {}*

## Release Notes
Release specific changes can be found in the [CHANGELOG](https://github.com/EricssonBroadcastServices/AndroidClientAnalytics/blob/master/CHANGELOG.md).

## Upgrade Guides
Major changes between releases will be documented with special [Upgrade Guides](https://github.com/EricssonBroadcastServices/AndroidClientAnalytics/blob/master/UPGRADE_GUIDE.md).


