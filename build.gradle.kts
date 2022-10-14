// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath ("com.android.tools.build:gradle:7.2.2")
        classpath ("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.20")
    }
}

plugins {
  id("org.sonarqube") version "3.4.0.2513"
    id("org.jetbrains.kotlin.android") version "1.7.20" apply false
}

allprojects {
    tasks.withType<JavaCompile> {
        val compilerArgs = options.compilerArgs
        compilerArgs.add("-Xlint:unchecked")
        compilerArgs.add("-Xlint:deprecation")
    }
}

sonarqube {
  properties {
    property("sonar.projectKey", "Lmh170_Android-Gallery-App")
    property("sonar.organization", "lmh170")
    property("sonar.host.url", "https://sonarcloud.io")
  }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
