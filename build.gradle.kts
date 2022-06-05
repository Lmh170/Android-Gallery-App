// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath ("com.android.tools.build:gradle:7.2.1")
        classpath ("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.21")
    }
}

plugins {
  id("org.sonarqube") version "3.3"
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
    property("sonar.projectKey", "flawedworld_Android-Gallery-App")
    property("sonar.organization", "flawedworld")
    property("sonar.host.url", "https://sonarcloud.io")
  }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
