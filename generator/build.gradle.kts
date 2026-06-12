import com.google.protobuf.gradle.*

plugins {
    kotlin("jvm")
    id("application")
    id("com.google.protobuf") version "0.10.0"
}

dependencies {
    implementation("com.google.protobuf:protobuf-kotlin:4.35.1")
    protobuf(files("proto"))
    implementation(kotlin("stdlib-jdk8"))
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.35.1"
    }
}

application {
    mainClass = "ConfigGeneratorKt"
}

kotlin {
    jvmToolchain(17)
}
