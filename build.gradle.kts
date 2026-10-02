plugins {
    kotlin("jvm") version "2.4.20"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
}

for (exampleIndex in 1..5) {
    tasks.register<JavaExec>("runExample$exampleIndex") {
        description = "Runs example $exampleIndex in the command line"
        mainClass = "org.example.example${exampleIndex}.Example${exampleIndex}Kt"
        classpath = sourceSets.main.get().runtimeClasspath
        standardInput = System.`in`
    }
}