import com.google.protobuf.gradle.*

plugins {
    kotlin("jvm") version "1.9.21"
    id("application")
    id("com.google.protobuf") version "0.9.4"
}

dependencies {
    implementation("com.google.protobuf:protobuf-kotlin:3.25.1")
    protobuf(files("proto"))
    implementation(kotlin("stdlib-jdk8"))
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.1"
    }
}

application {
    mainClass = "ConfigGeneratorKt"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
